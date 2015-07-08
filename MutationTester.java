import java.io.*;
import java.util.ArrayList;

public class MutationTester {

	private String className;

	public MutationTester(String className) {
		this.className = className;
	}

	public String runMutationTests(ArrayList<Test> tests, ArrayList<Mutant> mutants) {

		Process mutationTest = null;
		String output = "";

		for (Mutant m : mutants) {
			for (Test t : tests) {
				ProcessBuilder pb = new ProcessBuilder("C:\\apache-ant-1.9.5\\bin\\ant.bat",
						"test-mutant",
						"-Dmutant.file="+m.getFolder(),
						"-Dtest.file="+t.getFolder(),
						"-Dclassname="+className);
                pb.directory(new File("C:\\apache-tomcat-7.0.62\\webapps\\gammut\\WEB-INF"));

				try {
				    Process p = pb.start();
				    String line;
	        		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	        		while((line = es.readLine()) != null) {
	        			output = output + line; 
	        			if (m.isAlive()) {
	        				t.scorePoints(1);
	        				m.setAlive(false);
	        			}
	        		}
				} catch (Exception ex) {output += ex.toString();}
			}
			if (m.isAlive()) {m.scorePoints(1);}
		}
		return output;
	}

	public boolean checkTest(Test t) {
		String output = "";
		ProcessBuilder pb = new ProcessBuilder("C:\\apache-ant-1.9.5\\bin\\ant.bat",
					"test-mutant",
					"-Dmutant.file=resources",
					"-Dtest.file="+t.getFolder(),
					"-Dclassname="+className);
            pb.directory(new File("C:\\apache-tomcat-7.0.62\\webapps\\gammut\\WEB-INF"));

			try {
			    Process p = pb.start();
			    String line;
        		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        		while((line = es.readLine()) != null) {output = output + line;}
        		
			} catch (Exception ex) {output += ex.toString();}
		if (output.equals("")) {return true;}
		else {return false;}
	}
}