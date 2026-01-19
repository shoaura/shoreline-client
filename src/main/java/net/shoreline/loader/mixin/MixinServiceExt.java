package net.shoreline.loader.mixin;

import net.fabricmc.loader.impl.launch.knot.MixinServiceKnot;

import java.io.IOException;
import java.io.InputStream;

public final class MixinServiceExt extends MixinServiceKnot
{
    @Override
    public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException
    {
        byte[] bytes;
        if ((bytes = (byte[]) getInternalClassBytes(name)) != null)
        {
            return bytes;
        }

        return super.getClassBytes(name, runTransformers);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        InputStream is;
        if ((is = (InputStream) getInternalInputStream(name)) != null)
        {
            return is;
        }

        return super.getResourceAsStream(name);
    }

    private Object getInternalClassBytes(Object name)
    {
        return null;
    }

    private Object getInternalInputStream(Object name)
    {
        return null;
    }
}
