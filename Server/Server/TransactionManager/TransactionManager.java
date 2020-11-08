package Server.TransactionManager;


import Server.Common.*;
import java.util.*;

public class TransactionManager {

    //we want that for each active transaction, it somehow stores the values of the 
    //data it writes over before it writes over it so that when aborted or fails to commit we revert.
    protected HashMap<Integer, RMHashMap> previousData;

    protected HashMap<Integer, TransactionStatus> activeTransactions;

    protected HashMap<Integer, TransactionStatus> inactiveTransactions;

    public Integer next_xid = 0;

    public enum TransactionStatus {
		COMMITTED,
        ABORTED,
        INVALID,
        ACTIVE
	};
    

    public TransactionManager() {
        previousData = new HashMap<Integer, RMHashMap>();
        activeTransactions = new HashMap<Integer, TransactionStatus>();
        inactiveTransactions = new HashMap<Integer, TransactionStatus>();
    }

    public int startTransaction() {
        int xid = getNextTransactionId();
        //add the transaction to list of active transactions
        addActiveTransaction(xid);

        //print info
        Trace.info("TM::startTransaction() created new transaction " + xid);

        return xid;
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

    public void commitTransaction(int xid) {
		//call on commit method to
        // 1- delete previous data stored in case of abort
        removePreviousValues(xid);
        // 2- remove it from active transactions
        removeActiveTransaction(xid);
        // 3- put the xid in the inactive but committed
        addInactiveTransaction(xid, TransactionStatus.COMMITTED);

        //print info
        Trace.info("TM::commitTransaction(" + xid + ") committed");
    }

    public void abortTransaction(int xid) {
        // 1- delete previous data stored in case of abort
        removePreviousValues(xid);
        // 2- remove it from active transactions
        removeActiveTransaction(xid);
        // 3- put the xid in the inactive but aborted
        addInactiveTransaction(xid, TransactionStatus.ABORTED);

        //print info
        Trace.info("TM::abortTransaction(" + xid + ") aborted");
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

    // Read the hashmap of the previous values the transaction saw 
    // before changing them (i.e. what they were at the start of writing).
    // This method takes in the transaction id and gives the hashmap of strings and values.
	public RMHashMap readPreviousValues(int xid)
	{
		synchronized(previousData) {
			RMHashMap x_previousData = previousData.get(xid);
			if (x_previousData != null) {
				return (RMHashMap)x_previousData.clone();
			}
			return null;
		}
	}

    // Transaction wants to write a value to a data item, it uses this method
    // first to store the previous value
	public void writePreviousValue(int xid, String key, RMItem value)
	{
		synchronized(previousData) {
            //get the RMHashMap for the specific transaction and add the value
            RMHashMap x_map = previousData.get(xid);
            if(x_map == null) {
                x_map = new RMHashMap();
                x_map.put(key, value);
                previousData.put(xid, x_map);
            } else {
                x_map.put(key, value);
                previousData.put(xid, x_map);
            }
		}
	}

    // Transaction is no longer active, remove the previous values stored 
    // as it has either been committed or is being aborted so we do not need to save the values anymore
	public void removePreviousValues(int xid)
	{
		synchronized(previousData) {
			previousData.remove(xid);
		}
	}
     
}
