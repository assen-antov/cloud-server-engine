package com.infrarch.engine;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * An abstract {@link Command}. 
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public abstract class AbstractCommand implements Command {

	protected final Logger logger = Logger.getLogger(getClass());
	
	/**
	 * @see Command#getSupportedCommands()
	 */
	public abstract String[] getSupportedCommands();
	
	/**
	 * @see Command#execute(HttpServletRequest, JsonObject, JsonObjectBuilder)
	 */
	public abstract void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable;
}
