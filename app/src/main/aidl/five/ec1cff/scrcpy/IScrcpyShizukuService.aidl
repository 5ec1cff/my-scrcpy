package five.ec1cff.scrcpy;

import five.ec1cff.scrcpy.IScrcpyTaskController;

interface IScrcpyShizukuService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    oneway void exit() = 1; // Exit method defined by user

    int startScrcpy(int port, IBinder inputMethod) = 2;

    IBinder getBinderWrapper(IBinder target) = 3;

    IScrcpyTaskController getTaskController() = 4;
}