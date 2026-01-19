package net.shoreline.client.impl.module.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * AutoMace module - Automatically attacks players with mace after falling
 * @author hockeyl8, Shoreline
 * @since 1.0
 */
public class AutoMaceModule extends RotationModule
{
    private static AutoMaceModule INSTANCE;

    Config<Float> targetRangeConfig = register(new NumberConfig<>("TargetRange", "Target range", 10.0f, 15.0f, 30.0f));
    Config<Float> attackRangeConfig = register(new NumberConfig<>("AttackRange", "Attack range", 3.0f, 6.0f, 6.0f));
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotate to target", false));
    Config<Boolean> trackConfig = register(new BooleanConfig("Track", "Track target with elytra", true));
    Config<Boolean> rubberbandConfig = register(new BooleanConfig("Lagback", "Use lagback after attack", true));

    private PlayerEntity target = null;
    private final CacheTimer timer = new CacheTimer();

    private boolean attacking = false;
    private boolean shouldAttack = false;
    private boolean reset = false;

    public AutoMaceModule()
    {
        super("AutoMace", "Automatically attacks players with mace after falling", ModuleCategory.COMBAT);
        INSTANCE = this;
    }

    public static AutoMaceModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onEnable()
    {
        timer.reset();
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event)
    {
        try
        {
            if (!trackConfig.getValue() || mc.player == null || mc.player.isOnGround() || target == null)
            {
                return;
            }

            if (mc.player.distanceTo(target) <= attackRangeConfig.getValue())
            {
                return;
            }

            if (!timer.passed(200))
            {
                return;
            }

            if (mc.player.squaredDistanceTo(target.getX(), mc.player.getY(), target.getZ()) > MathHelper.square(6.0f))
            {
                if (!mc.player.isFallFlying())
                {
                    if (InventoryUtil.hasItemInHotbar(Items.ELYTRA))
                    {
                        int elytraSlot = findItemSlot(Items.ELYTRA);
                        if (elytraSlot != -1)
                        {
                            Managers.INVENTORY.setSlot(elytraSlot);
                        }
                    }
                }
            }
            else
            {
                if (mc.player.isFallFlying())
                {
                    int chestSlot = -1;
                    if (InventoryUtil.hasItemInHotbar(Items.DIAMOND_CHESTPLATE))
                    {
                        chestSlot = findItemSlot(Items.DIAMOND_CHESTPLATE);
                    }
                    else if (InventoryUtil.hasItemInHotbar(Items.NETHERITE_CHESTPLATE))
                    {
                        chestSlot = findItemSlot(Items.NETHERITE_CHESTPLATE);
                    }
                    if (chestSlot != -1)
                    {
                        Managers.INVENTORY.setSlot(chestSlot);
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Ignore errors
        }
    }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event)
    {
        try
        {
            target = getTarget();

            attacking = false;
            shouldAttack = false;

            if (target == null || mc.player == null)
            {
                return;
            }

            if (mc.player.distanceTo(target) > attackRangeConfig.getValue())
            {
                return;
            }

            if (!InventoryUtil.hasItemInHotbar(Items.MACE))
            {
                return;
            }

            if (rotateConfig.getValue())
            {
                float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), target.getEyePos());
                setRotation(rotations[0], rotations[1]);
            }

            attacking = true;

            if (!timer.passed(1000))
            {
                return;
            }

            shouldAttack = true;
        }
        catch (Exception e)
        {
            // Ignore errors
        }
    }

    @EventListener
    public void onUpdatePost(PlayerUpdateEvent event)
    {
        try
        {
            if (mc.player == null || !shouldAttack || !attacking || target == null)
            {
                shouldAttack = false;
                return;
            }

            int slot = findItemSlot(Items.MACE);
            if (slot == -1)
            {
                return;
            }

            int previousSlot = mc.player.getInventory().selectedSlot;

            if (slot != previousSlot)
            {
                Managers.INVENTORY.setSlot(slot);
            }

            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

            if (slot != previousSlot)
            {
                Managers.INVENTORY.setSlot(previousSlot);
            }

            if (rubberbandConfig.getValue())
            {
                doRubberband();
            }

            shouldAttack = false;
            timer.reset();
        }
        catch (Exception e)
        {
            // Ignore errors
        }
    }

    private void doRubberband()
    {
        try
        {
            if (mc.player != null)
            {
                Vec3d pos = mc.player.getPos();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y + 1, pos.z, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false));
            }
        }
        catch (Exception e)
        {
            // Ignore errors
        }
    }

    private int findItemSlot(Item item)
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.getInventory().getStack(i).getItem() == item)
            {
                return i;
            }
        }
        return -1;
    }

    private PlayerEntity getTarget()
    {
        if (mc.world == null || mc.player == null)
        {
            return null;
        }

        PlayerEntity optimalTarget = null;
        for (PlayerEntity player : mc.world.getPlayers())
        {
            if (player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(targetRangeConfig.getValue())) continue;

            if (optimalTarget == null)
            {
                optimalTarget = player;
                continue;
            }

            if (mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget))
            {
                optimalTarget = player;
            }
        }

        return optimalTarget;
    }
}
