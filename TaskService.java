import java.util.HashMap;
import java.util.Map;


public class TaskService {
	private final Map<String, Task> tasks = new HashMap<>();
	
	public Map<String, Task> getTask() {
		return tasks;
	}
	
	
	//add task
	public void add(Task task) {
		if (tasks.containsKey(task.getTaskId())) {
			throw new IllegalArgumentException("ID must be unique");
		}
		tasks.put(task.getTaskId(), task);
	}
	
	//yeet (delete) task
	public void deleteTask(String taskId) {
		if (!tasks.containsKey(taskId)) {
			throw new IllegalArgumentException("ID not found");
		}
		tasks.remove(taskId);
	}
	
	//update task by id
	public void updateTaskName(String taskId, String name) {
		Task task = tasks.get(taskId);
		if (task == null) {
			throw new IllegalArgumentException("ID not found");
		}
		task.setName(name);
	}
	
	//update descript
	public void updateTaskDescription(String taskId, String description) {
		Task task = tasks.get(taskId);
		if (task == null) {
			throw new IllegalArgumentException("ID not found");
		}
		task.setDescription(description);
	}
	
	//get for test
	public Task getTask(String taskId) {
		return tasks.get(taskId);
	}
}
