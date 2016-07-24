package com.infrarch.engine.constants;

public interface Response {

	public static final String MSG_OK = "OK";
	public static final String MSG_ERROR = "Error executing command"; // general error
	public static final String MSG_NOT_AUTHENTICATED = "Client not authenticated";
	public static final String MSG_INCORRECT_LOGIN = "Incorrect user name or password";
	public static final String MSG_NO_ACCESS = "No access";
	public static final String MSG_NO_SUCH_USER = "No such user";
	public static final String MSG_NO_COMMAND = "No command";
	public static final String MSG_UNRECOGNIZED_COMMAND = "Unrecognized command";
	public static final String MSG_UNSUPPORTED_COMMAND = "Unsupported command";
	public static final String MSG_MISSING_PARAMETER = "Missing or empty parameter";
	public static final String MSG_WRONG_PARAMETER = "Wrong parameter"; 
	public static final String MSG_NO_SUCH_FILE = "No such file";
	public static final String MSG_NO_SUCH_DIR = "No such directory";
	
	public static final int CODE_OK = 0;
	public static final int CODE_ERROR = 13;
	public static final int CODE_NOT_AUTHENTICATED = 100;
	public static final int CODE_INCORRECT_LOGIN = 101;
	public static final int CODE_NO_ACCESS = 102;
	public static final int CODE_NO_SUCH_USER = 103;
	public static final int CODE_NO_COMMAND = 200;
	public static final int CODE_UNRECOGNIZED_COMMAND = 201; 
	public static final int CODE_UNSUPPORTED_COMMAND = 202;
	public static final int CODE_MISSING_PARAMETER = 210;
	public static final int CODE_WRONG_PARAMETER = 211;
	public static final int CODE_NO_SUCH_FILE = 300;
	public static final int CODE_NO_SUCH_DIR = 301;
}
