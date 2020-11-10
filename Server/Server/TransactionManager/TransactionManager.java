package Server.TransactionManager;


import Server.Common.*;
import Server.Interface.*;
import Server.RMI.RMIMiddleware;

import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager {


    protected HashMap<Integer, TransactionStatus> activeTransactions;
    
    protected HashMap<Integer, List<ResourceManagerInvolved>> activeTransactionsRMs;

    protected HashMap<Integer, TransactionStatus> inactiveTransactions;

    protected static RMIMiddleware middleware;

    protected static HashMap<String, IResourceManager> resource_managers;

    public Integer next_xid = 0;

    public enum TransactionStatus {
		COMMITTED,
        ABORTED,
        INVALID,
        ACTIVE
    };
    
    public enum ResourceManagerInvolved {
		FLIGHTS,
        CARS,
        ROOMS
	};
    

    public TransactionManager(RMIMiddleware rmi_middleware) {
        activeTransactions = new HashMap<Integer, TransactionStatus>();
        inactiveTransactions = new HashMap<Integer, TransactionStatus>();
        activeTransactionsRMs = new HashMap<Integer, List<ResourceManagerInvolved>>();
        middleware = rmi_middleware; 
    }

    public void storeResourceManagers(HashMap<String, IResourceManager> rms) {
        resource_managers = rms;
    }



    public void addTransactionRM(int xid, List<ResourceManagerInvolved> rms) {
        List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
        for(ResourceManagerInvolved rm : rms) {
            list.add(rm);
        }
        synchronized(activeTransactionsRMs) { 
            activeTransactionsRMs.put(xid, list);
        }
    }

    public void removeTransactionRM(int xid) {
        synchronized(activeTransactionsRMs) {
            activeTransactionsRMs.remove(xid);
        }
    }

    public int getNextTransactionId()
	{
		synchronized(next_xid) {
            Integer next = next_xid;
            next_xid = next_xid + 1;
            return next;
		}
    }

    public Boolean isActiveTransaction(int xid)
	{
		synchronized(activeTransactions) {
			TransactionStatus isActive = activeTransactions.get(xid);
			if (isActive != null)
				return true;
			else
                return false;
		}
    }

    public TransactionStatus isValidTransaction(int xid) {
        synchronized(activeTransactions) {
            Boolean isActive = isActiveTransaction(xid);
            if(isActive) return TransactionStatus.ACTIVE;
            else {
                synchronized(inactiveTransactions) {
                    TransactionStatus isInactive = inactiveTransactions.get(xid);
                    if(isInactive == TransactionStatus.INVALID) return TransactionStatus.INVALID;
                    else return isInactive;
                }
            }
        }
    }


    public void addInactiveTransaction(int xid, TransactionStatus status) {
        synchronized(inactiveTransactions) {
            inactiveTransactions.put(xid, status);
        }
    }
    
    public void addActiveTransaction(int xid) 
    {
        synchronized(activeTransactions) {
            activeTransactions.put(xid, TransactionStatus.ACTIVE);
        }
    }

    public void removeActiveTransaction(int xid) 
    {
        synchronized(activeTransactions) {
            activeTransactions.remove(xid);
        }
    }

    public int startTransaction() throws RemoteException {
        //get the next unique id
        int xid = getNextTransactionId();

        //add the transaction to list of active transactions
        addActiveTransaction(xid);

        //print info
        Trace.info("TM::startTransaction() created new transaction " + xid);

        //start the timeout mechanism 
		middleware.startTimeout(xid);

        return xid;
    }

    public void commitTransaction(int xid) throws RemoteException {
        
        // 1- stop the timeout mechanism
        middleware.stopTimeout(xid);

        // 2- delete previous data stored in case of abort from all involved resource managers, then delete the list of resource managers involved
        synchronized(activeTransactionsRMs) {
            List<ResourceManagerInvolved> x_rms = activeTransactionsRMs.get(xid);
            //check if the associated resource managers are there and more than 0 
            if(x_rms != null && !x_rms.isEmpty()) {
                List<IResourceManager> rms = new LinkedList<IResourceManager>();
                for(ResourceManagerInvolved rminvolved : x_rms) {
                    if(rminvolved == ResourceManagerInvolved.FLIGHTS) rms.add(resource_managers.get(RMIMiddleware.FLIGHT_SERVER_NAME));
                    if(rminvolved == ResourceManagerInvolved.CARS) rms.add(resource_managers.get(RMIMiddleware.CAR_SERVER_NAME));
                    if(rminvolved == ResourceManagerInvolved.ROOMS) rms.add(resource_managers.get(RMIMiddleware.ROOM_SERVER_NAME));
                }
                for(IResourceManager rm : rms) {
                    rm.removePreviousValues(xid);
                    rm.transactionCommitted(xid);
                }
                activeTransactionsRMs.remove(xid);
            }
        }
        
        // 3- remove it from active transactions
        removeActiveTransaction(xid);

        // 4- put the xid in the inactive but committed
        addInactiveTransaction(xid, TransactionStatus.COMMITTED);

        //print info
        Trace.info("TM::commitTransaction(" + xid + ") committed");
    }

    public void abortTransaction(int xid, boolean timedOut) throws RemoteException {

        // 1- stop the timeout mechanism
        //if it was a timeout message sent by the timeoutmanager, then do nothing
        if(!timedOut) {
            middleware.stopTimeout(xid);
        }
        

        // 2- revert previous data stored from all involved resource managers, then delete the list of resource managers involved
        synchronized(activeTransactionsRMs) {
            List<ResourceManagerInvolved> x_rms = activeTransactionsRMs.get(xid);
            if(x_rms != null && !x_rms.isEmpty()) { 
                List<IResourceManager> rms = new LinkedList<IResourceManager>();
                for(ResourceManagerInvolved rminvolved : x_rms) {
                    if(rminvolved == ResourceManagerInvolved.FLIGHTS) rms.add(resource_managers.get(RMIMiddleware.FLIGHT_SERVER_NAME));
                    if(rminvolved == ResourceManagerInvolved.CARS) rms.add(resource_managers.get(RMIMiddleware.CAR_SERVER_NAME));
                    if(rminvolved == ResourceManagerInvolved.ROOMS) rms.add(resource_managers.get(RMIMiddleware.ROOM_SERVER_NAME));
                }
                //revert the values for each resource manager then remove them
                for(IResourceManager rm : rms) {
                    try{
                        rm.revertPreviousValues(xid);
                        rm.removePreviousValues(xid);
                    } catch(RemoteException e) {

                    }
                }
                //remove the resource managers involved list
                activeTransactionsRMs.remove(xid);
            }
        }

        // 3- remove it from active transactions
        removeActiveTransaction(xid);

        // 4- put the xid in the inactive but aborted
        addInactiveTransaction(xid, TransactionStatus.ABORTED);

        //print info
        if(timedOut) {
            Trace.info("TM::abortTransaction(" + xid + ") timeout occured, aborted");
        } else {
            Trace.info("TM::abortTransaction(" + xid + ") aborted");
        }
        
    }

     
}
