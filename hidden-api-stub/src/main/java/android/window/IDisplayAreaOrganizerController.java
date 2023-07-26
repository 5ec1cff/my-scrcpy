package android.window;

import android.os.Binder;
import android.os.IBinder;

public interface IDisplayAreaOrganizerController {

    /** Register a DisplayAreaOrganizer to manage display areas for a given feature. */
    void registerOrganizer(IDisplayAreaOrganizer organizer, int displayAreaFeature);

    /**
     * Unregisters a previously registered display area organizer.
     */
    void unregisterOrganizer(IDisplayAreaOrganizer organizer);

    class Stub extends Binder {
        public static IDisplayAreaOrganizerController asInterface(IBinder b) {
            throw new UnsupportedOperationException("STUB!");
        }
    }
}
