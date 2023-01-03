package serverside;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService(endpointInterface = "serverside.ServerOperationsInterface")
@SOAPBinding(style=Style.RPC)
public class ServerOperationsImpl implements ServerOperationsInterface {

	public String serverName;
	public File logFile;
	public HashMap<String, HashMap<String, Festival>> database;

	public ServerOperationsImpl(String serverName, HashMap<String, HashMap<String, Festival>> db) throws Exception {
		super();
		this.serverName = serverName;
		database = new HashMap<>(db);
		try {
			logFile = new File(serverName + "-log");
			if (logFile.createNewFile()) {
				System.out.println(logFile.getName() + " file created.");
			} else {
				System.out.println(logFile.getName() + " file not created");
			}
		} catch (IOException e) {
			System.out.println("Error creating file.");
			e.printStackTrace();
		}
	}

	@Override
	public String addReservationSlot(String eventID, String eventType, int capacity) {
		String requestTime = getTime();
		boolean success;
		String resultString;
		if (!checkEventType(eventType)) {
			success = false;
			resultString = "Invalid event type";
		} else if (!checkIDLength(eventID)) {
			success = false;
			resultString = "Invalid event ID";
		} else if (!checkCity(eventID)) {
			success = false;
			resultString = "Invalid city";
		} else if (!checkTime(eventID)) {
			success = false;
			resultString = "Invalid time of day";
		} else if (!checkDate(eventID)) {
			success = false;
			resultString = "Invalid date";
		} else {
			Festival check = null;
			Iterator it = database.entrySet().iterator();
			// checks if the eventID is unique across the event types
			while (it.hasNext()) {
				Map.Entry eventPair = (Map.Entry) it.next();
				check = ((HashMap<String, Festival>) eventPair.getValue()).get(eventID);
				if (check != null)
					break;
			}
			if (check == null) {
				HashMap<String, Festival> event = database.get(eventType);
				synchronized (this) {
					Festival result = event.put(eventID, new Festival(capacity));
				}
				success = true;
				resultString = "Added reservation slot " + eventID + " to database successfully";
			} else {
				success = false;
				resultString = "Adding reservation slot " + eventID + " to database rejected";
			}
		}
		String[] parameters = { "eventID", "eventType", "capacity" };
		updateLog(requestTime, "addReservationsSlot", parameters, success, resultString);
		return resultString;
	}

	@Override
	public String removeReservationSlot(String eventID, String eventType) {
		String requestTime = getTime();
		boolean success;
		String resultString;
		if (!checkEventType(eventType)) {
			success = false;
			resultString = "Invalid event type";
		} else if (!checkIDLength(eventID)) {
			success = false;
			resultString = "Invalid event ID";
		} else if (!checkCity(eventID)) {
			success = false;
			resultString = "Invalid city";
		} else if (!checkTime(eventID)) {
			success = false;
			resultString = "Invalid time of day";
		} else if (!checkDate(eventID)) {
			success = false;
			resultString = "Invalid date";
		} else {
			HashMap<String, Festival> event = database.get(eventType);
			Festival result = event.get(eventID);
			// checks if the event exists
			if (result != null) {
				// check if the event is booked
				if (result.getBooked() > 0) {
					success = false;
					resultString = "Removing reservation slot " + eventID
							+ " failed because event is booked by one or more users";
				} else {
					synchronized (this) {
						result = event.remove(eventID);
					}
					if (result != null) {
						success = true;
						resultString = "Removed reservation slot " + eventID + " from database successfully";
					} else {
						success = false;
						resultString = "Removing reservation slot " + eventID + " from database rejected";
					}
				}
			} else {
				success = false;
				resultString = "Removing reservation slot " + eventID
						+ " from database failed because it does not exists";
			}
		}
		String[] parameters = { "eventID", "eventType" };
		updateLog(requestTime, "removeReservationSlot", parameters, success, resultString);
		return resultString;
	}

	@Override
	public String listReservationSlotAvailable(String eventType) {
		String requestTime = getTime();
		String result = eventType + ":\n";
		boolean success;
		String resultString;
		if (!checkEventType(eventType)) {
			success = false;
			resultString = "Invalid event type";
		} else {
			success = true;
			resultString = "Listed all available reservation slots";
			result = listReservationSlotAvailableLocal(eventType);
		}

		// UDP section
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket();
			String requestString = "A " + eventType;
			byte[] m = requestString.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			result += sendPacket(aSocket, m, requestString, host);
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}

