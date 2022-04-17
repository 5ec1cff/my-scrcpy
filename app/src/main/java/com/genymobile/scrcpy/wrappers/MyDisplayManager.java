package com.genymobile.scrcpy.wrappers;

import com.genymobile.scrcpy.DisplayInfo;
import com.genymobile.scrcpy.Size;

import android.annotation.SuppressLint;
import android.hardware.display.VirtualDisplay;
import android.os.Handler;
import android.os.IInterface;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// TODO: use android's DisplayManager API instead
@SuppressLint("PrivateApi")
public final class MyDisplayManager {
    private final IInterface manager;

    private Method createVirtualDisplayMethod;
    private static Object globalCallback = null;

    public MyDisplayManager(IInterface manager) {
        this.manager = manager;
        try {
            createVirtualDisplayMethod = manager.getClass().getMethod("createVirtualDisplay",
                    Class.forName("android.hardware.display.IVirtualDisplayCallback"),
                    Class.forName("android.media.projection.IMediaProjection"),
                    String.class, String.class, int.class, int.class, int.class, Surface.class,
                    int.class, String.class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DisplayInfo getDisplayInfo(int displayId) {
        try {
            Object displayInfo = manager.getClass().getMethod("getDisplayInfo", int.class).invoke(manager, displayId);
            if (displayInfo == null) {
                return null;
            }
            Class<?> cls = displayInfo.getClass();
            // width and height already take the rotation into account
            int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
            int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
            int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
            int layerStack = cls.getDeclaredField("layerStack").getInt(displayInfo);
            int flags = cls.getDeclaredField("flags").getInt(displayInfo);
            int dpi = cls.getDeclaredField("logicalDensityDpi").getInt(displayInfo);
            return new DisplayInfo(displayId, new Size(width, height), rotation, layerStack, flags, dpi);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public int[] getDisplayIds() {
        try {
            return (int[]) manager.getClass().getMethod("getDisplayIds").invoke(manager);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object getCallback() {
        if (globalCallback == null) {
            try {
                Constructor<?> c = Class.forName("android.hardware.display.DisplayManagerGlobal$VirtualDisplayCallback")
                        .getConstructor(VirtualDisplay.Callback.class, Handler.class);
                c.setAccessible(true);
                globalCallback = c.newInstance(null, null);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
                System.out.println("failed to create callback");
            }
        }
        return globalCallback;
    }

    // supports Android 10 only
    public int createVirtualDisplay(String name, int width, int height, int densityDpi, int flags) {
        try {
            String packageName;
            if (android.os.Process.myUid() == 1000) {
                packageName = "android";
            } else {
                packageName = "com.android.shell";
            }
            return (int) createVirtualDisplayMethod.invoke(manager, getCallback(), null,
                    packageName, name, width, height, densityDpi, null, flags, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void setVirtualDisplaySurface(Surface surface) {
        try {
            manager.getClass().getMethod("setVirtualDisplaySurface",
                    Class.forName("android.hardware.display.IVirtualDisplayCallback"),
                    Surface.class).invoke(manager, getCallback(), surface);
            setVirtualDisplayState(true);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void releaseVirtualDisplay() {
        try {
            manager.getClass().getMethod("releaseVirtualDisplay",
                    Class.forName("android.hardware.display.IVirtualDisplayCallback"))
                    .invoke(manager, getCallback());
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void setVirtualDisplayState(boolean isOn) {
        try {
            manager.getClass().getMethod("setVirtualDisplayState",
                    Class.forName("android.hardware.display.IVirtualDisplayCallback"), boolean.class)
                    .invoke(manager, getCallback(), isOn);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
