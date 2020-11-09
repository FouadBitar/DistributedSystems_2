package Server.TransactionManager;

import Server.Common.Trace;
import Server.Interface.*;
import Server.TransactionManager.Message.MessageType;
import Server.Exception.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class TimeoutManager implements ITimeoutManager {
    

    private static IMiddleware middleware;
    private static  ConcurrentHashMap<Integer, TimeToLiveMechanism> transactionTimeouts;


    public TimeoutManager() {
        transactionTimeouts = new ConcurrentHashMap<Integer, TimeToLiveMechanism>();
    }   

    // this method is used by the timeout manager object to get a reference to the middleware so
    // it can communicate when a timeout occurs to abort a transaction
	public void connectToMiddleware(String server, int port, String name, String s_rmiPrefix) {
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
                    IMiddleware rm_temp = (IMiddleware)registry.lookup(s_rmiPrefix + name);
                    middleware = rm_temp;
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
    }


    public void startTimeout(int xid) throws RemoteException {  
        synchronized(transactionTimeouts) {
            TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
            //if there is already a timeout do nothing and print
            if(timeout != null) {
                Trace.info("TTLM::startTimeout(" + xid + ") already a timeout for " + xid);
                return;
            }
            //else start the thread and add it
            else {
                timeout = new TimeToLiveMechanism(xid);
                timeout.start();
                transactionTimeouts.put(xid, timeout);
                Trace.info("TTLM::startTimeout(" + xid + ") timeout started");
            }
        }
    }

    public void resetTimeout(int xid) throws RemoteException {
        synchronized(transactionTimeouts) {
            TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
            //if there is no timeout with this transaction do nothing
            if(timeout == null) {
                Trace.info("TTLM::resetTimeout(" + xid + ") does not have a timeout to reset");
                return;
            }
            //otherwise enqueue a message to reset the timer 
            else {
                timeout.in_queue.add(new Message(MessageType.RESET_TIMER));
                Trace.info("TTLM::resetTimeout(" + xid + ") timer reset");
            }
        }
    }

    public void closeTimeout(int xid) throws RemoteException {
        synchronized(transactionTimeouts) {
            TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
            //if there is no timeout with this transaction do nothing
            if(timeout == null) {
                Trace.info("TTLM::closeTimeout(" + xid + ") does not have a timeout to close");
                return;
            }
            //otherwise enqueue a message to close the timer
            else {
                timeout.in_queue.add(new Message(MessageType.CLOSE_TIMER));
                Trace.info("TTLM::closeTimeout(" + xid + ") timeout closed");
                //remove the store of the timeout
                transactionTimeouts.remove(xid);
            }
        }
    }

    

    public void run() throws RemoteException{

        while(true) {

            try{
                Thread.sleep(3000);
            } 
            catch(InterruptedException e) {
                System.out.println(e);
            }  

            synchronized(transactionTimeouts) {
                    
                Iterator<Integer> it = transactionTimeouts.keySet().iterator();
                Set<Integer> toRemove = new HashSet<Integer>();
                
                while(it.hasNext()){
                    Integer xid = it.next(); 
                    
                    TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
                    //if timeout is not null and there is a message that it timed out, then message middleware to abort
                    if(timeout!= null && !timeout.out_queue.isEmpty() && (timeout.out_queue.poll().getMessage() == MessageType.ABORT_TRANSACTION)){
                        //send to the middleware to abort the transaction
                        try{
                            Trace.info("TTLM::timeout(" + xid + ") timeout occured, aborting transaction " + xid);
                            middleware.abort(xid, true);
                            toRemove.add(xid);
                        } 
                        catch(RemoteException er) { System.out.println("exception caught");}
                        catch(InvalidTransactionException ei) { System.out.println("exception caught"); }
                    }
                }

                for(Integer i : toRemove) {
                    transactionTimeouts.remove(i);
                }
                
            }
        }

    }
    
}
