package Client;

import Server.Interface.*;
import Server.Exception.*;


import java.util.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

public abstract class Client
{
	IMiddleware middleware = null;

	public Client()
	{
		super();
	}

	public abstract void connectServer();

	public void start()
	{
		// Prepare for reading commands
		System.out.println();
		System.out.println("Location \"help\" for list of supported commands");

		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		while (true)
		{
			// Read the next command
			String command = "";
			Vector<String> arguments = new Vector<String>();
			try {
				System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
				command = stdin.readLine().trim();
			}
			catch (IOException io) {
				System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0m" + io.getLocalizedMessage());
				io.printStackTrace();
				System.exit(1);
			}

			try {
				arguments = parse(command);
				Command cmd = Command.fromString((String)arguments.elementAt(0));
				try {
					execute(cmd, arguments);
				}
				catch (ConnectException e) {
					connectServer();
					execute(cmd, arguments);
				}
			}
			catch (IllegalArgumentException|ServerException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
			}
			catch (ConnectException|UnmarshalException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mConnection to server lost");
			}
			catch (Exception e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
				e.printStackTrace();
			}
		}
	}

	public void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException, InvalidTransactionException, TransactionAbortedException
	{
		switch (cmd)
		{
			case Help:
			{
				if (arguments.size() == 1) {
					System.out.println(Command.description());
				} else if (arguments.size() == 2) {
					Command l_cmd = Command.fromString((String)arguments.elementAt(1));
					System.out.println(l_cmd.toString());
				} else {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
				}
				break;
			}
			case Start:{
				//no arguments needed
				checkArgumentsCount(1, arguments.size());
				System.out.println("Creating a new transaction...");
				int trans_num = this.middleware.start();
				System.out.println("Transaction with <xid> = " +trans_num+ " added. \nPlease use this number for all operations belonging \nto this transaction.");
				break;
			}
			case Commit:{
				//no arguments needed
				checkArgumentsCount(2, arguments.size());
				System.out.println("Committing transaction with <xid> = "+ arguments.elementAt(1));
				
				boolean result = false;
				
				try {
					result = this.middleware.commit(Integer.parseInt(arguments.elementAt(1)));
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransactionAbortedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTransactionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Transaction with <xid> = " + arguments.elementAt(1) + " successfully committed!");
				break;
			}
			
			case Abort: {
				checkArgumentsCount(2, arguments.size());
				System.out.println("Aborting transaction with <xid> = "+ arguments.elementAt(1));
				
				try {
					this.middleware.abort(Integer.parseInt(arguments.elementAt(1)), false);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTransactionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
				
			}
			case Shutdown: {
				try{
					middleware.shutdown();
				} catch(RemoteException e) {
					System.out.println(e.getMessage());
				}
				break;
			}
			case AddFlight: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				System.out.println("-Flight Seats: " + arguments.elementAt(3));
				System.out.println("-Flight Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));
				int flightSeats = toInt(arguments.elementAt(3));
				int flightPrice = toInt(arguments.elementAt(4));

				try {
					if (middleware.addFlight(id, flightNum, flightSeats, flightPrice)) {
						System.out.println("Flight added");
					} else {
						System.out.println("Flight could not be added");
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTransactionException e) {
					// TODO Auto-generated catch block
					System.out.println("Transaction number does not exist.");
				}
				break;

				
				/* int xid = -1;

				if(id == 0) {
					xid = middleware.start();
					//perform operation
					try {
						if (middleware.addFlight(xid, flightNum, flightSeats, flightPrice)) {
							//operation successful, commit
							middleware.commit(xid);
							System.out.println("Flight added");
						} 
						else {
							//could not add, abort the transaction
							middleware.abort(xid, false);
							System.out.println("Flight could not be added, transaction was aborted");
						}
					} 
					catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
					catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }
					
					break;
				} else if(id == 1) {
					//test to see if multiple transactions can read at the same time
					int xid1 = middleware.start();
					int xid2 = middleware.start();
					int xid3 = middleware.start();

					//perform operation
					try {
						middleware.addCars(xid1, "montreal", 50, 40);
						middleware.commit(xid1);
						int cars2 = middleware.queryCars(xid2, "montreal");
						int cars3 = middleware.queryCars(xid3, "montreal");
						System.out.println("Number of cars at this location: " + cars2);
						System.out.println("Number of cars at this location: " + cars3);
						middleware.commit(xid2);
						middleware.commit(xid3);

					} 
					catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
					catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }


				} else if(id ==2) {
					//test to see if timeout unlocks the resource 
					int xid1 = middleware.start();
					int xid2 = middleware.start();
					// int xid3 = middleware.start();

					//perform operation
					try {
						middleware.addCars(xid1, "montreal", 50, 40);
						middleware.addCars(xid2, "toronto", 60, 40);
						int cars1 = middleware.queryCars(xid1, "montreal");
						System.out.println("Number of cars at this location: " + cars1);

						middleware.commit(xid1);
						middleware.commit(xid2);

					} 
					catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
					catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }
				} else if(id ==3) {
					//this tests to see if the timeouts occur for these 
					middleware.start();
					middleware.start();
					middleware.start();
				} else if (id == 4) {
					middleware.start();
				} else if (id == 5) {

					int xid33 = middleware.start();

					try{
						Thread.sleep(4000);
					} 
					catch(InterruptedException e) {
						System.out.println(e);
					} 

					middleware.commit(xid33);
				} else if (id == 6) {
					//check to see lock conversion
					int xid22 = middleware.start();
					int xid23 = middleware.start();
					try {
						middleware.queryFlight(xid23, 40);
						middleware.queryFlight(xid22, 45);
						middleware.addFlight(xid22, 46, 200, 200);
						middleware.commit(xid22);
					}
					catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
					catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }
					
				} else if (id == 7) {
					middleware.shutdown();
				} else if (id == 8) {
					int xid20 = middleware.start();
					middleware.reserveFlight(xid20, 10, 45);
					middleware.abort(xid20, false);
				}
				else {
					//perform operation
					try {

						
						middleware.addFlight(xid, flightNum, flightSeats, flightPrice);
						middleware.abort(xid, false);
					} 
					catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
					catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }
					
					break;
				} */
				// break;
				
			}
			case AddCars: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				System.out.println("-Number of Cars: " + arguments.elementAt(3));
				System.out.println("-Car Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numCars = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				if (middleware.addCars(id, location, numCars, price)) {
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
				}
				break;
			}
			case AddRooms: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				System.out.println("-Number of Rooms: " + arguments.elementAt(3));
				System.out.println("-Room Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numRooms = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				if (middleware.addRooms(id, location, numRooms, price)) {
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
				}
				break;
			}
			case AddCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

				int id = toInt(arguments.elementAt(1));
				int customer = middleware.newCustomer(id);

				System.out.println("Add customer ID: " + customer);
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				if (middleware.newCustomer(id, customerID)) {
					System.out.println("Add customer ID: " + customerID);
				} else {
					System.out.println("Customer could not be added");
				}
				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				try {
					if (middleware.deleteFlight(id, flightNum)) {
						System.out.println("Flight Deleted");
					} else {
						System.out.println("Flight could not be deleted");
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTransactionException e) {
					// TODO Auto-generated catch block
					System.out.println("Transaction does not exist.");
				}
				break;
			}
			case DeleteCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				if (middleware.deleteCars(id, location)) {
					System.out.println("Cars Deleted");
				} else {
					System.out.println("Cars could not be deleted");
				}
				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				if (middleware.deleteRooms(id, location)) {
					System.out.println("Rooms Deleted");
				} else {
					System.out.println("Rooms could not be deleted");
				}
				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				if (middleware.deleteCustomer(id, customerID)) {
					System.out.println("Customer Deleted");
				} else {
					System.out.println("Customer could not be deleted");
				}
				break;
			}
			case QueryFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				int seats;
				
					try {
						try {
							seats = middleware.queryFlight(id, flightNum);
							if(seats == -1) {
								System.out.println("Transaction number does not exist.");
								break;
							}
							System.out.println("Number of seats available: " + seats);
						} catch (InvalidTransactionException e) {
							// TODO Auto-generated catch block
							System.out.println("Transaction does not exist.");
						}
						
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				

				break;
			}
			case QueryCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int numCars = middleware.queryCars(id, location);
				System.out.println("Number of cars at this location: " + numCars);
				break;
			}
			case QueryRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int numRoom = middleware.queryRooms(id, location);
				System.out.println("Number of rooms at this location: " + numRoom);
				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				String bill = middleware.queryCustomerInfo(id, customerID);
				System.out.print(bill);
				break;               
			}
			case QueryFlightPrice: {
				checkArgumentsCount(3, arguments.size());
				
				System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				int price;
				try {
					price = middleware.queryFlightPrice(id, flightNum);
					System.out.println("Price of a seat: " + price);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTransactionException e) {
					System.out.println("Transaction does not exist.");
				}

				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int price = middleware.queryCarsPrice(id, location);
				System.out.println("Price of cars at this location: " + price);
				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int price = middleware.queryRoomsPrice(id, location);
				System.out.println("Price of rooms at this location: " + price);
				break;
			}
			case ReserveFlight: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Flight Number: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				int flightNum = toInt(arguments.elementAt(3));

