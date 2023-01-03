package clientside;

import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import serverside.ServerOperationsInterface;

public class Client {

	private static Scanner keyboard = new Scanner(System.in);

	public static void main(String[] args) throws Exception {

		System.out.print("Enter user ID: ");
		String userID = keyboard.nextLine();

		if (userID.length() < 8) {
			System.out.println("Invalid ID");
			System.exit(0);
		}
		String city = userID.substring(0, 3);
		char userStatus = userID.charAt(3);
		String userIDNumber = userID.substring(4);
		if (!city.equals("MTL") && !city.equals("TOR") && !city.equals("VAN")) {
			System.out.println("Wrong city!");
			System.exit(0);
		} else if (userStatus != 'A' && userStatus != 'P') {
			System.out.println("Wrong user status!");
			System.exit(0);
		} else if (userIDNumber.length() != 4 || !isInteger(userIDNumber)) {
			System.out.println("Wrong user ID number!");
			System.exit(0);
		}

		File logFile = new File(userID + "-client-log.txt");

		String wsPort = getWSPort(city);
		String wsCity = city.toLowerCase();

		URL url = new URL("http://localhost:" + wsPort + "/" + wsCity + "?wsdl");

		QName qName = new QName("http://serverside/", "ServerOperationsImplService");

		// Gets the Service
		Service service = Service.create(url, qName);

		// Gets the Service Port
		ServerOperationsInterface server = service.getPort(ServerOperationsInterface.class);

		boolean isUserAdmin = server.isAdmin(userID);
		System.out.println("Choose from the following options");
		System.out.println(server.showOptions(isUserAdmin));
		System.out.print("Choice: ");
		int choice = keyboard.nextInt();
		keyboard.nextLine();

		int capacity = 0;
		String eventID = null;
		String eventType = null;
		String dateTime = null;
		String serverResponse = null;

		while (choice != 0) {
			String wsDestCity = "";
			String wsDestPort = "";
			if (isUserAdmin) {
			} else {
				choice += 3;
				if (choice == 4) {
					choice = 10;
				}
			}
			if (choice == 1) {
				System.out.println("Adding reservation slot:");
				eventType = promptString("event type");
				eventID = promptString("event ID");
				capacity = promptInt("capacity");
				dateTime = getTime();
				
				server = getServer(getEventCity(eventID));
				
				serverResponse = server.addReservationSlot(eventID, eventType, capacity);
				updateLog(logFile.getName(), "Add reservation slot", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 2) {
				System.out.println("Removing reservation slot:");
				eventType = promptString("event type");
				eventID = promptString("event ID");
				dateTime = getTime();
				
				server = getServer(getEventCity(eventID));
				
				serverResponse = server.removeReservationSlot(eventID, eventType);
				updateLog(logFile.getName(), "Remove reservation slot", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 3) {
				System.out.println("Listing available reservation slot: ");
				eventType = promptString("event type");
				dateTime = getTime();
				
				server = getServer(city);
				
				serverResponse = server.listReservationSlotAvailable(eventType);
				updateLog(logFile.getName(), "List available reservation slot", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 4) {
				System.out.println("Reserving ticket: ");
				eventType = promptString("event type");
				eventID = promptString("eventID");
				String otherUserID = promptString("userID");
				dateTime = getTime();
				
				server = getServer(getEventCity(eventID));
				
				serverResponse = server.reserveTicket(otherUserID, eventID, eventType);
				updateLog(logFile.getName(), "Reserve ticket", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 5) {
				System.out.println("Getting event schedule: ");
				dateTime = getTime();
				
				server = getServer(city);
				
				serverResponse = server.getEventSchedule(userID);
				updateLog(logFile.getName(), "Get event schedule", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 6) {
				System.out.println("Cancelling ticket: ");
				eventID = promptString("eventID");
				dateTime = getTime();

				server = getServer(getEventCity(eventID));
				
				serverResponse = server.cancelTicket(userID, eventID);
				updateLog(logFile.getName(), "Cancel ticket", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 7) {
				System.out.println("Exchanging ticket: ");
				eventType = promptString("event type");
				String tbcEventID = promptString("to-be-cancelled eventID");
				String tbaEventID = promptString("to-be-reserved eventID");
				dateTime = getTime();

				server = getServer(getEventCity(eventID));
				
				serverResponse = server.exchangeTickets(userID, tbcEventID, tbaEventID, eventType);
				updateLog(logFile.getName(), "Exhange ticket", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else if (choice == 10) {
				System.out.println("Reserving ticket: ");
				eventType = promptString("event type");
				eventID = promptString("eventID");
				dateTime = getTime();
				
				server = getServer(getEventCity(eventID));
				
				serverResponse = server.reserveTicket(userID, eventID, eventType);
				updateLog(logFile.getName(), "Reserve ticket", serverResponse, dateTime);
				System.out.println(serverResponse);
			} else
				System.out.println("Invalid option. Pick again.");
			System.out.println();
			System.out.println("===========================================");
			System.out.println("Choose from the following options");
			System.out.println(server.showOptions(isUserAdmin));
			System.out.print("Choice: ");
			choice = keyboard.nextInt();
			keyboard.nextLine();
		}

		keyboard.close();
	}

	public static boolean isInteger(String str) {
		if (str == null || str.length() == 0) {
			return false;
		}
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private static String promptString(String dataName) {
		System.out.print("Enter " + dataName + ": ");
		String rValue = keyboard.nextLine();
		return rValue;
	}

	private static int promptInt(String dataName) {
		System.out.print("Enter " + dataName + ": ");
		int rValue = keyboard.nextInt();
		return rValue;
	}

	private static void updateLog(String fileName, String request, String response, String dateTime) {
		try {
			FileWriter writer = new FileWriter(fileName, true);
			writer.write("----------------------------------------\n");
			writer.write("Date and time requested: " + dateTime + "\n");
			writer.write("Request: " + request + "\n");
			writer.write("Response: " + response + "\n\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Writing to file error.");
			e.printStackTrace();
		}
	}

	private static String getTime() {
		LocalDateTime rawDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String dateTime = rawDateTime.format(formatter);

		return dateTime;
	}

	private static String getEventCity(String eventID) {
		String city = eventID.substring(0, 3);
		return city;
	}

	private static int getPort(String city) {
		int port = 0;
		if (city.equals("MTL"))
			port = 5000;
		else if (city.equals("TOR"))
			port = 5001;
		else if (city.equals("VAN"))
			port = 5002;
		else
			port = 9999;
		return port;
	}

	private static String getWSPort(String city) {
		String port = "";
		if (city.equals("MTL"))
			port = "6000";
		else if (city.equals("TOR"))
			port = "6001";
		else if (city.equals("VAN"))
			port = "6002";
		else
			port = "9999";
		return port;
	}
	
	private static ServerOperationsInterface getServer(String eventCity) throws Exception {
		
		String wsDestCity = eventCity.toLowerCase();
		String wsDestPort = getWSPort(eventCity);
		URL url = new URL("http://localhost:" + wsDestPort + "/" + wsDestCity + "?wsdl");
		QName qName = new QName("http://serverside/", "ServerOperationsImplService");
		Service service = Service.create(url, qName);
		ServerOperationsInterface server = service.getPort(ServerOperationsInterface.class);
		
		return server;
	}
}
