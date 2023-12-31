package com.genymobile.scrcpy.wrappers;

import com.genymobile.scrcpy.Ln;

import android.annotation.SuppressLint;
import android.content.IContentProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ContentProvider implements Closeable {

    public static final String TABLE_SYSTEM = "system";
    public static final String TABLE_SECURE = "secure";
    public static final String TABLE_GLOBAL = "global";

    // See android/providerHolder/Settings.java
    private static final String CALL_METHOD_GET_SYSTEM = "GET_system";
    private static final String CALL_METHOD_GET_SECURE = "GET_secure";
    private static final String CALL_METHOD_GET_GLOBAL = "GET_global";

    private static final String CALL_METHOD_PUT_SYSTEM = "PUT_system";
    private static final String CALL_METHOD_PUT_SECURE = "PUT_secure";
    private static final String CALL_METHOD_PUT_GLOBAL = "PUT_global";

    private static final String CALL_METHOD_USER_KEY = "_user";

    private static final String NAME_VALUE_TABLE_VALUE = "value";

    private final ActivityManager manager;
    // android.content.IContentProvider
    private final Object provider;
    private final String name;
    private final IBinder token;

    private Method callMethod;
    private int callMethodVersion;

    private Object attributionSource;

    ContentProvider(ActivityManager manager, Object provider, String name, IBinder token) {
        this.manager = manager;
        this.provider = provider;
        this.name = name;
        this.token = token;
    }

    @SuppressLint("PrivateApi")
    private Method getCallMethod() throws NoSuchMethodException {
        if (callMethod == null) {
            try {
                Class<?> attributionSourceClass = Class.forName("android.content.AttributionSource");
                callMethod = provider.getClass().getMethod("call", attributionSourceClass, String.class, String.class, String.class, Bundle.class);
                callMethodVersion = 0;
            } catch (NoSuchMethodException | ClassNotFoundException e0) {
                // old versions
                try {
                    callMethod = provider.getClass()
                            .getMethod("call", String.class, String.class, String.class, String.class, String.class, Bundle.class);
                    callMethodVersion = 1;
                } catch (NoSuchMethodException e1) {
                    try {
                        callMethod = provider.getClass().getMethod("call", String.class, String.class, String.class, String.class, Bundle.class);
                        callMethodVersion = 2;
                    } catch (NoSuchMethodException e2) {
                        callMethod = provider.getClass().getMethod("call", String.class, String.class, String.class, Bundle.class);
                        callMethodVersion = 3;
                    }
                }
            }
        }
        return callMethod;
    }

    @SuppressLint("PrivateApi")
    private Object getAttributionSource()
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (attributionSource == null) {
            Class<?> cl = Class.forName("android.content.AttributionSource$Builder");
            Object builder = cl.getConstructor(int.class).newInstance(ServiceManager.USER_ID);
            cl.getDeclaredMethod("setPackageName", String.class).invoke(builder, ServiceManager.PACKAGE_NAME);
            attributionSource = cl.getDeclaredMethod("build").invoke(builder);
        }

        return attributionSource;
    }

    private Bundle call(String callMethod, String arg, Bundle extras) {
        try {
            Method method = getCallMethod();
            Object[] args;
            switch (callMethodVersion) {
                case 0:
                    args = new Object[]{getAttributionSource(), "settings", callMethod, arg, extras};
                    break;
                case 1:
                    args = new Object[]{ServiceManager.PACKAGE_NAME, null, "settings", callMethod, arg, extras};
                    break;
                case 2:
                    args = new Object[]{ServiceManager.PACKAGE_NAME, "settings", callMethod, arg, extras};
                    break;
                default:
                    args = new Object[]{ServiceManager.PACKAGE_NAME, callMethod, arg, extras};
                    break;
            }
            return (Bundle) method.invoke(provider, args);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException | InstantiationException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    public void close() {
        manager.removeContentProviderExternal(name, token);
    }

    private static String getGetMethod(String table) {
        switch (table) {
            case TABLE_SECURE:
                return CALL_METHOD_GET_SECURE;
            case TABLE_SYSTEM:
                return CALL_METHOD_GET_SYSTEM;
            case TABLE_GLOBAL:
                return CALL_METHOD_GET_GLOBAL;
            default:
                throw new IllegalArgumentException("Invalid table: " + table);
        }
    }

    private static String getPutMethod(String table) {
        switch (table) {
            case TABLE_SECURE:
                return CALL_METHOD_PUT_SECURE;
            case TABLE_SYSTEM:
                return CALL_METHOD_PUT_SYSTEM;
            case TABLE_GLOBAL:
                return CALL_METHOD_PUT_GLOBAL;
            default:
                throw new IllegalArgumentException("Invalid table: " + table);
        }
    }

    public String getValue(String table, String key) {
        String method = getGetMethod(table);
        Bundle arg = new Bundle();
        arg.putInt(CALL_METHOD_USER_KEY, ServiceManager.USER_ID);
        Bundle bundle = call(method, key, arg);
        if (bundle == null) {
            return null;
        }
        return bundle.getString("value");
    }

    public void putValue(String table, String key, String value) {
        String method = getPutMethod(table);
        Bundle arg = new Bundle();
        arg.putInt(CALL_METHOD_USER_KEY, ServiceManager.USER_ID);
        arg.putString(NAME_VALUE_TABLE_VALUE, value);
        call(method, key, arg);
    }

    public String getAndPutValue(String table, String key, String value) {
        String oldValue = getValue(table, key);
        if (!value.equals(oldValue)) {
            putValue(table, key, value);
        }
        return oldValue;
    }

    // ext method

    public IContentProvider getIContentProvider() {
        return (IContentProvider) provider;
    }

    public static Bundle callCompat(IContentProvider provider, String callingPkg, String authority, String method, String arg, Bundle extras) throws RemoteException {
        Bundle result;
        if (Build.VERSION.SDK_INT >= 30) {
            result = provider.call(callingPkg, (String) null, authority, method, arg, extras);
        } else if (Build.VERSION.SDK_INT >= 29) {
            result = provider.call(callingPkg, authority, method, arg, extras);
        } else {
            result = provider.call(callingPkg, method, arg, extras);
        }

        return result;
    }
}
