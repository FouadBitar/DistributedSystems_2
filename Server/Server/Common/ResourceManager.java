// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.Exception.*;

import java.util.*;
import java.rmi.RemoteException;



public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected HashMap<Integer, RMHashMap> previousData;


	
	public ResourceManager(String p_name)
	{
		m_name = p_name;
		previousData = new HashMap<Integer, RMHashMap>(); 
	}

	// Reads a data item
	protected RMItem readData(int xid, String key)
	{
		synchronized(m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key)
	{
		synchronized(m_data) {
			m_data.remove(key);
		}
	}



	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");

		ReservableItem curObj = (ReservableItem)readData(xid, key);
		
		// Check if there is such an item in the storage
		if (curObj == null) 
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				//save data before writing
				writePreviousValue(xid, curObj.getKey(), curObj);
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");

		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
	
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null) {
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
		

		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{     
			//save data before writing
			writePreviousValue(xid, customer.getKey(), customer);
			writePreviousValue(xid, item.getKey(), item);

			//update customer info
			customer.reserve(key, location, item.getPrice());        
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}  
	}



	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{

		//store data this method is going to write to in case of abort
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		RMItem clone = readData(xid, Flight.getKey(flightNum));
		writePreviousValue(xid, Flight.getKey(flightNum), clone);

		// if object does not exist, write it
		if (curObj == null) {
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		//otherwise it exists, update it
		else {
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0) {
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;

	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		//store data this method is going to write to in case of abort
		Car curObj = (Car)readData(xid, Car.getKey(location));
		writePreviousValue(xid, Car.getKey(location), curObj);

		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		//store data this method is going to write to in case of abort
		Room curObj = (Room)readData(xid, Room.getKey(location));
		writePreviousValue(xid, Room.getKey(location), curObj);


		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Flight.getKey(flightNum));	
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");


		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public int newCustomer(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ") called");

		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		
		Customer customer = new Customer(cid);

		
		//store null with cid created to remove if transaction aborted
		writePreviousValue(xid, Customer.getKey(cid), null);
		
		//write the data and return the cid
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	//for now, we are assuming there are no failures when using the lock interface method 
	//i.e. we do not assume it returns false but that it only throws an exception.
	public boolean newCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");

			
		//read customer data	
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		
		
		//if customer exists, return false
		if (customer != null) {
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
		//otherwise create the new customer with this id 
		else {
			customer = new Customer(customerID);
			//store customer data this method is going to create in case of abort
			writePreviousValue(xid, Customer.getKey(customerID), null);
			
			//write the data and return
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}

	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");


		//read customer data
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));


		//customer doesnt exist
		if (customer == null) {
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		}
		//customer exists, delete the customer
		else {    
			// write new customer

			//store customer that is about to removed
			writePreviousValue(xid, Customer.getKey(customerID), customer);

			// Increase the reserved numbers of all reservable items which the customer reserved. 
			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet())
			{        
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
				ReservableItem item  = (ReservableItem)readData(xid, reserveditem.getKey());

				//save value of reserved item before reserved count is changed
				writePreviousValue(xid, item.getKey(), item);

				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;

		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
		return false;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}




/* 	public int start() throws RemoteException {
		return 0;
	}

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return false;
	}
    
    public void abort(int transactionId, boolean timedOut) throws RemoteException, InvalidTransactionException {
		return;
	}
    
    public boolean shutdown() throws RemoteException {
		return false;
	} */

	public void shutdown() throws RemoteException {
		System.exit(0);
	}


	//helper method, reads the values before they were written for this transaction
	private RMHashMap readPreviousValues(int xid) throws RemoteException
	{
		synchronized(previousData) {
			RMHashMap x_previousData = previousData.get(xid);
			if (x_previousData != null) {
				return (RMHashMap)x_previousData.clone();
			}
			return null;
		}
	}

	//store copy of what this key was, used to revert in case of abort
	public void writePreviousValue(int xid, String key, RMItem value) throws RemoteException
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

	//transaction is no longer active, can remove the storage of the state before writing
	public void removePreviousValues(int xid) throws RemoteException
	{
		synchronized(previousData) {
			previousData.remove(xid);
		}
	}


	//transaction is being aborted, revert the values to what they were before the writes of the transaction
	public void revertPreviousValues(int xid) throws RemoteException {
		RMHashMap prev_data = readPreviousValues(xid);

		Trace.info("RM::revertPreviousValues(" + xid + ") called");

		//if there are values that were written, revert them
		if(prev_data != null && prev_data.size() > 0) {
			for (Map.Entry<String, RMItem> entry : prev_data.entrySet()) {
				String key = entry.getKey();
				RMItem value = entry.getValue();
				
				//we already have the write locks for these keys because we wrote to them
				if(value == null) { //it did not exist before, simply remove it
					removeData(xid, key);
				} else { //it did exist, revert its value
					writeData(xid, key, value);
				}
			}
		} 
	}

	//log that the transaction is being committed
	public void transactionCommitted(int xid) throws RemoteException {
		Trace.info("RM::commitTransaction(" + xid + ") committed");
	}
}
 
