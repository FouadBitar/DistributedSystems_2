package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import Server.Exception.*;

import java.util.*;

public interface IMiddleware extends Remote  {


    /**
     * Add seats to a flight.
     *
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 
    
    /**
     * Add car at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 
   
    /**
     * Add room at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 			    
			    
    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 
    
    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
        throws RemoteException, TransactionAbortedException, InvalidTransactionException;

    /**
     * Delete the flight.
     *
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */   
    public boolean deleteFlight(int id, int flightNum) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 
    
    /**
     * Delete all cars at a location.
     *
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */		    
    public boolean deleteCars(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Delete all rooms at a location.
     *
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 
    
    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException;
    
    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) 
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws RemoteException, TransactionAbortedException, InvalidTransactionException; 

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
        throws RemoteException;


    /* NEWLY ADDED INTERFACE METHODS */
    public int start() 
    throws RemoteException;

    public boolean commit(int transactionId) 
    throws RemoteException, TransactionAbortedException, InvalidTransactionException;
    
    public void abort(int transactionId, boolean timedOut) 
    throws RemoteException, InvalidTransactionException;
    
    public void shutdown() 
    throws RemoteException;
    
}