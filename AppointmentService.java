import java.util.HashMap;
import java.util.Map;

public class AppointmentService {
    private final Map<String, Appointment> appointments = new HashMap<>();

    public Map<String, Appointment> getAppointments() {
        return appointments;
    }


    //add new app
    public void addAppointment(Appointment appointment) {
        if (appointments.containsKey(appointment.getAppointmentID())) {
            throw new IllegalArgumentException("Appointment already exists");
        }
        appointments.put(appointment.getAppointmentID(), appointment);
    }

    //del app
    public void deleteAppointment(String appointmentID) {
        if (!appointments.containsKey(appointmentID)) {
            throw new IllegalArgumentException("Appointment does not exist");
        }
        appointments.remove(appointmentID);
    }

    //get for test
    public Appointment getAppointment(String appointmentID) {
        return appointments.get(appointmentID);
    }
}
