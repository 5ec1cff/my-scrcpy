// IInputMethodClient.aidl
package five.ec1cff.scrcpy.ext.shared;

// Declare any non-default types here with import statements

interface IInputMethodClient {
    void onBindInput(IBinder binder);
    void onInputStarted();
    void onInputFinished();
    void onCursorChanged(float x, float y);
}