package com.genymobile.scrcpy;

import com.genymobile.scrcpy.wrappers.ClipboardManager;
import com.genymobile.scrcpy.wrappers.ContentProvider;
import com.genymobile.scrcpy.wrappers.MyDisplayManager;
import com.genymobile.scrcpy.wrappers.InputManager;
import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.genymobile.scrcpy.wrappers.SurfaceControl;

import android.content.IOnPrimaryClipChangedListener;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;

import five.ec1cff.scrcpy.ScrcpyServer;

public final class ScreenDevice extends IRotationWatcher.Stub {

    public static final int POWER_MODE_OFF = SurfaceControl.POWER_MODE_OFF;
    public static final int POWER_MODE_NORMAL = SurfaceControl.POWER_MODE_NORMAL;

    public static final int LOCK_VIDEO_ORIENTATION_UNLOCKED = -1;
    public static final int LOCK_VIDEO_ORIENTATION_INITIAL = -2;

    private static final ServiceManager SERVICE_MANAGER = ServiceManager.get();
    private static final DisplayManager displayManager = ScrcpyServer.INSTANCE.getContext().getSystemService(DisplayManager.class);

    public interface RotationListener {
        void onRotationChanged(int rotation);
    }

    public interface ClipboardListener {
        void onClipboardTextChanged(String text);
    }

    private ScreenInfo screenInfo;
    private RotationListener rotationListener;
    private ClipboardListener clipboardListener;
    private final AtomicBoolean isSettingClipboard = new AtomicBoolean();

    /**
     * Logical display identifier
     */
    public int displayId;
    private int currentRotation = 0;

    /**
     * The surface flinger layer stack associated with this logical display
     */
    private final int layerStack;

    private final boolean supportsInputEvents;

    private VirtualDisplay virtualDisplay;

    public boolean isVirtual = false;

    private boolean rotationWatcherRegistered = false;

    private DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {
            if (i == displayId) {
                Ln.d("virtual display created");
                registerRotationWatcher();
                displayManager.unregisterDisplayListener(this);
            }
        }

        @Override
        public void onDisplayRemoved(int i) {

        }

