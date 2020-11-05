package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

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
	// private static String s_serverHost = "localhost";
	
	// Resource Managers
	// private static Vector<String> serverNames;
	private static Vector<String> serverHosts;
	private static Vector<String> serverNames;
	private static HashMap<String, IResourceManager> resource_managers_hash;
    
    public static void main(String args[])
	{
		// Initialize variables
		serverHosts = new Vector<String>();
		serverNames = new Vector<String>();
		serverNames.add("Flights");
		serverNames.add("Cars");
		serverNames.add("Rooms");
		resource_managers_hash = new HashMap<String, IResourceManager>();

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

		
		// Create the RMI server entry 
		try {
			// Create a new Server object
			RMIMiddleware server = new RMIMiddleware();

			// Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
			server.connectToResourceManagers(serverHosts, serverNames);
			

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
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
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
	}
	
	// Loop through list of resource manager server names, retrieve their references and add to hash map
	public void connectToResourceManagers(Vector<String> serverHosts, Vector<String> serverNames)
	{
		int i = 0;
		for(String host : serverHosts) {
			connectToResourceManager(host, s_serverPort, serverNames.elementAt(i));
			i++;
		}
	}

	// Get references to RMI Resource Manager servers (i.e. Flights, Cars, Rooms)
	public void connectToResourceManager(String server, int port, String name) {
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					IResourceManager rm_temp = (IResourceManager)registry.lookup(s_rmiPrefix + name);
					resource_managers_hash.put(name, rm_temp);
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


	// IMPLEMENTATION
	// --------------
	// --------------
	// --------------

	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		boolean result = flight_rm.addFlight(id, flightNum, flightSeats, flightPrice);

		return result;
	}

	public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {

		IResourceManager car_rm = resource_managers_hash.get("Cars");
		boolean result = car_rm.addCars(id, location, numCars, price);

		return result;
	}

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {

		IResourceManager room_rm = resource_managers_hash.get("Rooms");
		boolean result = room_rm.addRooms(id, location, numRooms, price);

		return result;
	} 			    

    public boolean deleteFlight(int id, int flightNum) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		boolean result = flight_rm.deleteFlight(id, flightNum);

		return result;
	}

	public boolean deleteCars(int id, String location) throws RemoteException {
		
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		boolean result = car_rm.deleteCars(id, location);

		return result;
	}


    public boolean deleteRooms(int id, String location) throws RemoteException {
		
		IResourceManager room_rm = resource_managers_hash.get("Rooms");
		boolean result = room_rm.deleteRooms(id, location);

		return result;
	}

	public int queryFlight(int id, int flightNumber) throws RemoteException {
		
		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		int result = flight_rm.queryFlight(id, flightNumber);

		return result;
	}


    public int queryCars(int id, String location) throws RemoteException {
		
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		int result = car_rm.queryCars(id, location);

		return result;
	}

    public int queryRooms(int id, String location) throws RemoteException {
		
		IResourceManager room_rm = resource_managers_hash.get("Rooms");
		int result = room_rm.queryRooms(id, location);

		return result;
	}

	public int queryFlightPrice(int id, int flightNumber) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		int result = flight_rm.queryFlightPrice(id, flightNumber);

		return result;
	}


    public int queryCarsPrice(int id, String location) throws RemoteException {
		
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		int result = car_rm.queryCarsPrice(id, location);

		return result;
	}


    public int queryRoomsPrice(int id, String location) throws RemoteException {
		
		IResourceManager room_rm = resource_managers_hash.get("Rooms");
		int result = room_rm.queryRoomsPrice(id, location);

		return result;
	}

	 
	// CUSTOMERS
	// --------------
	// --------------
	// --------------

    public int newCustomer(int id) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		// create the customer with its unique cid
		int cid = flight_rm.newCustomer(id);

		// then replicate the customer at the other resource managers
		car_rm.newCustomer(id, cid);
		room_rm.newCustomer(id, cid);

		return cid;
	}
	
	
    public boolean newCustomer(int id, int cid) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		// create the customer
		boolean result = flight_rm.newCustomer(id, cid);
		
		// replicate the customer at the other resource managers
		car_rm.newCustomer(id, cid);
		room_rm.newCustomer(id, cid);

		return result;
	}
    
    
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
		
		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		// delete the customer on all the resource managers
		boolean result = flight_rm.deleteCustomer(id, customerID);
		car_rm.deleteCustomer(id, customerID);
		room_rm.deleteCustomer(id, customerID);

		return result;
	}


    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
		
		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		IResourceManager room_rm = resource_managers_hash.get("Rooms");

		// since the information of the flights, cars and rooms is on separate servers, we can display the info from each one
		String flight_bill = flight_rm.queryCustomerInfo(id, customerID);
		String car_bill = car_rm.queryCustomerInfo(id, customerID);
		String room_bill = room_rm.queryCustomerInfo(id, customerID);

		// structure the results
		String final_bill = "Flight Bill: \n" + flight_bill + "Car Bill: \n" + car_bill + "Room Bill: \n" + room_bill;

		return final_bill;
	}


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {

		IResourceManager flight_rm = resource_managers_hash.get("Flights");
		boolean result = flight_rm.reserveFlight(id, customerID, flightNumber);

		return result;
	}


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
		
		IResourceManager car_rm = resource_managers_hash.get("Cars");
		boolean result = car_rm.reserveCar(id, customerID, location);

		return result;
	}

	
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
		
		IResourceManager room_rm = resource_managers_hash.get("Rooms");
		boolean result = room_rm.reserveRoom(id, customerID, location);

		return result;
	}

 
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
		
		IResourceManager flight_rm = resource_managers_hash.get("Flights");
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

		
		return result;
		
	}

    public String getName() throws RemoteException {
		return s_serverName;
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (Integer.valueOf(string)).intValue();
	}
	
    
}
