package gammut;

public class User {

	public int id;
	public String name;
	public String password;

	public User(int id, String name, String password) {
		this.id = id;
		this.name = name;
		if (name == null) {name = "Empty";}
		this.password = password;
	}
}