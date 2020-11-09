package Server.RMI;

import Server.Interface.*;
import Server.Common.*;
import Server.TransactionManager.*;
import Server.TransactionManager.TransactionManager.ResourceManagerInvolved;
import Server.LockManager.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;




/**
 * - Dealing with customers through replication of customer data. I.e. when a customer is created,
 * 		they are created with the same cid on all three resource manager servers. Each resource manager will keep
 * 		track fo the resources that the customer interacts with. When we require information on all three, we 
 * 		would manipulate it either her in the middleware or another resource manager if complexity increases.
 */
public class RMIMiddleware implements IResourceManager {

    private static final int s_serverPort = 1095;
    private static String s_serverName = "Server";
	private static String s_rmiPrefix = "group_49_";
	private static String s_timeoutName = "TimeoutManager";
	// private static String s_serverHost = "localhost";
	
	// Resource Managers
	// private static Vector<String> serverNames;
	private static Vector<String> serverHosts;
	private static Vector<String> serverNames;
	private static HashMap<String, IResourceManager> resource_managers;
	private static ITimeoutManager timeoutManager;


	protected LockManager lm = null;
	public static TransactionManager tm = null;
	
	public static String FLIGHT_SERVER_NAME = "Flights";
	public static String CAR_SERVER_NAME = "Cars";
	public static String ROOM_SERVER_NAME = "Rooms";
	public static String TIMEOUT_SERVER_NAME = "TimeoutManager";
	public static String RM_FLIGHT_DATA = "m_data_flights";
	public static String RM_CAR_DATA = "m_data_cars";
	public static String RM_ROOM_DATA = "m_data_rooms";


	//constructor
	public RMIMiddleware() {
		lm = new LockManager();
		tm = new TransactionManager(this);
	}
    
    public static void main(String args[])
	{
		// Initialize variables
		serverHosts = new Vector<String>();
		serverNames = new Vector<String>();
		serverNames.add(FLIGHT_SERVER_NAME);
		serverNames.add(CAR_SERVER_NAME);
		serverNames.add(ROOM_SERVER_NAME);
		resource_managers = new HashMap<String, IResourceManager>();

		// Retrieve the resource manager server names from command line arguments
		if (args.length == 3)
		{
			serverHosts.add(args[0]);
			serverHosts.add(args[1]);
			serverHosts.add(args[2]);
			
		}
		else
		{
			System.err.println((char)27 + "need 4 arguments exactly");
			System.exit(1);
		}




		ITimeoutManager timeoutmanager_rmi = null;
		try {

			// FIRST BIND THE MIDDELWARE SERVER
			// --------------------------------
			// --------------------------------

			// Create a new Server object
			RMIMiddleware server = new RMIMiddleware();

			// Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
			server.connectToResourceManagers(serverHosts, serverNames);

			//provide the resource managers to the transaction manager once connecte to them
			tm.storeResourceManagers(resource_managers);
			

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(s_serverPort);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(s_serverPort);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' middleware unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");

			//NOW BIND THE TIMEOUT MANAGER SERVER
			// --------------------------------
			// --------------------------------

			// Create the timeoutmanager server object
			TimeoutManager timeoutmanagerServer = new TimeoutManager();

			// Get the reference to the middleware server
			timeoutmanagerServer.connectToMiddleware("localhost", s_serverPort, s_serverName, s_rmiPrefix);
			
			// Dynamically generate the stub (client proxy)
            timeoutmanager_rmi = (ITimeoutManager)UnicastRemoteObject.exportObject(timeoutmanagerServer, 0);

			// Bind the remote object's stub in the registry
			registry.rebind(s_rmiPrefix + s_timeoutName, timeoutmanager_rmi);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_timeoutName);
						System.out.println("'" + s_timeoutName + "' unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_timeoutName + "' server ready and bound to '" + s_rmiPrefix + s_timeoutName + "'");

			//get references to the RMI timeout manager
			server.connectToResourceManager("localhost", 1095, TIMEOUT_SERVER_NAME, false);
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


        if(timeoutmanager_rmi != null) {
            try {  
                timeoutmanager_rmi.run();
                System.out.println("Timeout manager is running");
            } catch(RemoteException e) {
                return;
            }
            
        }
	}
	
	// Loop through list of resource manager server names, retrieve their references and add to hash map
	public void connectToResourceManagers(Vector<String> serverHosts, Vector<String> serverNames)
	{
		int i = 0;
		for(String host : serverHosts) {
			connectToResourceManager(host, s_serverPort, serverNames.elementAt(i), true);
			i++;
		}
	}

	// Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
	public void connectToResourceManager(String server, int port, String name, boolean isRM) {
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);

					if(isRM) {
						IResourceManager rm_temp = (IResourceManager)registry.lookup(s_rmiPrefix + name);
						resource_managers.put(name, rm_temp);
					} else {
						ITimeoutManager timeoutmanager_temp = (ITimeoutManager)registry.lookup(s_rmiPrefix + name);
						timeoutManager = timeoutmanager_temp;
					}
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

	


	public boolean isTransactionActive(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		TransactionManager.TransactionStatus status = tm.isValidTransaction(xid);
		if(status == TransactionManager.TransactionStatus.ACTIVE) return true;
		else if(status == TransactionManager.TransactionStatus.COMMITTED)
			throw new InvalidTransactionException(xid, "This transaction has already been committed");
		else if(status == TransactionManager.TransactionStatus.INVALID) 
			throw new InvalidTransactionException(xid, "This transaction is invalid");
		else 
			throw new TransactionAbortedException(xid, "This transaction has been aborted");
	}

	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.FLIGHTS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock for the flight data
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);

