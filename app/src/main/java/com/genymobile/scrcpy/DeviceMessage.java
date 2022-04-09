package com.genymobile.scrcpy;

public final class DeviceMessage {

    public static final int TYPE_CLIPBOARD = 0;
    public static final int TYPE_IME_INPUT_STARTED = 1;
    public static final int TYPE_IME_INPUT_FINISHED = 2;
    public static final int TYPE_IME_CURSOR_CHANGED = 3;

    private int type;
    private String text;
    private float x;
    private float y;

    private DeviceMessage() {
    }

    public static DeviceMessage createClipboard(String text) {
        DeviceMessage event = new DeviceMessage();
        event.type = TYPE_CLIPBOARD;
        event.text = text;
        return event;
    }

    public static DeviceMessage createEmpty(int type) {
        DeviceMessage event = new DeviceMessage();
        event.type = type;
        return event;
    }

    public static DeviceMessage createCursorChanged(float x, float y) {
        DeviceMessage event = new DeviceMessage();
        event.type = TYPE_IME_CURSOR_CHANGED;
        event.x = x;
        event.y = y;
        return event;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public float[] getFloats() {
        return new float[] {x, y};
    }
}
