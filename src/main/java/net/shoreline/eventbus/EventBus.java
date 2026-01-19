package net.shoreline.eventbus;

import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.Event;

import java.lang.reflect.Method;
import java.util.*;

public final class EventBus
{
    public static final EventBus INSTANCE = new EventBus();

    /**
     * A Map<Class<Event>, Invoker> where the keys are the linked list for that event type.
     *
     * So the list may look like...
     *
     * <PacketEvent:Invoker>,
     * <RenderEvent:Invoker>,
     * <JoinGameEvent:Invoker>
     *
     * This way, instead of a single linked list that we iterate down each time an event is posted,
     * we query the map to get the linked list associated with a certain event and invoke ALL the events
     * on that chain of invokers, without checking if the methodType matches the eventType.
     */
    private final Map<Class<? extends Event>, List<InvokerNode>> event2InvokerMap = new HashMap<>();
    private final Map<Object, List<InvokerNode>> subscriber2NodesMap = new HashMap<>();

    private EventBus()
    {
    }

    /**
     * Iterate through the linked list for the event and invoke any entries matching the event type
     */
    public void dispatch(Event event)
    {
        List<InvokerNode> invokers = event2InvokerMap.get(event.getClass());
        if (invokers == null) return;

        for (InvokerNode node : invokers)
        {
            try
            {
                node.invoker.invoke(event);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Subscribe an object to receive events
     */
    public Object subscribe(Object subscriber)
    {
        if (subscriber == null) return null;

        List<InvokerNode> nodes = new ArrayList<>();
        Class<?> clazz = subscriber.getClass();

        // Get all methods including inherited ones
        Method[] allMethods = clazz.getMethods();
        for (Method method : allMethods)
        {
            if (!method.isAnnotationPresent(EventListener.class)) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) continue;

            method.setAccessible(true);
            EventListener listener = method.getAnnotation(EventListener.class);
            int priority = listener.priority();
            boolean receiveCanceled = listener.receiveCanceled();

            Class<? extends Event> eventClass = (Class<? extends Event>) params[0];
            Invoker invoker = (event) ->
            {
                if (((Event) event).isCanceled() && !receiveCanceled) return;

                try
                {
                    method.invoke(subscriber, event);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            };

            InvokerNode node = new InvokerNode(invoker, subscriber, priority);
            nodes.add(node);

            event2InvokerMap.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(node);
            event2InvokerMap.get(eventClass).sort(Comparator.comparingInt(n -> -n.priority));
        }

        subscriber2NodesMap.put(subscriber, nodes);
        return subscriber;
    }

    /**
     * Unsubscribe an object from receiving events
     */
    public Object unsubscribe(Object subscriber)
    {
        if (subscriber == null) return null;

        List<InvokerNode> nodes = subscriber2NodesMap.remove(subscriber);
        if (nodes != null)
        {
            for (InvokerNode node : nodes)
            {
                event2InvokerMap.values().forEach(list -> list.remove(node));
            }
        }

        return subscriber;
    }


    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    public final static class InvokerNode
    {
        private final Invoker invoker;
        private final Object subscriber;
        private final Integer priority;

        public InvokerNode(Invoker invoker, Object subscriber, Integer priority)
        {
            this.invoker = invoker;
            this.subscriber = subscriber;
            this.priority = priority;
        }
    }

    @FunctionalInterface
    public interface Invoker
    {
        void invoke(Object event);
    }
}
