package Server.TransactionManager;

import Server.Common.Trace;
import Server.Interface.IResourceManager;
import Server.TransactionManager.Message.MessageType;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class TimeoutManager implements ITimeoutManager {

    private static final int port = 1095;
    private static String s_timeoutName = "TimeoutManager";
    private static String s_middlewareName = "Server";
    private static String s_middlewareHost = "localhost";
    private static String s_rmiPrefix = "group_49_";
    
    
    private static IResourceManager middleware;
    private static  ConcurrentHashMap<Integer, TimeToLiveMechanism> transactionTimeouts;
    // BlockingQueue<Message> manager_queue = new LinkedBlockingQueue<Message>();

    public TimeoutManager() {
        transactionTimeouts = new ConcurrentHashMap<Integer, TimeToLiveMechanism>();
    }   

    public void startTimeout(int xid) throws RemoteException {

        Thread thisThread = Thread.currentThread();
        
        synchronized(transactionTimeouts) {
            synchronized(thisThread) {
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
                }
            }
        }
    }

    public void resetTimeout(int xid) throws RemoteException {
        Thread thisThread = Thread.currentThread();
        
        synchronized(transactionTimeouts) {
            synchronized(thisThread) {
                TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
                //if there is no timeout with this transaction do nothing
                if(timeout == null) {
                    Trace.info("TTLM::resetTimeout(" + xid + ") does not have a timeout to reset");
                    return;
                }
                //otherwise enqueue a message to reset the timer 
                else {
                    timeout.in_queue.add(new Message(MessageType.RESET_TIMER));
                }
            }
        }
    }

    public void closeTimeout(int xid) throws RemoteException {
        Thread thisThread = Thread.currentThread();

        synchronized(transactionTimeouts) {
            synchronized(thisThread) {
                TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
                //if there is no timeout with this transaction do nothing
                if(timeout == null) {
                    Trace.info("TTLM::closeTimeout(" + xid + ") does not have a timeout to close");
                    return;
                }
                //otherwise enqueue a message to close the timer
                else {
                    timeout.in_queue.add(new Message(MessageType.CLOSE_TIMER));
                    //remove the store of the timeout
                    transactionTimeouts.remove(xid);
                }
            }
        }
    }



/*     public static void main(String args[])
	{

        // Retrieve the middleware host 
		if (args.length >= 1)
		{
			s_middlewareHost = args[0];	
		}

        ITimeoutManager timeoutmanager = null;
		// Create the RMI server entry 
		try {
			// Create a new Server object
			TimeoutManager timeoutmanagerServer = new TimeoutManager();

			// Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
			timeoutmanagerServer.connectToMiddleware(s_middlewareHost, port, s_middlewareName);
			

			// Dynamically generate the stub (client proxy)
            timeoutmanager = (ITimeoutManager)UnicastRemoteObject.exportObject(timeoutmanagerServer, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(port);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(port);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_timeoutName, timeoutmanager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_timeoutName);
						System.out.println("'" + s_timeoutName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_timeoutName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_timeoutName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
        }
        if(timeoutmanager != null) {
            try {  
                timeoutmanager.run();
                System.out.println("Timeout manager is running");
            } catch(RemoteException e) {
                return;
            }
            
        }
        
    } */

    // Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
	public void connectToMiddleware(String server, int port, String name) {
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
                    IResourceManager rm_temp = (IResourceManager)registry.lookup(s_rmiPrefix + name);
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

    
    public void run() throws RemoteException{
        Thread thisThread = Thread.currentThread();

        

        while(true) {

            try{
                Thread.sleep(3000);
            } 
            catch(InterruptedException e) {
                System.out.println(e);
            }  
            synchronized(transactionTimeouts) {
                // synchronized(thisThread) {
                    // Set<Integer> set = transactionTimeouts.keySet();
                    // if(set.isEmpty()) System.out.println("timeoutmanager still has empty timeouts hashmap");
                    //sleep and every time wake up, check if there are any transactions that timed out
                    System.out.println("number of timeouts - " + transactionTimeouts.size());
                    
                    // Iterator<Integer> it =set.iterator();
                    Iterator<Integer> it = transactionTimeouts.keySet().iterator();

                    
                    Set<Integer> toRemove = new HashSet<Integer>();
                    
                    while(it.hasNext()){
                        // System.out.println("number of set - " + set.size());
                        Integer xid = it.next(); 
                        System.out.println("xid - " + xid);
                        
                        TimeToLiveMechanism timeout = transactionTimeouts.get(xid);
                        if(timeout!= null && !timeout.out_queue.isEmpty() && (timeout.out_queue.poll().getMessage() == MessageType.ABORT_TRANSACTION)){
                            //send to the middleware to abort the transaction
                            try{
                                middleware.abort(xid, true);
                                toRemove.add(xid);
                                // it.remove();
                                // transactionTimeouts.remove(xid);
                            } 
                            catch(RemoteException er) { System.out.println("exception caught");}
                            catch(InvalidTransactionException ei) { System.out.println("exception caught"); }
                            
                        }
                    }
                    System.out.println("size to remove " + toRemove.size());
                    for(Integer i : toRemove) {
                        transactionTimeouts.remove(i);
                    }
                // }
            }
        }

    }
    
}
