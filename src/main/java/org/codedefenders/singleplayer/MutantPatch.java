package org.codedefenders.singleplayer;

class MutantPatch {
	private int line;
	private String original;
	private String replacement;

	/**
	 * A datatype to represent key mutant information
	 * @param lineNumber The linenumber which the mutant is applied to
	 * @param beforeAfter Sub-strings of the mutated line, before and after mutation.
	 */
	public MutantPatch(int lineNumber, String[] beforeAfter) {
		line = lineNumber;
		original = beforeAfter[0];
		replacement = beforeAfter[1];
		if (replacement.matches("<NO-OP>")) {
			replacement = "";
			original += ";";
		}
	}

	protected int getLineNum() {
		return line;
	}
	protected String getOriginal() {
		return original;
	}
	protected String getReplacement() {
		return replacement;
	}

}