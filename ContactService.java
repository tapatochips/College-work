//this class is for storing all the contacts and managing them

import java.util.HashMap;
import java.util.Map;


public class ContactService {
	
	private Map<String, Contact> contacts = new HashMap<>();
	
	public Map<String, Contact> getContacts() {
		return contacts;
	}
	
	//add contact, check id
	public void addContact(Contact contact) {
		if (contacts.containsKey(contact.getContactId())) {
			throw new IllegalArgumentException("Contact already exists");
		}
		contacts.put(contact.getContactId(), contact);
	}
	
	//delete contact by their ID
	public void deleteContact(String contactId) {
		if (!contacts.containsKey(contactId)) {
			throw new IllegalArgumentException("Contact not found");
		}
		contacts.remove(contactId);
	}
	
	//update contact
	public void updateContact(String contactId, String firstName, String lastName, String phone, String address) {
		Contact contact = contacts.get(contactId);
		if (contact == null) {
			throw new IllegalArgumentException("Contact not found");
		}
		if (firstName != null) contact.setFirstName(firstName);
		if (lastName != null) contact.setLastName(lastName);
		if (phone != null) contact.setPhone(phone);
		if (address != null) contact.setAddress(address);
	}
	
}
