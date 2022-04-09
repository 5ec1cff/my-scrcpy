package com.genymobile.scrcpy.ext;

import android.content.IContentProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.genymobile.scrcpy.Ln;
import com.genymobile.scrcpy.wrappers.ActivityManager;
import com.genymobile.scrcpy.wrappers.ContentProvider;
import com.genymobile.scrcpy.wrappers.ServiceManager;

import five.ec1cff.scrcpy.ext.shared.IInputMethod;
import five.ec1cff.scrcpy.ext.shared.IInputMethodClient;

public class IMEController {
    private static final String AUTHORITY = "com.genymobile.scrcpy.ext";
    private static final String METHOD_SEND_BINDER = "sendBinder";
    private static final String EXTRA_BINDER = "EXTRA_BINDER";

    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            method = null;
        }
    };

    public interface Listener {
        void onInputStarted();
        void onInputFinished();
        void onCursorChanged(float x, float y);
    }

    static IMEController instance;

    public static IMEController get() {
        if (instance == null) {
            instance = new IMEController();
        }
        return instance;
    }

    IInputMethod method;
    boolean isInputStarted;
    Listener listener;

    final IInputMethodClient client = new IInputMethodClient.Stub() {
        @Override
        public void onBindInput(IBinder binder) throws RemoteException {
            if (method != null) {
                Ln.e("duplicated bind");
                return;
            }
            method = IInputMethod.Stub.asInterface(binder);
            if (method != null) {
                Ln.i("bind inputmethod:" + method);
                binder.linkToDeath(deathRecipient, 0);
            } else {
                Ln.e("bind inputmethod: asInterface failed");
            }
        }

        @Override
        public void onInputStarted() throws RemoteException {
            isInputStarted = true;
            Ln.d("onInputStarted");
            if (listener != null) {
                listener.onInputStarted();
            }
        }

        @Override
        public void onInputFinished() throws RemoteException {
            isInputStarted = false;
            Ln.d("onInputFinished");
            if (listener != null) {
                listener.onInputFinished();
            }
        }

        @Override
        public void onCursorChanged(float x, float y) throws RemoteException {
            Ln.d("onCursorChanged:" + x + "," + y);
            if (listener != null) {
                listener.onCursorChanged(x, y);
            }
        }
    };

    public boolean connectToIME() {
        ActivityManager am = ServiceManager.get().getActivityManager();
        IBinder token = null;
        ContentProvider provider = am.getContentProviderExternal(AUTHORITY, token);
        if (provider == null) {
            Ln.e("ext provider is null!");
            return false;
        }
        IContentProvider ic = provider.getIContentProvider();
        if (ic == null) {
            Ln.e("ext iprovider is null!");
            return false;
        }
        Bundle extra = new Bundle();
        extra.putBinder(EXTRA_BINDER, client.asBinder());
        try {
            Bundle reply = ContentProvider.callCompat(ic, null, AUTHORITY,
                    METHOD_SEND_BINDER, null, extra);
            if (reply != null) {
                Ln.d("call success");
                return true;
            } else {
                Ln.e("call failed, no reply");
                return false;
            }
        } catch (RemoteException e) {
            Ln.e("call failed", e);
            return false;
        } finally {
            am.removeContentProviderExternal(AUTHORITY, token);
        }
    }

    public boolean isIMENeeded() {
        if (method == null) {
            return false;
        }
        return isInputStarted;
    }

    public boolean commitText(String text) {
        try {
            method.commitText(text);
            return true;
        } catch (RemoteException e) {
            Ln.e("failed to commit text", e);
            return false;
        }
    }

    public boolean commitComposingText(String text) {
        try {
            method.commitComposingText(text);
            return true;
        } catch (RemoteException e) {
            Ln.e("failed to commit text", e);
            return false;
        }
    }

    public void setListener(Listener l) {
        listener = l;
    }
}