        @Override
        public void onDisplayChanged(int i) {

        }
    };

    public ScreenDevice(int requestedDisplayId, int maxSize, int lockedVideoOrientation) {
        MyDisplayManager myDisplayManager = SERVICE_MANAGER.getDisplayManager();
        displayId = requestedDisplayId;
        DisplayInfo displayInfo = myDisplayManager.getDisplayInfo(displayId);
        if (displayInfo == null) {
            Ln.d("no display found, try create virtual display");
            // get main screen display info
            DisplayInfo mainDisplay = myDisplayManager.getDisplayInfo(0);
            Size mainDisplaySize = mainDisplay.getSize();
            int flags;
            // Process.myUid() returns original uid (0) even if we have called seteuid(1000) (?)
            if (true || android.os.Process.myUid() == 1000) {
                Ln.d("create with system perm");
                // perm check pass only if binder caller uid=1000 and package belongs to android
                flags = VIRTUAL_DISPLAY_FLAG_PUBLIC | VIRTUAL_DISPLAY_FLAG_SECURE | VIRTUAL_DISPLAY_FLAG_PRESENTATION;
            } else {
                flags = VIRTUAL_DISPLAY_FLAG_PRESENTATION;
            }
            VirtualDisplay vd = displayManager.createVirtualDisplay("scrcpy_" + displayId,
                    mainDisplaySize.getWidth(), mainDisplaySize.getHeight(), mainDisplay.getDensityDpi(),
                    null, flags);
            if (vd != null) {
                displayId = vd.getDisplay().getDisplayId();
                displayInfo = myDisplayManager.getDisplayInfo(displayId);
                Ln.d("get virtual display:" + displayId);
                isVirtual = true;
                virtualDisplay = vd;
            } else {
                int[] displayIds = SERVICE_MANAGER.getDisplayManager().getDisplayIds();
                throw new InvalidDisplayIdException(displayId, displayIds);
            }
        }

        int displayInfoFlags = displayInfo.getFlags();

        screenInfo = ScreenInfo.computeScreenInfo(displayInfo, null, maxSize, lockedVideoOrientation);
        layerStack = displayInfo.getLayerStack();

        if (!isVirtual) {
            registerRotationWatcher();
        } else {
            displayManager.registerDisplayListener(listener, ScrcpyServer.INSTANCE.getHandler());
        }

        // TODO: move to control
        if (false) {
            // If control is enabled, synchronize Android clipboard to the computer automatically
            ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
            if (clipboardManager != null) {
                clipboardManager.addPrimaryClipChangedListener(new IOnPrimaryClipChangedListener.Stub() {
                    @Override
                    public void dispatchPrimaryClipChanged() {
                        if (isSettingClipboard.get()) {
                            // This is a notification for the change we are currently applying, ignore it
                            return;
                        }
                        synchronized (ScreenDevice.this) {
                            if (clipboardListener != null) {
                                String text = getClipboardText();
                                if (text != null) {
                                    clipboardListener.onClipboardTextChanged(text);
                                }
                            }
                        }
                    }
                });
            } else {
                Ln.w("No clipboard manager, copy-paste between device and computer will not work");
            }
        }

        if ((displayInfoFlags & DisplayInfo.FLAG_SUPPORTS_PROTECTED_BUFFERS) == 0) {
            Ln.w("Display doesn't have FLAG_SUPPORTS_PROTECTED_BUFFERS flag, mirroring can be restricted");
        }

        // main display or any display on Android >= Q
        supportsInputEvents = displayId == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        if (!supportsInputEvents) {
            Ln.w("Input events are not supported for secondary displays before Android 10");
        }
    }

    private void registerRotationWatcher() {
        currentRotation = SERVICE_MANAGER.getWindowManager().watchRotation(this, displayId);
        rotationWatcherRegistered = true;
    }

    @Override
    public void onRotationChanged(int rotation) {
        synchronized (ScreenDevice.this) {
            currentRotation = rotation;
            screenInfo = screenInfo.withDeviceRotation(rotation);

            // notify
            if (rotationListener != null) {
                rotationListener.onRotationChanged(rotation);
            }
        }
    }

    public synchronized ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    public int getLayerStack() {
        return layerStack;
    }

    public Point getPhysicalPoint(Position position) {
        // it hides the field on purpose, to read it with a lock
        @SuppressWarnings("checkstyle:HiddenField")
        ScreenInfo screenInfo = getScreenInfo(); // read with synchronization

        // ignore the locked video orientation, the events will apply in coordinates considered in the physical device orientation
        Size unlockedVideoSize = screenInfo.getUnlockedVideoSize();

        int reverseVideoRotation = screenInfo.getReverseVideoRotation();
        // reverse the video rotation to apply the events
        Position devicePosition = position.rotate(reverseVideoRotation);

        Size clientVideoSize = devicePosition.getScreenSize();
        if (!unlockedVideoSize.equals(clientVideoSize)) {
            // The client sends a click relative to a video with wrong dimensions,
            // the device may have been rotated since the event was generated, so ignore the event
            return null;
        }
        Rect contentRect = screenInfo.getContentRect();
        Point point = devicePosition.getPoint();
        int convertedX = contentRect.left + point.getX() * contentRect.width() / unlockedVideoSize.getWidth();
        int convertedY = contentRect.top + point.getY() * contentRect.height() / unlockedVideoSize.getHeight();
        return new Point(convertedX, convertedY);
    }

    public void toScreenPoint(float x, float y, float[] to) {
        ScreenInfo screenInfo = getScreenInfo();
        Rect contentRect = screenInfo.getContentRect();
        Size unlockedVideoSize = screenInfo.getUnlockedVideoSize();

        float convertedX = (x - contentRect.left) / contentRect.width() * unlockedVideoSize.getWidth();
        float convertedY = (y - contentRect.top) / contentRect.height() * unlockedVideoSize.getHeight();

        int reverseVideoRotation = screenInfo.getReverseVideoRotation();

        switch (reverseVideoRotation) {
            case 1:
                to[0] = unlockedVideoSize.getHeight() - convertedY;
                to[1] = convertedX;
            case 2:
                to[0] = unlockedVideoSize.getWidth() - convertedX;
                to[1] = unlockedVideoSize.getHeight() - convertedY;
            case 3:
                to[0] = convertedY;
                to[1] = unlockedVideoSize.getWidth() - convertedX;
            default:
                to[0] = convertedX;
                to[1] = convertedY;
        }
    }

    public static String getDeviceName() {
        return Build.MODEL;
    }

    public static boolean supportsInputEvents(int displayId) {
        return displayId == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public boolean supportsInputEvents() {
        return supportsInputEvents;
    }

    public static boolean injectEvent(InputEvent inputEvent, int displayId) {
        if (!supportsInputEvents(displayId)) {
            throw new AssertionError("Could not inject input event if !supportsInputEvents()");
        }

        if (displayId != 0 && !InputManager.setDisplayId(inputEvent, displayId)) {
            Ln.d("failed to inject event");
            return false;
        }

        return SERVICE_MANAGER.getInputManager().injectInputEvent(inputEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectEvent(InputEvent event) {
        return injectEvent(event, displayId);
    }

    public static boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState, int displayId) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event, displayId);
    }

    public boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        return injectKeyEvent(action, keyCode, repeat, metaState, displayId);
    }

    public static boolean pressReleaseKeycode(int keyCode, int displayId) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0, displayId) && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0, displayId);
    }

    public boolean pressReleaseKeycode(int keyCode) {
        return pressReleaseKeycode(keyCode, displayId);
    }

    public static boolean isScreenOn() {
        return SERVICE_MANAGER.getPowerManager().isScreenOn();
    }

    public synchronized void setRotationListener(RotationListener rotationListener) {
        this.rotationListener = rotationListener;
    }

    public synchronized void setClipboardListener(ClipboardListener clipboardListener) {
        this.clipboardListener = clipboardListener;
    }

    public static void expandNotificationPanel() {
        SERVICE_MANAGER.getStatusBarManager().expandNotificationsPanel();
    }

    public static void expandSettingsPanel() {
        SERVICE_MANAGER.getStatusBarManager().expandSettingsPanel();
    }

    public static void collapsePanels() {
        SERVICE_MANAGER.getStatusBarManager().collapsePanels();
    }

    public static String getClipboardText() {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return null;
        }
        CharSequence s = clipboardManager.getText();
        if (s == null) {
            return null;
        }
        return s.toString();
    }

    public boolean setClipboardText(String text) {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return false;
        }

        String currentClipboard = getClipboardText();
        if (currentClipboard != null && currentClipboard.equals(text)) {
            // The clipboard already contains the requested text.
            // Since pasting text from the computer involves setting the device clipboard, it could be set twice on a copy-paste. This would cause
            // the clipboard listeners to be notified twice, and that would flood the Android keyboard clipboard history. To workaround this
            // problem, do not explicitly set the clipboard text if it already contains the expected content.
            return false;
        }

        isSettingClipboard.set(true);
        boolean ok = clipboardManager.setText(text);
        isSettingClipboard.set(false);
        return ok;
    }

    /**
     * @param mode one of the {@code POWER_MODE_*} constants
     */
    public static boolean setScreenPowerMode(int mode) {
        IBinder d = SurfaceControl.getBuiltInDisplay();
        if (d == null) {
            Ln.e("Could not get built-in display");
            return false;
        }
        return SurfaceControl.setDisplayPowerMode(d, mode);
    }

    public static boolean powerOffScreen(int displayId) {
        if (!isScreenOn()) {
            return true;
        }
        return pressReleaseKeycode(KeyEvent.KEYCODE_POWER, displayId);
    }

    /**
     * Disable auto-rotation (if enabled), set the screen rotation and re-enable auto-rotation (if it was enabled).
     */
    public void rotateDevice() {
        if (!rotationWatcherRegistered) return;
        IWindowManager wm = SERVICE_MANAGER.getWindowManager();

        boolean accelerometerRotation = !wm.isDisplayRotationFrozen(displayId);

        int newRotation = (currentRotation & 1) ^ 1; // 0->1, 1->0, 2->1, 3->0
        String newRotationString = newRotation == 0 ? "portrait" : "landscape";

        Ln.i("Device rotation requested: " + newRotationString);
        wm.freezeDisplayRotation(displayId, newRotation);

        // restore auto-rotate if necessary
        if (accelerometerRotation) {
            wm.thawDisplayRotation(displayId);
        }
    }

    public static ContentProvider createSettingsProvider() {
        return SERVICE_MANAGER.getActivityManager().createSettingsProvider();
    }

    public void cleanUp() {
        if (rotationWatcherRegistered) {
            SERVICE_MANAGER.getWindowManager().removeRotationWatcher(this);
        }
        if (isVirtual && virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    public VirtualDisplay getVirtualDisplay() {
        return virtualDisplay;
    }
}
