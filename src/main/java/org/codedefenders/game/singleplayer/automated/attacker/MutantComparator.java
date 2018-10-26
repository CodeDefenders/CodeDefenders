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

import org.codedefenders.game.Mutant;

import java.util.Comparator;

/**
 * @author Ben Clegg
 */
public class MutantComparator implements Comparator<Mutant> {
	@Override
	public int compare(Mutant m1, Mutant m2) {
		return m1.getTimesKilledAi() - m2.getTimesKilledAi();
	}
}
