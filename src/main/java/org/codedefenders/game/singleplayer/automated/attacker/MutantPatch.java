/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game.singleplayer.automated.attacker;

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