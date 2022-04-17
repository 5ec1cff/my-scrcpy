package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IWindowManager extends IInterface {

    // void freezeRotation(int rotation);
    void freezeDisplayRotation(int displayId, int rotation);
    // boolean isRotationFrozen();
    boolean isDisplayRotationFrozen(int displayId);
    // void thawRotation();
    void thawDisplayRotation(int displayId);

    /**
     * Watch the rotation of the specified screen.  Returns the current rotation,
     * calls back when it changes.
     */
    int watchRotation(IRotationWatcher watcher, int displayId);
    void removeRotationWatcher(IRotationWatcher watcher);
    // int watchRotation(IRotationWatcher watcher);

    abstract class Stub extends Binder implements IWindowManager {
        public static IWindowManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
