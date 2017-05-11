package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

/**
 * Created by jmr on 13/07/2016.
 */
public abstract class AbstractGame {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractGame.class);
	protected int id;
	protected int classId;
	protected int creatorId;
	protected GameState state;
	protected GameLevel level;
	protected GameMode mode;

	protected int levelNum;
	protected int puzzleId;
	protected int levelId;
	protected int puzzle;
	protected int points;
	protected String puzzleName;
	protected String storyName;
	protected String alias;
	protected String hint;
	protected String desc;
	protected boolean canModify;
	protected StoryState storyState;
	protected PuzzleMode storyMode;

	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return DatabaseAccess.getClassForKey("Class_ID", classId).getName();
	}

	public GameClass getCUT() {
		return DatabaseAccess.getClassForKey("Class_ID", classId);
	}

	public StoryClass getPCUT() { return DatabaseAccess.getStoryForKey(classId); }

	public StoryClass getPTest() { return DatabaseAccess.getTestForPuzzleId(puzzleId); }

	public StoryClass getUserPuzzleTest() { return DatabaseAccess.getUserPuzzleTest(puzzleId, id); }

	public StoryClass getUserPuzzleMutant() { return DatabaseAccess.getUserPuzzleMutant(puzzleId, id); }

	public int getCreatorId() {
		return creatorId;
	}

	public GameState getState() {
		return state;
	}

	public void setState(GameState s) {
		state = s;
	}

	public GameLevel getLevel() {
		return this.level;
	}

	public void setLevel(GameLevel level) {
		this.level = level;
	}

	public GameMode getMode() {
		return this.mode;
	}

	protected void setMode(GameMode newMode) { this.mode = newMode; }

	//story
	public int getLevelNum() { return levelNum; }

	public int getPuzzleId() { return puzzleId; }

	public int getLevelId() { return levelId; }

	public int getPuzzle() { return puzzle; }

	public int getPoints() { return points; }

	public void setPoints(int p) { this.points = p; }

	public String getPuzzleName() { return puzzleName; }

	public StoryState getStoryState() { return this.storyState; }

	public void setStoryState(StoryState s) { this.storyState = s; }

	public PuzzleMode getStoryMode() { return this.storyMode; }

	protected void setStoryMode(PuzzleMode newSMode) { this.storyMode = newSMode; }

	public String getStoryName() { return storyName; }

	public String getAlias() { return alias; }

	public String getHint() { return hint; }

	public String getDesc() { return desc; }

	//end

	public List<Test> getTests() {
		return getTests(false);
	}

	public List<Test> getTests(boolean defendersOnly) {
		return DatabaseAccess.getExecutableTests(this.id, defendersOnly);
	}

	public List<Mutant> getMutants() {
		return DatabaseAccess.getMutantsForGame(id);
	}

	public List<Mutant> getAliveMutants() {
		return getMutants().stream().filter(mutant -> mutant.isAlive() &&
				mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO) &&
				mutant.getClassFile() != null).collect(Collectors.toList());
	}

	public List<Mutant> getKilledMutants() {
		return getMutants().stream().filter(mutant -> !mutant.isAlive() &&
				(mutant.getEquivalent().equals(ASSUMED_NO) || mutant.getEquivalent().equals(PROVEN_NO)) &&
				(mutant.getClassFile() != null)).collect(Collectors.toList());
	}

	public Mutant getMutantByID(int mutantID) {
		for (Mutant m : getMutants()) {
			if (m.getId() == mutantID)
				return m;
		}
		return null;
	}

	public List<PuzzleTest> getPTests() {

		return getPTests(false);

	}

	public List<PuzzleTest> getPTests(boolean defendersOnly) {

		return DatabaseAccess.getExecutablePTests(this.puzzleId,defendersOnly);

	}

	public List<PuzzleMutant> getPMutants() { return DatabaseAccess.getMutantsforPuzzle(puzzleId); }

	public List<PuzzleMutant> getAlivePMutants() {

		return getPMutants().stream().filter(puzzleMutant -> puzzleMutant.isAlive() &&
				puzzleMutant.getEquivalent().equals(PuzzleMutant.Equivalence.ASSUMED_NO) &&
				puzzleMutant.getClassFile() != null).collect(Collectors.toList());

	}

	public List<PuzzleMutant> getKilledPMutants() {

		return getPMutants().stream().filter(puzzleMutant -> !puzzleMutant.isAlive() &&
				(puzzleMutant.getEquivalent().equals(ASSUMED_NO) || puzzleMutant.getEquivalent().equals(PROVEN_NO)) &&
				(puzzleMutant.getClassFile() != null)).collect(Collectors.toList());
	}

	public PuzzleMutant getPMutantByID(int mid) {

		for (PuzzleMutant m : getPMutants()) {
			if (m.getMutantId() == mid)
				return m;
		}

		return null;

	}

	public abstract boolean addPlayer(int userId, Role role);

	public Role getRole(int userId){
		return DatabaseAccess.getRole(userId, getId());
	}

	public abstract boolean insert();

	public abstract boolean update();


}
