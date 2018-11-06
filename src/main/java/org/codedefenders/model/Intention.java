package org.codedefenders.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Intention {

	private Set<Integer> lines = new HashSet<>();
	private Set<Integer> mutants = new HashSet<>();
	
	public Intention(Set<Integer> lines, Set<Integer> mutants) {
		this.lines = lines;
		this.mutants = mutants;
	}
	
	public Set<Integer> getLines() {
		return lines;
	}
	
	public Set<Integer> getMutants() {
		return mutants;
	}
	
	public static Set<Integer> parseIntentionFromCommaSeparatedValueString(String csvString ){
		List<String> numbers = Arrays.asList(csvString.split(","));
		Set<Integer> parsed = new HashSet<>();
		for (String number : numbers) {
			if( "".equals(number) || (number != null && number.trim().equals("") ) ){
				continue;
			}
			parsed.add(Integer.valueOf(number));
		}
		return parsed;
	}
	
	@Override
	public String toString() {
		return "Intention :" + "\n"
				+ "\tLines: " + lines + "\n"
				+ "\tMutants:" + mutants; 
	}
}
