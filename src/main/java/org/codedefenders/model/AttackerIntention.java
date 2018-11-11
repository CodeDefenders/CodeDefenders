package org.codedefenders.model;

public enum AttackerIntention {
	
	EQUIVALENT, KILLABLE, DONTKNOW;

	public static AttackerIntention fromString(String parameter) {
		try{
			return AttackerIntention.valueOf(parameter);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