			//call this method on it
			boolean result = rm_f.addFlight(xid, flightNum, flightSeats, flightPrice);

			return result;
		}
		
		catch(DeadlockException de) {
			Trace.info("RM::addFlight(" + xid + ") could not add flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	public boolean addCars(int xid, String location, int numCars, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + numCars + ", $" + price + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock 
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);

			//call this method on it
			boolean result = rm_c.addCars(xid, location, numCars, price);

			return result;
		}
		
		catch(DeadlockException de) {
			Trace.info("RM::addCars(" + xid + ") could not add cars, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

    public boolean addRooms(int xid, String location, int numRooms, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + numRooms + ", $" + price + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock 
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			//call this method on it
			boolean result = rm_r.addRooms(xid, location, numRooms, price);

			return result;	
		}
		
		catch(DeadlockException de) {
			Trace.info("RM::addRooms(" + xid + ") could not add rooms, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	} 			    

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteFlight(" + xid + ", flightNum=" + flightNum + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.FLIGHTS);
		tm.addTransactionRM(xid, list);

		try{
			//grab  read lock
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);

			//call this method on it
			boolean result = rm_f.deleteFlight(xid, flightNum);

			return result;

		}
		catch(DeadlockException de) {
			Trace.info("RM::deleteFlight(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	public boolean deleteCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteCars(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock 
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);

			//call this method on it
			boolean result = rm_c.deleteCars(xid, location);

			return result;

		}
		catch(DeadlockException de) {
			Trace.info("RM::deleteCars(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public boolean deleteRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteRooms(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock 
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			//get the right resource manager 
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			//call this method on it
			boolean result = rm_r.deleteRooms(xid, location);

			return result;	

		}
		catch(DeadlockException de) {
			Trace.info("RM::deleteRooms(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	public int queryFlight(int xid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryFlight(" + xid + ", flightNum=" + flightNumber + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			//grab  read lock
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);

			//call this method on it
			int result = rm_f.queryFlight(xid, flightNumber);

			return result;

		}
		catch(DeadlockException de) {
			Trace.info("RM::queryFlight(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public int queryCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryFlight(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			//grab  read lock
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);

			//call this method on it
			int result = rm_c.queryCars(xid, location);

			return result;

		}
		catch(DeadlockException de) {
			Trace.info("RM::queryCars(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

    public int queryRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryFlight(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			//grab  read lock
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			//call this method on it
			int result = rm_r.queryRooms(xid, location);

			return result;

		}
		catch(DeadlockException de) {
			Trace.info("RM::queryCars(" + xid + ") could not be done, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	public int queryFlightPrice(int xid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		
		Trace.info("RM::queryFlightPrice(" + xid + ", flightNum=" + flightNumber + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			// grab read lock 
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);

			//call this method on it
			int result = rm_f.queryFlightPrice(xid, flightNumber);

			return result;

		}
		//deadlock occured, abort the transaction
		catch(DeadlockException de) {
			Trace.info("RM::queryFlightPrice(" + xid + ") could not query the price, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public int queryCarsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryCarsPrice(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			// grab read lock 
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);

			//call this method on it
			int result = rm_c.queryCarsPrice(xid, location);

			return result;

		}
		//deadlock occured, abort the transaction
		catch(DeadlockException de) {
			Trace.info("RM::queryCarsPrice(" + xid + ") could not query the price, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public int queryRoomsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryRoomsPrice(" + xid + ", location=" + location + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		try{
			// grab read lock 
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_READ);

			//get the right resource manager 
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			//call this method on it
			int result = rm_r.queryRoomsPrice(xid, location);

			return result;

		}
		//deadlock occured, abort the transaction
		catch(DeadlockException de) {
			Trace.info("RM::queryRoomsPrice(" + xid + ") could not query the price, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	 
	// CUSTOMERS
	// --------------
	// --------------
	// --------------

	//need to store the resource managers involved in the transaction so that if aborted we can send a message to each to abort and revert
    public int newCustomer(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {

		Trace.info("RM::newCustomer(" + xid + ") called");

		//check that the transaction is active
		isTransactionActive(xid);


		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		list.add(ResourceManagerInvolved.FLIGHTS);
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);

		try{
			// grab write lock 
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			// have lock to all resource managers, now call on each to create the customer
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			int cid = rm_f.newCustomer(xid);
			rm_c.newCustomer(xid, cid);
			rm_r.newCustomer(xid, cid);

			return cid;

		}
		//deadlock occured, abort transaction
		catch(DeadlockException de) {
			Trace.info("RM::addFlight(" + xid + ") could not add flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}
	
	
    public boolean newCustomer(int xid, int cid) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::newCustomer(" + xid + ", " + cid + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		list.add(ResourceManagerInvolved.FLIGHTS);
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);


		try{
			// grab write lock 
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			// have lock to all resource managers, now call on each to create the customer
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			boolean result = rm_f.newCustomer(xid, cid);
			rm_c.newCustomer(xid, cid);
			rm_r.newCustomer(xid, cid);

			return result;
		}
		//deadlock occured, abort the transaction and throw exception
		catch(DeadlockException de) {
			Trace.info("RM::addFlight(" + xid + ") could not add flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}
    
    
    public boolean deleteCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		list.add(ResourceManagerInvolved.FLIGHTS);
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);


		try{
			// grab write lock 
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			// have lock to all resource managers, now call on each to create the customer
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			boolean result = rm_f.deleteCustomer(xid, customerID);
			rm_c.deleteCustomer(xid, customerID);
			rm_r.deleteCustomer(xid, customerID);

			return result;
		}
		//deadlock occured, abort the transaction and throw exception
		catch(DeadlockException de) {
			Trace.info("RM::addFlight(" + xid + ") could not add flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public String queryCustomerInfo(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");

		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		list.add(ResourceManagerInvolved.FLIGHTS);
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);

		String customerInfo = "";
		// grab write lock 
		try{
			// grab read locks
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_READ);
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_READ);
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_READ);

			// have lock to all resource managers, now call on each to create the customer
			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);
			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);
			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			String result1 = rm_f.queryCustomerInfo(xid, customerID);
			String result2 = rm_c.queryCustomerInfo(xid, customerID);
			String result3 = rm_r.queryCustomerInfo(xid, customerID);

			customerInfo = customerInfo + result1 + result2 + result3;

			return customerInfo;

		}
		//deadlock occured, abort the transaction
		catch(DeadlockException de) {
			Trace.info("RM::addFlight(" + xid + ") could not add flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}



		//return "";
		/* IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		// since the information of the flights, cars and rooms is on separate servers, we can display the info from each one
		String flight_bill = flight_rm.queryCustomerInfo(id, customerID);
		String car_bill = car_rm.queryCustomerInfo(id, customerID);
		String room_bill = room_rm.queryCustomerInfo(id, customerID);

		// structure the results
		String final_bill = "Flight Bill: \n" + flight_bill + "Car Bill: \n" + car_bill + "Room Bill: \n" + room_bill;

		return final_bill; */
	}


    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {

		Trace.info("RM::reserveFlight(" + xid + ", customer=" + customerID + ", flight-" + flightNumber + ", " + ") called" );
		
		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.FLIGHTS);
		tm.addTransactionRM(xid, list);

		try{
			//grab  read lock
			lm.Lock(xid, RM_FLIGHT_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			IResourceManager rm_f = resource_managers.get(FLIGHT_SERVER_NAME);

			boolean result = rm_f.reserveFlight(xid, customerID, flightNumber);

			return result;

		}

		catch(DeadlockException de) {
			Trace.info("RM::reserveFlight(" + xid + ") could not reserve flight, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}


    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::reserveCar(" + xid + ", customer=" + customerID + ", location-" + location + ", " + ") called" );
		
		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.CARS);
		tm.addTransactionRM(xid, list);

		try{
			//grab  read lock
			lm.Lock(xid, RM_CAR_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			IResourceManager rm_c = resource_managers.get(CAR_SERVER_NAME);

			boolean result = rm_c.reserveCar(xid, customerID, location);

			return result;

		}

		catch(DeadlockException de) {
			Trace.info("RM::reserveCar(" + xid + ") could not reserve car, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

	
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		Trace.info("RM::reserveRoom(" + xid + ", customer=" + customerID + ", location-" + location + ", " + ") called" );
		
		//check that the transaction is active
		isTransactionActive(xid);

		//add the resource managers this operation will interact with
		List<ResourceManagerInvolved> list = new LinkedList<ResourceManagerInvolved>();
		list.add(ResourceManagerInvolved.ROOMS);
		tm.addTransactionRM(xid, list);

		try{
			//grab  read lock
			lm.Lock(xid, RM_ROOM_DATA, TransactionLockObject.LockType.LOCK_WRITE);

			IResourceManager rm_r = resource_managers.get(ROOM_SERVER_NAME);

			boolean result = rm_r.reserveRoom(xid, customerID, location);

			return result;

		}

		catch(DeadlockException de) {
			Trace.info("RM::reserveRoom(" + xid + ") could not reserve room, deadlock exception");
			abort(xid, false);
			throw new TransactionAbortedException(xid, "Deadlock occured - transaction aborted");
		}
	}

 
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, TransactionAbortedException, InvalidTransactionException  {
		return false;
		/* IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		boolean result = true;

		// reserve set of flights
		for(String flightNumber : flightNumbers) {
			flight_rm.reserveFlight(id, customerID, toInt(flightNumber));
		}
		
		// reserve car and/or room at final location
		if(car) {
			car_rm.reserveCar(id, customerID, location);
		}
		if(room) {
			room_rm.reserveRoom(id, customerID, location);
		}

		
		return result; */
		
	}

    public String getName() throws RemoteException {
		return s_serverName;
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (Integer.valueOf(string)).intValue();
	}


	public int start() throws RemoteException {
		int xid = tm.startTransaction();

		return xid;
	}

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		//first check it is a valid active transaction
		TransactionManager.TransactionStatus status = tm.isValidTransaction(transactionId);
		if(status == TransactionManager.TransactionStatus.INVALID) {
			throw new InvalidTransactionException(transactionId, "Transaction is invalid");
		} else {
			if(status == TransactionManager.TransactionStatus.ACTIVE) {

				tm.commitTransaction(transactionId);

				//unlock all the locks 
				lm.UnlockAll(transactionId);


				return true;
			} else if (status == TransactionManager.TransactionStatus.ABORTED) {
				throw new TransactionAbortedException(transactionId, "Transaction has been aborted");
			} else {
				//its already been committed
				return true;
			}
		}
	}
    
    public void abort(int transactionId, boolean timedOut) throws RemoteException, InvalidTransactionException {
		//check the status of the transaction
		TransactionManager.TransactionStatus status = tm.isValidTransaction(transactionId);
		//boolean isValid = tm.isActiveTransaction(transactionId);
		if(status == TransactionManager.TransactionStatus.INVALID) {
			throw new InvalidTransactionException(transactionId, "Invalid Transaction");
		} else {
			if(status == TransactionManager.TransactionStatus.COMMITTED) {
				//transaction already committed
				throw new InvalidTransactionException(transactionId, "Transaction already committed");
			} else if (status == TransactionManager.TransactionStatus.ABORTED) {
				//transaction already aborted, do nothing
			} else {
				//transaction is active, abort it
				tm.abortTransaction(transactionId, timedOut);

				//then unlock the locks held
				lm.UnlockAll(transactionId);

			}
		}
	}

	public void startTimeout(int xid)  throws RemoteException {
		timeoutManager.startTimeout(xid);
	}

	public void stopTimeout(int xid) throws RemoteException {
		timeoutManager.closeTimeout(xid);
	}
    
    public boolean shutdown() 
    throws RemoteException {
		return false;
	}


	public void transactionCommitted(int xid) {
		return;
	}

	public void removePreviousValues(int xid) { return; }

    // public RMHashMap readPreviousValues(int xid) { return new RMHashMap(); }

    public void revertPreviousValues(int xid) { return; }
	
    
}
