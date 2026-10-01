package android.net.wifi;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import androidx.annotation.RequiresApi;

public interface IWifiManager extends IInterface {
    /**
     * Legacy Soft AP callback registration ABI used by AOSP API 29 and API 30.
     */
    void registerSoftApCallback(IBinder binder, ISoftApCallback callback, int callbackIdentifier)
            throws RemoteException;

    @RequiresApi(31)
    void registerSoftApCallback(ISoftApCallback callback) throws RemoteException;

    /**
     * Legacy Soft AP callback unregistration ABI used by AOSP API 29 and API 30.
     */
    void unregisterSoftApCallback(int callbackIdentifier) throws RemoteException;

    @RequiresApi(31)
    void unregisterSoftApCallback(ISoftApCallback callback) throws RemoteException;
}
