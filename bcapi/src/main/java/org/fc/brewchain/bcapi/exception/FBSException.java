package org.fc.brewchain.bcapi.exception;

import lombok.Data;

@Data
public class FBSException extends RuntimeException {

	String ret_code;
	String ret_message;

	public FBSException() {
		super();
	}

	public FBSException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FBSException(String message, Throwable cause) {
		super(message, cause);
	}

	public FBSException(String message) {
		super(message);
	}

	public FBSException(String ret_code, String ret_message, Throwable cause) {
		super(cause);
		this.ret_code = ret_code;
		this.ret_message = ret_message;
	}

	public FBSException(String ret_code, String ret_message) {
		super();
		this.ret_code = ret_code;
		this.ret_message = ret_message;
	}

}
