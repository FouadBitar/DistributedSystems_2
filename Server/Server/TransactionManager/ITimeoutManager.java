package Server.TransactionManager;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITimeoutManager extends Remote {

    public void run() throws RemoteException;

    public void startTimeout(int xid) throws RemoteException;

    public void resetTimeout(int xid) throws RemoteException;

    public void closeTimeout(int xid) throws RemoteException;
    
}
