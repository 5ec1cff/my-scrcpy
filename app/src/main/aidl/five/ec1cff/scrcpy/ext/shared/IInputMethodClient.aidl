// IInputMethodClient.aidl
package five.ec1cff.scrcpy.ext.shared;

// Declare any non-default types here with import statements

interface IInputMethodClient {
    oneway void onInputStarted();
    oneway void onInputFinished();
    oneway void onCursorChanged(float x, float y);
    oneway void onInputMethodStatusChanged(boolean avaliable);
}