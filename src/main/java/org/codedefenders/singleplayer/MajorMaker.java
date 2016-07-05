package org.codedefenders.singleplayer;

import org.codedefenders.AntRunner;
import org.codedefenders.DatabaseAccess;
import org.codedefenders.GameClass;

public class MajorMaker {

	private String cutTitle;
	private int cId;
	private GameClass cut;

	public MajorMaker(int classId) {
		cId = classId;
		cut = DatabaseAccess.getClassForKey("Class_ID", cId);
		cutTitle = cut.getBaseName();
	}

	public boolean createMutants() {
		AntRunner.generateMutantsFromCUT(cutTitle);



		return true;
	}

}
