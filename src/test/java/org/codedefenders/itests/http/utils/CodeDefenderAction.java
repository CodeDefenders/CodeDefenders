package org.codedefenders.itests.http.utils;

public abstract class CodeDefenderAction {

	private long delay;
	private String payload;
	
	private String userId;

	public CodeDefenderAction(String userId, String payload) {
		this.userId = userId;
		this.payload = payload;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getDelay() {
		return delay;
	}

	public String getPayload() {
		return payload;
	}
	
	public String getUserId() {
		return userId;
	}

}
