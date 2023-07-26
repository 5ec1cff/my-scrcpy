package android.window;

import android.os.Binder;
import android.os.IBinder;

public interface IWindowOrganizerController {
    /**
     * Apply multiple WindowContainer operations at once.
     * @param t The transaction to apply.
     */
    void applyTransaction(WindowContainerTransaction t);

    /** @return An interface enabling the management of task organizers. */
    ITaskOrganizerController getTaskOrganizerController();

    /** @return An interface enabling the management of display area organizers. */
    IDisplayAreaOrganizerController getDisplayAreaOrganizerController();

    class Stub extends Binder {
        public static IWindowOrganizerController asInterface(IBinder b) {
            throw new UnsupportedOperationException("STUB!");
        }
    }
}
