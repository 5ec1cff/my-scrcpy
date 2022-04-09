package five.ec1cff.scrcpy.ext.inputmethod;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import five.ec1cff.scrcpy.ext.shared.IInputMethod;
import five.ec1cff.scrcpy.ext.shared.IInputMethodClient;

public class MyInputMethod extends InputMethodService {
    private static final String TAG = "inputmethod";
    HelperService helper = null;
    static MyInputMethod instance = null;
    static MyInputMethod get() {
        return instance;
    }

    static IInputMethodClient client;
    public static void onBindScrcpyInputMethod(IBinder binder) {
        if (client != null) {
            Log.e(TAG, "failed to bind, already bound yet");
            return;
        }
        if (binder != null) {
            if (!binder.pingBinder()) {
                Log.e(TAG, "ping failed");
                return;
            }
            Log.d(TAG, "received binder=" + binder);
            client = IInputMethodClient.Stub.asInterface(binder);
            if (client == null) {
                Log.e(TAG, "asInterface failed");
                return;
            }
            try {
                binder.linkToDeath(MyInputMethod::onBinderDied, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (instance != null) {
                instance.onBindScrcpyInputMethod();
            }
        } else {
            Log.e(TAG, "received null");
        }
    }

    public static void onBinderDied() {
        client = null;
    }

    IInputMethod method = new IInputMethod.Stub() {
        @Override
        public void commitText(CharSequence c) throws RemoteException {
            commitTextInternal(c);
        }

        @Override
        public void commitComposingText(CharSequence c) throws RemoteException {
            commitComposingTextInternal(c);
        }
    };

    void onBindScrcpyInputMethod() {
        try {
            client.onBindInput(method.asBinder());
        } catch (Throwable t) {
            Log.e(TAG, "onBindInput failed");
            t.printStackTrace();
        }
    }

    void commitTextInternal(CharSequence c) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(c, 1);
        }
    }

    void commitComposingTextInternal(CharSequence c) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText(c, 1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "created");
        if (client != null) {
            onBindScrcpyInputMethod();
        }
        startService(new Intent(this, HelperService.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "destroyed");
        if (HelperService.get() != null) {
            HelperService.get().dismissAll();
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d(TAG, "onStartInput:attr=" + attribute + ",restarting=" + restarting);
        updateCursor("onStartInput", null);
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            Log.d(TAG, "current connection:" + connection);
            boolean result = connection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE | InputConnection.CURSOR_UPDATE_MONITOR);
            Log.d(TAG, "requested cursor update, result=" + result);
        } else {
            Log.d(TAG, "connection is null");
        }
    }

    @Override
    public void onBindInput() {
        super.onBindInput();
        Log.d(TAG, "onBindInput");
        updateCursor("onBindInput", null);
    }

    @Override
    public void onUnbindInput() {
        Log.d(TAG, "onUnbindInput");
        updateCursor("onUnbindInput", null);
        super.onUnbindInput();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        updateCursor("onFinishInput", null);
        Log.d(TAG, "onFinishInput");
    }

    @Override
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo info) {
        Log.d(TAG, "updateCursorAnchorInfo" + info.toString());
        updateCursor("ims", info);
        super.onUpdateCursorAnchorInfo(info);
    }

    void updateCursor(String reason, CursorAnchorInfo info) {
        if (HelperService.get() != null) {
            HelperService.get().updateCursor(reason, info);
        }
        if (client != null) {
            if (info == null) {
                return;
            }
            float[] pts = new float[]{info.getInsertionMarkerHorizontal(), info.getInsertionMarkerBaseline()};
            info.getMatrix().mapPoints(pts);
            if (Float.isNaN(pts[0]) || Float.isNaN(pts[1])) {
                Log.w(TAG, "cursor is NaN");
                return;
            }
            try {
                client.onCursorChanged(pts[0], pts[1]);
            } catch (RemoteException e) {
                Log.e(TAG, "failed to push cursor change", e);
            }
        }
    }

    @Override
    public void onWindowHidden() {
        Log.d(TAG, "onWindowHidden");
        if (client != null) {
            try {
                client.onInputFinished();
            } catch (RemoteException e) {
                Log.e(TAG, "failed to push input finished", e);
            }
        }
    }

    @Override
    public void onWindowShown() {
        Log.d(TAG, "onWindowShown");
        if (client != null) {
            try {
                client.onInputStarted();
            } catch (RemoteException e) {
                Log.e(TAG, "failed to push input started", e);
            }
        }
    }

    /*@Override
    public AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface() {
        // return super.onCreateInputMethodSessionInterface();
        Log.d(TAG, "session created");
        return new InputMethodSessionImpl() {
            @Override
            public void updateCursorAnchorInfo(CursorAnchorInfo info) {
                Log.d(TAG, "session#updateCursorAnchorInfo" + info.toString());
                MyInputMethod.updateCursor(info);
                super.updateCursorAnchorInfo(info);
            }

            @Override
            public void updateCursor(Rect newCursor) {
                Log.d(TAG, "session#updateCursor" + newCursor.toString());
                super.updateCursor(newCursor);
            }
        };
    }*/
}