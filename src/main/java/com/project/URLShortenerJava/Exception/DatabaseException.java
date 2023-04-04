package com.project.URLShortenerJava.Exception;

public class DatabaseException extends RuntimeException {
	private String message;

	public DatabaseException(String message) {
		super();
		this.message = message;
	}

	public DatabaseException() {
		super();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
