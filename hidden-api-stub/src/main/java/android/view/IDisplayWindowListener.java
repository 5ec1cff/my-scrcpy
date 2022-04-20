package android.view;

import android.content.res.Configuration;
import android.os.Binder;

public interface IDisplayWindowListener {

    /**
     * Called when a display is added to the WM hierarchy.
     */
    void onDisplayAdded(int displayId);

    /**
     * Called when a display's window-container configuration has changed.
     */
    void onDisplayConfigurationChanged(int displayId, Configuration newConfig);

    /**
     * Called when a display is removed from the hierarchy.
     */
    void onDisplayRemoved(int displayId);

    /**
     * Called when fixed rotation is started on a display.
     */
    void onFixedRotationStarted(int displayId, int newRotation);

    /**
     * Called when the previous fixed rotation on a display is finished.
     */
    void onFixedRotationFinished(int displayId);

    abstract class Stub implements IDisplayWindowListener {}
}
