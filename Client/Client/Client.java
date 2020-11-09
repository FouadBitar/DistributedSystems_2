package Client;

import Server.Interface.*;
import Server.TransactionManager.InvalidTransactionException;
import Server.TransactionManager.TransactionAbortedException;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

public abstract class Client
{
	IResourceManager m_resourceManager = null;

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

				//open transaction
				int xid = 0;
				// int xid = m_resourceManager.start();
				

				if(id == 0) {
					
					//perform operation
					try {
						if (m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice)) {
							//operation successful, commit
							m_resourceManager.commit(xid);
							System.out.println("Flight added");
						} 
						else {
							//could not add, abort the transaction
							m_resourceManager.abort(xid);
							System.out.println("Flight could not be added, transaction was aborted");
						}
					} 
					catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
					catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }
					
					break;
				} else if(id == 1) {
					//test to see if multiple transactions can read at the same time
					int xid1 = m_resourceManager.start();
					int xid2 = m_resourceManager.start();
					int xid3 = m_resourceManager.start();

					//perform operation
					try {
						m_resourceManager.addCars(xid1, "montreal", 50, 40);
						m_resourceManager.commit(xid1);
						int cars2 = m_resourceManager.queryCars(xid2, "montreal");
						int cars3 = m_resourceManager.queryCars(xid3, "montreal");
						System.out.println("Number of cars at this location: " + cars2);
						System.out.println("Number of cars at this location: " + cars3);
						m_resourceManager.commit(xid2);
						m_resourceManager.commit(xid3);

					} 
					catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
					catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }


				} else if(id ==2) {
					//test to see if multiple writes can take place - they should not be able to
					//test to see if multiple transactions can read at the same time
					int xid1 = m_resourceManager.start();
					int xid2 = m_resourceManager.start();
					// int xid3 = m_resourceManager.start();

					//perform operation
					try {
						m_resourceManager.addCars(xid1, "montreal", 50, 40);
						m_resourceManager.addCars(xid2, "toronto", 60, 40);
						int cars1 = m_resourceManager.queryCars(xid1, "montreal");
						System.out.println("Number of cars at this location: " + cars1);

						m_resourceManager.commit(xid1);
						m_resourceManager.commit(xid2);

					} 
					catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
					catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }
				} else if(id ==3) {
					int xid4 = m_resourceManager.start();
					int xid5 = m_resourceManager.start();
					int xid6 = m_resourceManager.start();
				} else if (id == 4) {
					m_resourceManager.start();
				}
				else {
					//perform operation
					try {

						
						m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
						m_resourceManager.abort(xid);
					} 
					catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
					catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }
					
					break;
				}
				break;
				
			}
			case AddCars: {
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
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.addCars(xid, location, numCars, price)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Cars added");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Cars could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

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
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.addRooms(xid, location, numRooms, price)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Rooms added");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Rooms could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case AddCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

				// int id = toInt(arguments.elementAt(1));
				//open transaction
				int xid = m_resourceManager.start();

				try {
					int customer = m_resourceManager.newCustomer(xid);
					
					m_resourceManager.commit(xid);

					System.out.println("Add customer ID: " + customer);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }
				
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.newCustomer(xid, customerID)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Add customer ID: " + customerID);
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Customer could not be added");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.deleteFlight(xid, flightNum)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Flight Deleted");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Flight could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case DeleteCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.deleteCars(xid, location)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Cars Deleted");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Cars could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.deleteRooms(xid, location)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Rooms Deleted");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Rooms could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.deleteCustomer(xid, customerID)) {
						//operation successful, commit
						m_resourceManager.commit(xid);
						System.out.println("Customer Deleted");
					} 
					else {
						//could not add, abort the transaction
						m_resourceManager.abort(xid);
						System.out.println("Customer could not be deleted");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case QueryFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				
				// int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int seats = m_resourceManager.queryFlight(xid, flightNum);
					System.out.println("Number of seats available: " + seats);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				break;
			}
			case QueryCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int numCars = m_resourceManager.queryCars(xid, location);
					System.out.println("Number of cars at this location: " + numCars);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
				break;
			}
			case QueryRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				//int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int numRoom = m_resourceManager.queryRooms(xid, location);
					System.out.println("Number of rooms at this location: " + numRoom);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					String bill = m_resourceManager.queryCustomerInfo(xid, customerID);
					System.out.print(bill);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
				break;               
			}
			case QueryFlightPrice: {
				checkArgumentsCount(3, arguments.size());
				
				System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				//int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int price = m_resourceManager.queryFlightPrice(xid, flightNum);
					System.out.println("Price of a seat: " + price);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int price = m_resourceManager.queryCarsPrice(xid, location);
					System.out.println("Price of cars at this location: " + price);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				// int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				//open transaction
				int xid = m_resourceManager.start();

				//perform operation
				try {
					int price = m_resourceManager.queryRoomsPrice(xid, location);
					System.out.println("Price of rooms at this location: " + price);
					m_resourceManager.commit(xid);
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
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
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.reserveFlight(xid, customerID, flightNum)) {
						m_resourceManager.commit(xid);
						System.out.println("Flight Reserved");
					} else {
						m_resourceManager.abort(xid);
						System.out.println("Flight could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

				
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
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.reserveCar(xid, customerID, location)) {
						m_resourceManager.commit(xid);
						System.out.println("Car Reserved");
					} else {
						m_resourceManager.abort(xid);
						System.out.println("Car could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

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
				int xid = m_resourceManager.start();

				//perform operation
				try {
					if (m_resourceManager.reserveRoom(xid, customerID, location)) {
						m_resourceManager.commit(xid);
						System.out.println("Room Reserved");
					} else {
						m_resourceManager.abort(xid);
						System.out.println("Room could not be reserved");
					}
				} 
				catch (InvalidTransactionException ei) { System.out.println("This transaction id is invalid: " + xid); } 
				catch (TransactionAbortedException ea) { System.out.println("This transaction has been aborted: " + xid); }

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

				if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
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
