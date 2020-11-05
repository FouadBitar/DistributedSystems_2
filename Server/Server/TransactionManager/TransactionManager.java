package Server.TransactionManager;


import Server.Common.*;
import java.util.*;

public class TransactionManager {

    //we want that for each active transaction, it somehow stores the values of the 
    //data it writes over before it writes over it so that when aborted or fails to commit we revert.
    protected HashMap<Integer, RMHashMap> previousData;

    protected HashMap<Integer, Boolean> activeTransactions;
    

    public TransactionManager() {
        previousData = new HashMap<Integer, RMHashMap>();
        activeTransactions = new HashMap<Integer, Boolean>();
    }

    public Boolean isActiveTransaction(int xid)
	{
		synchronized(activeTransactions) {
			Boolean isActive = activeTransactions.get(xid);
			if (isActive != null)
				return false;
			else
                return true;
		}
    }
    
    public void addActiveTransaction(int xid) 
    {
        synchronized(activeTransactions) {
            activeTransactions.put(xid, true);
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
			RMHashMap x_previousData = (RMHashMap)previousData.get(xid);
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
