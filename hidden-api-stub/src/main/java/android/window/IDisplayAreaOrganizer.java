package android.window;

import android.os.Binder;
import android.os.IBinder;
import android.view.SurfaceControl;

public interface IDisplayAreaOrganizer {
    void onDisplayAreaAppeared(DisplayAreaInfo displayAreaInfo, SurfaceControl leash);
    void onDisplayAreaVanished(DisplayAreaInfo displayAreaInfo);
    void onDisplayAreaInfoChanged(DisplayAreaInfo displayAreaInfo);

    abstract class Stub extends Binder implements IDisplayAreaOrganizer {
        public static IDisplayAreaOrganizer asInterface(IBinder b) {
            throw new UnsupportedOperationException("STUB!");
        }
    }
}
