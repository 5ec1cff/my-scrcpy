package five.ec1cff.scrcpy.util;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BinderWrapper extends Binder {
    private static final String TAG = "BinderWrapper";
    private final IBinder target;
    public BinderWrapper(IBinder target) {
        this.target = target;
        Log.d(TAG, "wrapper for " + target + " created");
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        Log.v(TAG, "wrapper for " + target + " transact code=" + code);
        return target.transact(code, data, reply, flags);
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "wrapper for " + target + " destroyed");
        super.finalize();
    }
}
