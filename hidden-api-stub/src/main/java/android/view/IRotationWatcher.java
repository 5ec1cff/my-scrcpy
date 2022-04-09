package android.view;

public interface IRotationWatcher {
    void onRotationChanged(int rotation);

    abstract class Stub implements IRotationWatcher {

    }
}
