package gammut;

public class GameClass {
	
	public int id;
	public String name;
	public String javaFile;
	public String classFile;

	public GameClass(int id, String name, String jFile, String cFile) {
		this.id = id;
		this.name = name;
		this.javaFile = jFile;
		this.classFile = cFile;
	}
}