				if (middleware.reserveFlight(id, customerID, flightNum)) {
					System.out.println("Flight Reserved");
				} else {
					System.out.println("Flight could not be reserved");
				}
				break;
			}
			case ReserveCar: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Car Location: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				if (middleware.reserveCar(id, customerID, location)) {
					System.out.println("Car Reserved");
				} else {
					System.out.println("Car could not be reserved");
				}
				break;
			}
			case ReserveRoom: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Room Location: " + arguments.elementAt(3));
				
				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				if (middleware.reserveRoom(id, customerID, location)) {
					System.out.println("Room Reserved");
				} else {
					System.out.println("Room could not be reserved");
				}
				break;
			}
			case Bundle: {
				if (arguments.size() < 7) {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					System.out.println("-Flight Number: " + arguments.elementAt(3+i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				Vector<String> flightNumbers = new Vector<String>();
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					flightNumbers.addElement(arguments.elementAt(3+i));
				}
				String location = arguments.elementAt(arguments.size()-3);
				boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
				boolean room = toBoolean(arguments.elementAt(arguments.size()-1));

				if (middleware.bundle(id, customerID, flightNumbers, location, car, room)) {
					System.out.println("Bundle Reserved");
				} else {
					System.out.println("Bundle could not be reserved");
				}
				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());

				System.out.println("Quitting client");
				System.exit(0);
				
			/* case AddCars: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				System.out.println("-Number of Cars: " + arguments.elementAt(3));
				System.out.println("-Car Price: " + arguments.elementAt(4));

				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numCars = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.addCars(xid, location, numCars, price)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Cars added");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Cars could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case AddRooms: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				System.out.println("-Number of Rooms: " + arguments.elementAt(3));
				System.out.println("-Room Price: " + arguments.elementAt(4));

				// int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numRooms = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.addRooms(xid, location, numRooms, price)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Rooms added");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Rooms could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case AddCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

				// int id = toInt(arguments.elementAt(1));
				//open transaction
				int xid = middleware.start();

				try {
					int customer = middleware.newCustomer(xid);
					
					middleware.commit(xid);

					System.out.println("Add customer ID: " + customer);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }
				
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.newCustomer(xid, customerID)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Add customer ID: " + customerID);
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Customer could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.deleteFlight(xid, flightNum)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Flight Deleted");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Flight could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case DeleteCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.deleteCars(xid, location)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Cars Deleted");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Cars could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.deleteRooms(xid, location)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Rooms Deleted");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Rooms could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.deleteCustomer(xid, customerID)) {
						//operation successful, commit
						middleware.commit(xid);
						System.out.println("Customer Deleted");
					} 
					else {
						//could not add, abort the transaction
						middleware.abort(xid, false);
						System.out.println("Customer could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case QueryFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				
				// int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int seats = middleware.queryFlight(xid, flightNum);
					System.out.println("Number of seats available: " + seats);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case QueryCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int numCars = middleware.queryCars(xid, location);
					System.out.println("Number of cars at this location: " + numCars);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case QueryRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int numRoom = middleware.queryRooms(xid, location);
					System.out.println("Number of rooms at this location: " + numRoom);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					String bill = middleware.queryCustomerInfo(xid, customerID);
					System.out.print(bill);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;               
			}
			case QueryFlightPrice: {
				checkArgumentsCount(3, arguments.size());
				
				System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int price = middleware.queryFlightPrice(xid, flightNum);
					System.out.println("Price of a seat: " + price);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int price = middleware.queryCarsPrice(xid, location);
					System.out.println("Price of cars at this location: " + price);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					int price = middleware.queryRoomsPrice(xid, location);
					System.out.println("Price of rooms at this location: " + price);
					middleware.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case ReserveFlight: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Flight Number: " + arguments.elementAt(3));

				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				int flightNum = toInt(arguments.elementAt(3));

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.reserveFlight(xid, customerID, flightNum)) {
						middleware.commit(xid);
						System.out.println("Flight Reserved");
					} else {
						middleware.abort(xid, false);
						System.out.println("Flight could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				
				break;
			}
			case ReserveCar: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Car Location: " + arguments.elementAt(3));

				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.reserveCar(xid, customerID, location)) {
						middleware.commit(xid);
						System.out.println("Car Reserved");
					} else {
						middleware.abort(xid, false);
						System.out.println("Car could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case ReserveRoom: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Room Location: " + arguments.elementAt(3));
				
				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				//open transaction
				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.reserveRoom(xid, customerID, location)) {
						middleware.commit(xid);
						System.out.println("Room Reserved");
					} else {
						middleware.abort(xid, false);
						System.out.println("Room could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case Bundle: {
				if (arguments.size() < 7) {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					System.out.println("-Flight Number: " + arguments.elementAt(3+i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				Vector<String> flightNumbers = new Vector<String>();
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					flightNumbers.addElement(arguments.elementAt(3+i));
				}
				String location = arguments.elementAt(arguments.size()-3);
				boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
				boolean room = toBoolean(arguments.elementAt(arguments.size()-1));

				int xid = middleware.start();

				//perform operation
				try {
					if (middleware.bundle(xid, customerID, flightNumbers, location, car, room)) {
						middleware.commit(xid);
						System.out.println("Bundle Reserved");
					} else {
						middleware.abort(xid, false);
						System.out.println("Bundle could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println(ei.getMessage()); } 
				catch (TransactionAbortedException ea) { System.out.println(ea.getMessage()); }

				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());

				System.out.println("Quitting client");
				System.exit(0); */
		}
	}

	public static Vector<String> parse(String command)
	{
		Vector<String> arguments = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(command,",");
		String argument = "";
		while (tokenizer.hasMoreTokens())
		{
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
	}

	public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException
	{
		if (expected != actual)
		{
			throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
		}
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (Integer.valueOf(string)).intValue();
	}

	public static boolean toBoolean(String string)// throws Exception
	{
		return (Boolean.valueOf(string)).booleanValue();
	}
}
