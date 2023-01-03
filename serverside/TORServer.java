package serverside;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.HashMap;

import javax.xml.ws.Endpoint;

public class TORServer extends Thread {

	static String city;
	static int serverPort;
	static ServerOperationsInterface serverOps;

	public static void main(String[] args) throws Exception {

		city = "TOR";
		serverPort = 5001;

		String fest1ID = city + "M010122";
		Festival fest1 = new Festival(10);
		String fest2ID = city + "M020122";
		Festival fest2 = new Festival(20);
		String fest3ID = city + "A030122";
		Festival fest3 = new Festival(30);
		String fest4ID = city + "E040122";
		Festival fest4 = new Festival(40);
		String fest5ID = city + "E050122";
		Festival fest5 = new Festival(50);
		HashMap<String, Festival> sampleF1 = new HashMap<String, Festival>() {
			{
				put(fest1ID, fest1);
				put(fest2ID, fest2);
				put(fest3ID, fest3);
				put(fest4ID, fest4);
				put(fest5ID, fest5);
			}
		};

		String fest6ID = city + "M060122";
		Festival fest6 = new Festival(10);
		String fest7ID = city + "M070122";
		Festival fest7 = new Festival(20);
		String fest8ID = city + "A080122";
		Festival fest8 = new Festival(30);
		HashMap<String, Festival> sampleF2 = new HashMap<String, Festival>() {
			{
				put(fest6ID, fest6);
				put(fest7ID, fest7);
				put(fest8ID, fest8);
			}
		};

		String fest9ID = city + "M090122";
		Festival fest9 = new Festival(10);
		String fest10ID = city + "M100122";
		Festival fest10 = new Festival(20);
		String fest11ID = city + "A110122";
		Festival fest11 = new Festival(30);
		HashMap<String, Festival> sampleF3 = new HashMap<String, Festival>() {
			{
				put(fest9ID, fest9);
				put(fest10ID, fest10);
				put(fest11ID, fest11);
			}
		};

		HashMap<String, HashMap<String, Festival>> sampleDB = new HashMap<String, HashMap<String, Festival>>() {
			{
				put("Art Gallery", sampleF1);
				put("Concerts", sampleF2);
				put("Theatre", sampleF3);
			}
		};
		
		serverOps = new ServerOperationsImpl(city, sampleDB);

		Endpoint endpoint = Endpoint.publish("http://localhost:6001/tor", serverOps);

		// wait for invocations from clients
		TORServer udpServer = new TORServer();
		udpServer.start();
		System.out.println(city + " UDP has started.");

	}

	public void run() {

		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(serverPort);
			System.out.println(city + "Server with server port " + serverPort + " ready and waiting ...");
			System.out.println(
					"Created a socket with port " + aSocket.getLocalPort() + " and host " + aSocket.getInetAddress());
			byte[] buffer = null;
			while (true) {
				buffer = new byte[1000];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String requestString = (new String(request.getData())).trim();
				String[] requestStringArr = requestString.split(" ");
				if (requestString.charAt(0) == 'A') {
					String eventType = requestStringArr[1];
					String result = serverOps.listReservationSlotAvailableLocal(eventType);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'P') {
					String participantID = requestString.substring(2);
					String result = serverOps.getEventScheduleLocal(participantID);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'C') {
					String participantID = requestStringArr[1];
					String tbaEventStr = requestStringArr[2];
					String newEventType = requestStringArr[3];
					if (requestStringArr[3].equals("Art")) {
						newEventType = requestStringArr[3] + " " + requestStringArr[4];
					} else
						newEventType = requestStringArr[3];
					Festival tbaEvent = serverOps.getEvent(tbaEventStr, newEventType);
					String result = null;
					if (tbaEvent != null) {
						boolean tbaEventContainsUser = tbaEvent.isUserBooked(participantID);
						if (tbaEventContainsUser) {
							result = "1 1";
						} else {
							result = "1 0";
						}
					} else {
						result = "0 0";
					}
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'R') {
					String participantID = requestStringArr[1];
					String tbaEventStr = requestStringArr[2];
					String newEventType = "";
					if (requestStringArr[3].equals("Art")) {
						newEventType = requestStringArr[3] + " " + requestStringArr[4];
					} else
						newEventType = requestStringArr[3];
					String result = serverOps.reserveTicket(participantID, tbaEventStr, newEventType);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				}
			}
		} catch (SocketException e) {
			System.out.println("Socket error");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}
}
