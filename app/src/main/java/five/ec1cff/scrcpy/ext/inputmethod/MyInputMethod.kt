package five.ec1cff.scrcpy.ext.inputmethod

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import android.util.Log
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import five.ec1cff.scrcpy.ext.shared.IInputMethod
import five.ec1cff.scrcpy.ext.shared.IInputMethodClient

class MyInputMethod : InputMethodService() {
    companion object {
        private const val TAG = "inputmethod"
        var instance: MyInputMethod? = null

        val inputMethod = object : IInputMethod.Stub() {
            override fun setClient(binder: IBinder) {
                client = IInputMethodClient.Stub.asInterface(binder)
                Log.d(TAG, "set client: $client")
                try {
                    binder.linkToDeath(DeathRecipient { onBinderDied() }, 0)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun isInputMethodAvaliable(): Boolean {
                return instance != null
            }

            override fun commitText(c: CharSequence?) {
                Log.d(TAG, "commit $c")
                instance?.commitTextInternal(c)
            }

            override fun commitComposingText(c: CharSequence?) {
                Log.d(TAG, "commitComposing $c")
                instance?.commitComposingTextInternal(c)
            }

        }

        var client: IInputMethodClient? = null

        fun onBinderDied() {
            client = null
        }
    }

    fun commitTextInternal(c: CharSequence?) {
        val ic = currentInputConnection
        ic?.commitText(c, 1)
    }

    fun commitComposingTextInternal(c: CharSequence?) {
        val ic = currentInputConnection
        ic?.setComposingText(c, 1)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "created")
        // startService(Intent(this, HelperService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "destroyed")
        if (HelperService.get() != null) {
            HelperService.get().dismissAll()
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(
            TAG,
            "onStartInput:attr=$attribute,restarting=$restarting"
        )
        updateCursor("onStartInput", null)
        val connection = currentInputConnection
        if (connection != null) {
            Log.d(TAG, "current connection:$connection")
            val result =
                connection.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR)
            Log.d(
                TAG,
                "requested cursor update, result=$result"
            )
        } else {
            Log.d(TAG, "connection is null")
        }
    }

    override fun onBindInput() {
        super.onBindInput()
        Log.d(TAG, "onBindInput")
        updateCursor("onBindInput", null)
    }

    override fun onUnbindInput() {
        Log.d(TAG, "onUnbindInput")
        updateCursor("onUnbindInput", null)
        super.onUnbindInput()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        updateCursor("onFinishInput", null)
        Log.d(TAG, "onFinishInput")
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo) {
        Log.d(TAG, "updateCursorAnchorInfo$info")
        updateCursor("ims", info)
        super.onUpdateCursorAnchorInfo(info)
    }

    fun updateCursor(reason: String?, info: CursorAnchorInfo?) {
        if (HelperService.get() != null) {
            HelperService.get().updateCursor(reason, info)
        }
        if (client != null) {
            if (info == null) {
                return
            }
            val pts = floatArrayOf(info.insertionMarkerHorizontal, info.insertionMarkerBaseline)
            info.matrix.mapPoints(pts)
            if (java.lang.Float.isNaN(pts[0]) || java.lang.Float.isNaN(pts[1])) {
                Log.w(TAG, "cursor is NaN")
                return
            }
            try {
                client!!.onCursorChanged(pts[0], pts[1])
            } catch (e: RemoteException) {
                Log.e(TAG, "failed to push cursor change", e)
            }
        }
    }

    override fun onWindowHidden() {
        Log.d(TAG, "onWindowHidden")
        if (client != null) {
            try {
                client!!.onInputFinished()
            } catch (e: RemoteException) {
                Log.e(TAG, "failed to push input finished", e)
            }
        }
    }

    override fun onWindowShown() {
        Log.d(TAG, "onWindowShown")
        if (client != null) {
            try {
                client!!.onInputStarted()
            } catch (e: RemoteException) {
                Log.e(TAG, "failed to push input started", e)
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