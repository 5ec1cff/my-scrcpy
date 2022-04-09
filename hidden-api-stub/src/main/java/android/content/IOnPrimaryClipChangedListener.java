package android.content;

public interface IOnPrimaryClipChangedListener {
    void dispatchPrimaryClipChanged();

    abstract class Stub implements IOnPrimaryClipChangedListener {}
}
