package android.window;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IWindowContainerToken extends IInterface {
    class Stub extends Binder {
        public static IWindowContainerToken asInterface(IBinder b) {
            throw new UnsupportedOperationException("STUB!");
        }
    }
}
