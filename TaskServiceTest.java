import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test; 

class TaskServiceTest {

	@Test
	void testAddTask() {
		TaskService service = new TaskService();
		Task task = new Task("123", "Task1", "Description blah bleh");
		service.add(task);
		
		assertEquals("Task1", service.getTask("123").getName());
	}
	
	@Test
	void testAddDuplicateTaskId() {
		TaskService service = new TaskService();
		Task task = new Task("123", "Task1", "Blah blah blah");
		service.add(task);

		assertThrows(IllegalArgumentException.class, () -> service.add(task));
	}
	
	@Test
	void testDeleteTask() {
		TaskService service = new TaskService();
		Task task = new Task("123", "Task1", "blah blah");
		service.add(task);
		
		service.deleteTask("123");
		assertNull(service.getTask("123"));
	}
	
	@Test
	void testDeleteNonExistentTask() {
		TaskService service = new TaskService();
		assertThrows(IllegalArgumentException.class, ()-> service.deleteTask("123"));
	}
	
	@Test
	void testUpdateTaskName() {
		TaskService service = new TaskService();
		Task task = new Task("123", "Task1", "Blahbleh");
		service.add(task);
		service.updateTaskName("123", "updated task1");
		assertEquals("updated task1", service.getTask("123").getName());
	}
	
	@Test
	void testUpdateTaskDescription() {
		TaskService service = new TaskService();
		Task task = new Task("123", "Task1", "Blahbleh");
		service.add(task);
		service.updateTaskDescription("123", "updated blah");
		assertEquals("updated blah", service.getTask("123").getDescription());
	}
	
	@Test
	void testUpdateNonExistentTask() {
		TaskService service = new TaskService();
		assertThrows(IllegalArgumentException.class, () -> service.updateTaskName("123", "New name"));
		assertThrows(IllegalArgumentException.class, () -> service.updateTaskDescription("123", "blahbleh"));
	}
	
}
