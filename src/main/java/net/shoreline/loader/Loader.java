package net.shoreline.loader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.shoreline.loader.session.UserSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

public class Loader implements
		ClientModInitializer, PreLaunchEntrypoint, // Fabric
		IMixinConfigPlugin // Sponge
{
	private static final Logger LOGGER = LogManager.getLogger("Shoreline");
	public static final String VERSION = "r1.0.2";

	public static final UserSession SESSION;

	static
	{
		info("Loading Shoreline...");

		SESSION = UserSession.load();
		performVersionCheck(VERSION);
	}

	/* -------------------------------- Fabric --------------------------------*/

	@Override
	public void onPreLaunch()
	{
		// Pre-launch hook
	}

	@Override
	public void onInitializeClient()
	{
		// Initialize Shoreline client
		try
		{
			Class<?> shorelineClass = Class.forName("net.shoreline.client.Shoreline");
			java.lang.reflect.Method initMethod = shorelineClass.getDeclaredMethod("init");
			initMethod.setAccessible(true);
			initMethod.invoke(null);
		}
		catch (Exception e)
		{
			error("Failed to initialize Shoreline: %s", e.getMessage());
		}
	}

	/* -------------------------------- Sponge --------------------------------*/

	@Override
	public void onLoad(String mixinPackage)
	{
		// Mixin load hook
	}

	@Override
	public String getRefMapperConfig()
	{
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName,
									   String mixinClassName)
	{
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets,
									 Set<String> otherTargets)
	{
		// Accept all targets
	}

	@Override
	public List<String> getMixins()
	{
		return List.of();
	}

	@Override
	public void preApply(String targetClassName,
								ClassNode targetClass,
								String mixinClassName,
								IMixinInfo mixinInfo)
	{
		// Pre-apply hook
	}

	@Override
	public void postApply(String targetClassName,
								 ClassNode targetClass,
								 String mixinClassName,
								 IMixinInfo mixinInfo)
	{
		// Post-apply hook
	}

	/* ------------------------------------------------------------------------*/

	private static String getExt()
	{
		String os_name = System.getProperty("os.name");

		if (os_name.contains("Windows"))
		{
			return "dll";
		}

		if (os_name.contains("Linux"))
		{
			return "so";
		}

		if (os_name.contains("OS X"))
		{
			return "dylib";
		}

		Loader.error("Unsupported OS: {}", os_name);
		throw new IllegalStateException("Unsupported OS: " + os_name);
	}

	private static void performVersionCheck(Object currentVersion)
	{
		// Version check implementation
	}

	public static Object showErrorWindow(Object message)
	{
		JOptionPane.showMessageDialog(null, message.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		return null;
	}

	public static void info(String message)
	{
		LOGGER.info(String.format("[Shoreline] %s", message));
	}

	public static void info(String message, Object... params)
	{
		LOGGER.info(String.format("[Shoreline] %s", message), params);
	}

	public static void error(String message)
	{
		LOGGER.error(String.format("[Shoreline] %s", message));
	}

	public static void error(String message, Object... params)
	{
		LOGGER.error(String.format("[Shoreline] %s", message), params);
	}

	public static InputStream getResource(String name)
	{
		return Loader.class.getClassLoader().getResourceAsStream(name);
	}
}