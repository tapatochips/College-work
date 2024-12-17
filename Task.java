import java.util.Objects;


public class Task {
	private final String taskId;
	private String name;
	private String description;
	
	//construct
	public Task(String taskId, String name, String description) {
		if (taskId == null || taskId.length() > 10) {
			throw new IllegalArgumentException("ID cant be null or longer than 10.");
		}
		if (name == null || name.length() > 20) {
			throw new IllegalArgumentException("Name cant be null or longer than 10.");
		}
		if (description == null || description.length() > 50) {
			throw new IllegalArgumentException("Description cant be null or longer than 50.");
		}
		
		this.taskId = taskId;
		this.name = name;
		this.description = description;
	}
	
	//getters
	public String getTaskId() {
		return taskId;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	//setters
	public void setName(String name) {
		if (name == null || name.length() > 20) {
			throw new IllegalArgumentException("Name cant be null or longer than 20");
		}
		this.name = name;
	}
	public void setDescription(String description) {
		if (description == null || description.length() > 50) {
			throw new IllegalArgumentException("Description cant be null or longer than 50");
		}
		this.description = description;
	}
}