		String[] parameters = { "eventType" };
		updateLog(requestTime, "listReservationSlotAvailable", parameters, success, resultString);
		if (success)
			return result;
		else
			return resultString;
	}

	@Override
	public String reserveTicket(String participantID, String eventID, String eventType) {
		String requestTime = getTime();
		boolean success = false;
		String resultString = null;
		if (!checkEventType(eventType)) {
			success = false;
			resultString = "Invalid event type";
		} else if (!checkIDLength(eventID)) {
			success = false;
			resultString = "Invalid event ID";
		} else if (!checkCity(eventID)) {
			success = false;
			resultString = "Invalid city";
		} else if (!checkTime(eventID)) {
			success = false;
			resultString = "Invalid time of day";
		} else if (!checkDate(eventID)) {
			success = false;
			resultString = "Invalid date";
		} else {
			HashMap<String, Festival> event = database.get(eventType);
			Festival festival = event.get(eventID);

			String pattern = "ddMMyy";
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			String addDateString = eventID.substring(4);
			Date addDate = null;
			try {
				addDate = sdf.parse(addDateString);
			} catch (ParseException e) {
				System.out.println("Parse exception AddDateString = " + addDateString);
				e.printStackTrace();
			}

			String[] userEvents = getEventScheduleLocal(participantID).trim().split("\n");

			// check if the event to be added has the same day of one of the days in the
			// database
			boolean isSameDay = false;
			Iterator it = event.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry eventPair = (Map.Entry) it.next();
				String tempEventDate = ((String) eventPair.getKey()).substring(4);
				Festival tempEvent = (Festival) eventPair.getValue();
				if (tempEventDate.equals(addDateString) && tempEvent.getBookings().contains(participantID)) {
					isSameDay = true;
					break;
				}
			}

			// check if the user is local or not
			if (getServerName().equals(participantID.substring(0, 3))) {
				// checks if festival exists and if its full
				if (festival == null) {
					success = false;
					resultString = "Event with ID " + eventID + " does not exist";
				} else if (festival.isFull()) {
					success = false;
					resultString = "Event with ID " + eventID + " is full";
				} else if (isSameDay) {
					success = false;
					resultString = "Can't reserve user " + participantID + " to event " + eventID
							+ " because user is already reserved to another event on the same day";
				} else {
					synchronized (this) {
						success = festival.addBookings(participantID);
					}
					if (success)
						resultString = "User " + participantID + " was successfully added to event " + eventID;
					else
						resultString = "User " + participantID + " was not added to event " + eventID
								+ " because user is already in the event";
				}
			} else {
				// checks if the event exists or if full
				if (festival == null) {
					success = false;
					resultString = "Event with ID " + eventID + " does not exist";
				} else if (festival.isFull()) {
					success = false;
					resultString = "Event with ID " + eventID + " is full";
				} else if (isSameDay) {
					success = false;
					resultString = "Can't reserve user " + participantID + " to event " + eventID
							+ " because user is already reserved to another event on the same day";
				} else {

					boolean userAllowedAdd = true;

					// counter for the 7 day limit
					// each index is a week
					// and each index starts at (7 + index offset) days
					int weeklyCounter[] = new int[7];

					if (!userEvents[0].isEmpty()) {
						for (int i = 0; i < userEvents.length; i++) {
							// MASSIVELY CHANGED
							String tempDateString = userEvents[i].trim().substring(4);
							Date tempDate = null;
							try {
								tempDate = sdf.parse(tempDateString);
							} catch (ParseException e) {
								System.out.println("Parse exception AddDateString = " + addDateString);
								e.printStackTrace();
							}
							// CHANGES FROM A1 post submission

							// count the gap
							long diffInMillies = tempDate.getTime() - addDate.getTime();
							long gap = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

							// count the days that is in the proper weeks
							if (gap >= -7 && gap <= 0)
								weeklyCounter[0]++;
							if (gap >= -6 && gap <= 1)
								weeklyCounter[1]++;
							if (gap >= -5 && gap <= 2)
								weeklyCounter[2]++;
							if (gap >= -4 && gap <= 3)
								weeklyCounter[3]++;
							if (gap >= -3 && gap <= 4)
								weeklyCounter[4]++;
							if (gap >= -2 && gap <= 5)
								weeklyCounter[5]++;
							if (gap >= -1 && gap <= 6)
								weeklyCounter[6]++;
							if (gap >= 0 && gap <= 7)
								weeklyCounter[7]++;

							// break if any of the numbers in the array is over 4
							for (int j = 0; j < weeklyCounter.length; j++) {
								if (weeklyCounter[j] >= 3) {
									userAllowedAdd = false;
									break;
								}
							}

							if (!userAllowedAdd)
								break;
						}
					}
					if (userAllowedAdd) {
						synchronized (this) {
							success = festival.addBookings(participantID);
						}
						if (success)
							resultString = "User " + participantID + " was successfully added to event " + eventID;
						else
							resultString = "User " + participantID + " was not added to event " + eventID;
					} else {
						success = false;
						resultString = "User " + participantID + " already has 3 events within a week";
					}
				}
			}
		}
		String[] parameters = { "participantID", "eventID", "eventType" };
		updateLog(requestTime, "reserveTicket", parameters, success, resultString);
		return resultString;
	}

	@Override
	public String getEventSchedule(String participantID) {
		String requestTime = getTime();
		boolean success = false;
		String result = "";
		String resultString = "";

		success = true;
		result += getEventScheduleLocal(participantID);
		resultString = "Listed all event schedule of user " + participantID;

		// UDP section
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket();
			String requestString = "P " + participantID;
			byte[] m = requestString.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			result += sendPacket(aSocket, m, requestString, host);
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}

		String[] parameters = { "participantID" };
		updateLog(requestTime, "getEventSchedule", parameters, success, resultString);
		if (success)
			return result;
		else
			return resultString;
	}

	@Override
	public String cancelTicket(String participantID, String eventID) {
		String requestTime = getTime();
		boolean success;
		String resultString = null;
		if (!checkIDLength(eventID)) {
			success = false;
			resultString = "Invalid event ID";
		} else if (!checkCity(eventID)) {
			success = false;
			resultString = "Invalid city";
		} else if (!checkTime(eventID)) {
			success = false;
			resultString = "Invalid time of day";
		} else if (!checkDate(eventID)) {
			success = false;
			resultString = "Invalid date";
		} else {
			Festival festival = null;
			Iterator it = database.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry eventPair = (Map.Entry) it.next();
				festival = ((HashMap<String, Festival>) eventPair.getValue()).get(eventID);
				if (festival != null)
					break;
			}
			if (festival == null) {
				success = false;
				resultString = "Event with ID " + eventID + " does not exist";
			} else {
				synchronized (this) {
					success = festival.removeBookings(participantID);
				}
				if (success)
					resultString = "User " + participantID + " was successfully removed from event " + eventID;
				else
					resultString = "User " + participantID + " was not removed because user is not in event " + eventID;
			}
		}
		String[] parameters = { "participantID", "eventID" };
		updateLog(requestTime, "cancelTicket", parameters, success, resultString);
		return resultString;
	}

	public String exchangeTickets(String participantID, String eventID, String newEventID, String newEventType) {
		String requestTime = getTime();
		boolean success;
		String resultString = null;
		if (!checkIDLength(eventID)) {
			success = false;
			resultString = "Invalid event ID";
		} else if (!checkCity(eventID)) {
			success = false;
			resultString = "Invalid city";
		} else if (!checkTime(eventID)) {
			success = false;
			resultString = "Invalid time of day";
		} else if (!checkDate(eventID)) {
			success = false;
			resultString = "Invalid date";
		} else if (!checkEventType(newEventType)) {
			success = false;
			resultString = "Invalid event type";
		} else {
			Festival tbcEvent = getEvent(eventID);
			synchronized (this) {
				if (tbcEvent != null) {
					boolean tbcEventContainsUser = tbcEvent.isUserBooked(participantID);
					if (tbcEventContainsUser) {
						String tbaEventCity = getEventCity(newEventID);
						if (tbaEventCity.equals(serverName)) {
							Festival tbaEvent = getEvent(newEventID, newEventType);
							if (tbaEvent != null) {
								boolean tbaEventContainsUser = tbaEvent.isUserBooked(participantID);
								if (!tbaEventContainsUser) {
									synchronized (this) {
										tbcEvent.removeBookings(participantID);
										tbaEvent.addBookings(participantID);
									}
									success = true;
									resultString = "Exhange successful between event " + eventID + " and event "
											+ newEventID + " for user " + participantID + " was successful";
								} else {
									success = false;
									resultString = "User is already reserved to the to-be-added event " + newEventID;
								}
							} else {
								success = false;
								resultString = "Event " + newEventID + " can't be exchanged because it does not exist";
							}
						} else {
							// UDP section
							String result = "";
							DatagramSocket aSocket = null;
							try {
								aSocket = new DatagramSocket();
								String requestString = "C " + participantID + " " + newEventID + " " + newEventType;
								byte[] m = requestString.getBytes();
								InetAddress host = InetAddress.getByName("localhost");
								result += sendPacketToOneCity(aSocket, m, requestString, host, tbaEventCity);
							} catch (SocketException e) {
								System.out.println("Socket: " + e.getMessage());
							} catch (IOException e) {
								System.out.println("IO: " + e.getMessage());
							} finally {
								if (aSocket != null)
									aSocket.close();
							}

							// Process Received Response
							String[] resultArr = result.split(" ");
							boolean tbaEventExists = Integer.parseInt(resultArr[0]) == 1 ? true : false;
							boolean tbaEventContainsUser = Integer.parseInt(resultArr[1]) == 1 ? true : false;

							if (tbaEventExists) {
								if (!tbaEventContainsUser) {
									synchronized (this) {
										tbcEvent.removeBookings(participantID);
										try {
											aSocket = new DatagramSocket();
											String requestString = "R " + participantID + " " + newEventID + " "
													+ newEventType;
											byte[] m = requestString.getBytes();
											InetAddress host = InetAddress.getByName("localhost");
											result += sendPacketToOneCity(aSocket, m, requestString, host,
													tbaEventCity);
										} catch (SocketException e) {
											System.out.println("Socket: " + e.getMessage());
										} catch (IOException e) {
											System.out.println("IO: " + e.getMessage());
										} finally {
											if (aSocket != null)
												aSocket.close();
										}
									}
									success = true;
									resultString = "Exhange successful between event " + eventID + " and event "
											+ newEventID + " for user " + participantID + " was successful";
								} else {
									success = false;
									resultString = "User is already reserved to the to-be-added event " + newEventID;
								}
							} else {
								success = false;
								resultString = "Event " + newEventID + " can't be exchanged because it does not exist";
							}
						}
					} else {
						success = false;
						resultString = "User is not reserved in the to-be-cancelled event " + eventID;
					}
				} else {
					success = false;
					resultString = "Event " + eventID + " can't be cancelled because it does not exist";
				}
			}
		}
		String[] parameters = { "participantID", "eventID", "newEventID", "newEventType" };
		updateLog(requestTime, "exchangeTickets", parameters, success, resultString);
		return resultString;
	}

	private void updateLog(String time, String requestType, String[] requestParameters, boolean success,
			String response) {
		try {
			FileWriter writer = new FileWriter(logFile.getName(), true);
			writer.append("----------------------------------------\n");
			writer.append("Date and time: " + time + "\n");
			writer.append("Request type: " + requestType + System.lineSeparator());
			String rp = "";
			for (int i = 0; i < requestParameters.length; i++) {
				if (i == 0)
					rp += "Request parameters: " + requestParameters[i];
				else
					rp += ", " + requestParameters[i];
			}
			writer.write(rp + "\n");
			writer.write("Success: " + success + "\n");
			writer.write("Server response: " + response + "\n");
			writer.write("\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Writing to file error.");
			e.printStackTrace();
		}
	}

	/**
	 * check if database has the event id
	 * 
	 * @param eventID
	 * @return null if database does not have the eventID, otherwise returns the
	 *         object
	 */
	public Festival getEvent(String eventID) {
		Festival festival = null;
		Iterator it = database.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry eventPair = (Map.Entry) it.next();
			festival = ((HashMap<String, Festival>) eventPair.getValue()).get(eventID);
			if (festival != null)
				break;
		}
		return festival;
	}

	/**
	 * check if the event type has the event id in the database
	 * 
	 * @param eventID
	 * @param eventType
	 * @return
	 */
	public Festival getEvent(String eventID, String eventType) {
		Festival festival = null;
		HashMap<String, Festival> eventTypeMap = database.get(eventType);
		festival = eventTypeMap.get(eventID);
		return festival;
	}

	/**
	 * gets the city from the event id
	 * 
	 * @param eventID
	 * @return
	 */
	private String getEventCity(String eventID) {
		return eventID.substring(0, 3);
	}

	private String getTime() {
		LocalDateTime rawDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String dateTime = rawDateTime.format(formatter);

		return dateTime;
	}

	private boolean checkEventType(String eventType) {
		return eventType.equals("Concerts") || eventType.equals("Art Gallery") || eventType.equals("Theatre");
	}

	private boolean checkIDLength(String eventID) {
		return eventID.length() == 10;
	}

	@Override
	public boolean checkCity(String eventID) {
		String city = eventID.substring(0, 3);
		return city.equals("MTL") || city.equals("TOR") || city.equals("VAN");
	}

	private boolean checkTime(String eventID) {
		char time = eventID.charAt(3);
		return time == 'A' || time == 'M' || time == 'E';
	}

	private boolean checkDate(String eventID) {
		String date = eventID.substring(4);
		int day, month, year;
		try {
			day = Integer.parseInt(date.substring(0, 2));
			month = Integer.parseInt(date.substring(2, 4));
			year = Integer.parseInt(date.substring(4));
			if (day < 0 || day > 30 || month < 0 || month > 12 || year < 0 || year > 25)
				return false;
			else
				return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public String showOptions(boolean isUserAdmin) {
		int choice = -1;
		String options;
		if (isUserAdmin) {
			options = "\t1. Add reservation slot\n" + "\t2. Remove reservation slot\n"
					+ "\t3. List reservation slot available\n" + "\t4. Reserve ticket\n" + "\t5. Get event schedule\n"
					+ "\t6. Cancel ticket\n" + "\t7. Exchange ticket\n" + "\tEnter 0 to exit.";

		} else {
			options = "\t1. Reserve ticket\n" + "\t2. Get event schedule\n" + "\t3. Cancel ticket\n"
					+ "\t4. Exchange ticket\n" + "\tEnter 0 to exit.";
		}
		return options;
	}

	@Override
	public boolean isAdmin(String userID) {
		return userID.charAt(3) == 'A';
	}

	private String getServerName() {
		return serverName.substring(0, 3);
	}

	private String sendPacket(DatagramSocket aSocket, byte[] m, String temp, InetAddress host) throws IOException {
		String result = "";
		int serverPortMTL = 5000;
		int serverPortTOR = 5001;
		int serverPortVAN = 5002;
		if (serverName.equals("MTL")) {
			DatagramPacket request = new DatagramPacket(m, temp.length(), host, serverPortTOR);
			aSocket.send(request);
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim() + "\n";
			request = new DatagramPacket(m, temp.length(), host, serverPortVAN);
			aSocket.send(request);
			buffer = new byte[1000];
			reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim();
		} else if (serverName.equals("TOR")) {
			DatagramPacket request = new DatagramPacket(m, temp.length(), host, serverPortMTL);
			aSocket.send(request);
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim() + "\n";
			request = new DatagramPacket(m, temp.length(), host, serverPortVAN);
			aSocket.send(request);
			buffer = new byte[1000];
			reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim();
		} else {
			DatagramPacket request = new DatagramPacket(m, temp.length(), host, serverPortMTL);
			aSocket.send(request);
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim() + "\n";
			request = new DatagramPacket(m, temp.length(), host, serverPortTOR);
			aSocket.send(request);
			buffer = new byte[1000];
			reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			result += "\t" + (new String(reply.getData())).trim();
		}
		return result;
	}

	private String sendPacketToOneCity(DatagramSocket aSocket, byte[] m, String temp, InetAddress host, String city)
			throws IOException {
		String result = "";
		int serverPortMTL = 5000;
		int serverPortTOR = 5001;
		int serverPortVAN = 5002;
		int serverPort = 0;
		if (city.equals("MTL")) {
			serverPort = serverPortMTL;
		} else if (city.equals("TOR")) {
			serverPort = serverPortTOR;
		} else {
			serverPort = serverPortVAN;
		}
		DatagramPacket request = new DatagramPacket(m, temp.length(), host, serverPort);
		aSocket.send(request);
		byte[] buffer = new byte[1000];
		DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
		aSocket.receive(reply);
		result += (new String(reply.getData())).trim();
		return result;
	}

	@Override
	public String listReservationSlotAvailableLocal(String eventType) {
		String result = "";
		Map eventMap = database.get(eventType);
		Iterator it = eventMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry eventPair = (Map.Entry) it.next();
			Festival temp = ((Festival) eventPair.getValue());
			result += "\t" + eventPair.getKey() + " " + (temp.getCapacity() - temp.getBooked()) + "\n";
		}
		return result;
	}

	@Override
	public String getEventScheduleLocal(String participantID) {
		String result = "";
		Iterator it = database.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry dbPair = (Map.Entry) it.next();
			Iterator iterator = ((HashMap<String, Festival>) dbPair.getValue()).entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry eventPair = (Map.Entry) iterator.next();
				Festival festival = ((Festival) eventPair.getValue());
				if (festival.isUserBooked(participantID)) {
					result += "\t" + eventPair.getKey() + "\n";
				}
			}
		}
		return result;
	}

}
