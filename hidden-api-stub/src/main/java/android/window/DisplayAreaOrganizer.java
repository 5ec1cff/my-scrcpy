package android.window;

public class DisplayAreaOrganizer {
    public static final int FEATURE_UNDEFINED = -1;
    public static final int FEATURE_SYSTEM_FIRST = 0;
    // The Root display area on a display
    public static final int FEATURE_ROOT = FEATURE_SYSTEM_FIRST;
    // Display area hosting the default task container.
    public static final int FEATURE_DEFAULT_TASK_CONTAINER = FEATURE_SYSTEM_FIRST + 1;
    // Display area hosting non-activity window tokens.
    public static final int FEATURE_WINDOW_TOKENS = FEATURE_SYSTEM_FIRST + 2;

    public static final int FEATURE_SYSTEM_LAST = 10_000;

    // Vendor specific display area definition can start with this value.
    public static final int FEATURE_VENDOR_FIRST = FEATURE_SYSTEM_LAST + 1;
}
