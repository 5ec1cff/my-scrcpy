package android.window;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.IBinder;

import java.util.List;

public interface ITaskOrganizerController {

    /**
     * Register a TaskOrganizer to manage tasks as they enter the given windowing mode.
     * If there was already a TaskOrganizer for this windowing mode it will be evicted
     * and receive taskVanished callbacks the process.
     */
    void registerTaskOrganizer(ITaskOrganizer organizer, int windowingMode);

    /**
     * Unregisters a previously registered task organizer.
     */
    void unregisterTaskOrganizer(ITaskOrganizer organizer);

    /** Creates a persistent root task WM for a particular windowing-mode. */
    ActivityManager.RunningTaskInfo createRootTask(int displayId, int windowingMode);

    /** Deletes a persistent root task WM */
    boolean deleteRootTask(WindowContainerToken task);

    /** Gets direct child tasks (ordered from top-to-bottom) */
    List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken parent,
                                                        int[] activityTypes);

    /** Gets all root tasks on a display (ordered from top-to-bottom) */
    List<ActivityManager.RunningTaskInfo> getRootTasks(int displayId, int[] activityTypes);

    /** Get the root task which contains the current ime target */
    WindowContainerToken getImeTarget(int display);

    /**
     * Set's the root task to launch new tasks into on a display. {@code null} means no launch root
     * and thus new tasks just end up directly on the display.
     */
    void setLaunchRoot(int displayId, WindowContainerToken root);

    /**
     * Requests that the given task organizer is notified when back is pressed on the root activity
     * of one of its controlled tasks.
     */
    void setInterceptBackPressedOnTaskRoot(ITaskOrganizer organizer, boolean interceptBackPressed);

    class Stub extends Binder {
        public static ITaskOrganizerController asInterface(IBinder b) {
            throw new UnsupportedOperationException("STUB!");
        }
    }
}
