package serverside;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style=Style.RPC)
public interface ServerOperationsInterface 
{
  String addReservationSlot (String eventID, String eventType, int capacity);
  String removeReservationSlot (String eventID, String eventType);
  String listReservationSlotAvailable (String eventType);
  String reserveTicket (String participantID, String eventID, String eventType);
  String getEventSchedule (String participantID);
  String cancelTicket (String participantID, String eventID);
  String exchangeTickets (String participantID, String eventID, String newEventID, String newEventType);
  String showOptions (boolean isUserAdmin);
  boolean isAdmin (String userID);
  boolean checkCity (String eventID);
  String listReservationSlotAvailableLocal (String eventType);
  String getEventScheduleLocal (String participantID);
  Festival getEvent(String eventID, String eventType);
} 
