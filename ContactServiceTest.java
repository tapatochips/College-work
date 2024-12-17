import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class ContactServiceTest {
	private ContactService service;

	@BeforeEach
	void setUp() {
		service = new ContactService();
	}
	
	@Test
	void testAddContact() {
		Contact contact = new Contact("123456789", "John", "Doe", "1234567890", "123 Potato St");
		service.addContact(contact);
		assertEquals(contact, service.getContacts().get("123456789"));
	}
	
	@Test
	void testDeleteContact() {
		Contact contact = new Contact("123456789", "John", "Doe", "1234567890", "123 Potato St");
		service.addContact(contact);
		service.deleteContact("123456789");
		assertFalse(service.getContacts().containsKey("1234567890"));
	}
	
	@Test
	void testUpdateContact() {
		Contact contact = new Contact("123456789", "John", "Doe", "1234567890", "123 Potato St");
		service.addContact(contact);
		service.updateContact("123456789", "Jane", "Smith", "0987654321", "321 Potato St");
		
		assertEquals("Jane", contact.getFirstName());
		assertEquals("Smith", contact.getLastName());
		assertEquals("0987654321", contact.getPhone());
		assertEquals("321 Potato St", contact.getAddress());
	}
}
