// IInputMethod.aidl
package five.ec1cff.scrcpy.ext.shared;

interface IInputMethod {
    oneway void setClient(IBinder client);
    boolean isInputMethodAvaliable();
    oneway void commitText(CharSequence c);
    oneway void commitComposingText(CharSequence c);
}