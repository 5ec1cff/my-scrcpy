// IInputMethod.aidl
package five.ec1cff.scrcpy.ext.shared;

interface IInputMethod {
    void commitText(CharSequence c);
    void commitComposingText(CharSequence c);
}