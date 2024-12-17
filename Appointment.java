import java.util.Date;

public class Appointment {
    private final String appointmentID;
    private final Date appointmentDate;
    private final String description;

    //construct
    public Appointment(String appointmentID, Date appointmentDate, String description) {
        if (appointmentID == null || appointmentID.length() > 10 ) {
            throw new IllegalArgumentException("appointment ID must be less than 10");
        }
        if (appointmentDate == null || appointmentDate.before(new Date())) {
            throw new IllegalArgumentException("appointment date cant be in the past");
        }
        if (description == null || description.length() > 50 ) {
            throw new IllegalArgumentException("description must be less than 50");
        }

        this.appointmentID = appointmentID;
        this.appointmentDate = appointmentDate;
        this.description = description;
    }

    //get
    public String getAppointmentID() {
        return appointmentID;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public String getDescription() {
        return description;
    }
}