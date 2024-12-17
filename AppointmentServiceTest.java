import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

class AppointmentServiceTest {
    //create future date
    private Date createFutureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(calendar.DATE, 1);
        return calendar.getTime();
    }

    @Test
    void testAddAppointment() {
        AppointmentService service = new AppointmentService();
        Date futureDate = createFutureDate();
        Appointment appointment = new Appointment("12345", futureDate, "Doc App");

        service.addAppointment(appointment);
        assertEquals(appointment, service.getAppointment("12345"));
    }

    @Test
    void testAddAppointmentWithDuplicateID() {
        AppointmentService service = new AppointmentService();
        Date futureDate = createFutureDate();
        Appointment appointment1 = new Appointment("12345", futureDate, "Doc App");
        Appointment appointment2 = new Appointment("12345", futureDate, "Walk the fish");

        service.addAppointment(appointment1);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.addAppointment(appointment2);
        });

        assertEquals("Appointment already exists", exception.getMessage());
    }

    @Test
    void testDeleteAppointment() {
        AppointmentService service = new AppointmentService();
        Date futureDate = createFutureDate();
        Appointment appointment = new Appointment("12345", futureDate, "Doc App");

        service.addAppointment(appointment);
        service.deleteAppointment("12345");
        assertNull(service.getAppointment("12345"));
    }

    @Test
    void testDeleteAppointmentNonExistentAppointment() {
        AppointmentService service = new AppointmentService();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.deleteAppointment("12345");
        });

        assertEquals("Appointment does not exist", exception.getMessage());
    }
}
