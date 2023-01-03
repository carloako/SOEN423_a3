package serverside;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class Festival {
	private int capacity;
	private int booked;
	private LinkedList<String> bookings;
	private String host;
	
	public Festival() {
		this.capacity = 0;
		this.booked = 0;
		this.bookings = new LinkedList<String>();
		this.host = null;
	}
	
	public Festival(int capacity) {
		this.capacity = capacity;
		this.booked = 0;
		this.bookings = new LinkedList<String>();
		this.host = null;
	}
	
	public Festival(int capacity, String host) {
		this.capacity = capacity;
		this.booked = 0;
		this.bookings = new LinkedList<String>();
		this.host = host;
	}
	
	public Festival(int capacity, int booked, LinkedList<String> bookings, String host) {
		this.capacity = capacity;
		this.booked = booked;
		this.bookings = new LinkedList<String>(bookings);
		this.host = host;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getBooked() {
		return booked;
	}

	public void setBooked(int booked) {
		this.booked = booked;
	}
	
	public boolean addBookings(String participantID) {
		boolean addAllowed = !bookings.contains(participantID);
		if (addAllowed) {
			booked++;
			bookings.add(participantID);
		}
		return addAllowed;
	}
	
	public boolean removeBookings(String participantID) {
		boolean removeSuccess = bookings.remove(participantID);
		if (removeSuccess)
			booked--;
		return removeSuccess;
	}

	public LinkedList<String> getBookings() {
		return new LinkedList<String>(bookings);
	}

	public void setBookings(LinkedList<String> bookings) {
		this.bookings = new LinkedList<String>(bookings);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
	public boolean isFull() {
		return booked == capacity;
	}
	
	public boolean isUserBooked(String userID) {
		Iterator it = bookings.iterator();
		while(it.hasNext()) {
			String temp = (String) it.next();
			if(temp.equals(userID)) {
				return true;
			}
		}
		return false;
	}
}
