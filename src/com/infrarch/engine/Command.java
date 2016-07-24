package com.infrarch.engine;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

/**
 * A common interface defining pluggable commands for the file server 
 * {@link Engine}. 
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public interface Command {

	/**
	 * Returns a {@code String} array with the names of the supported commands.
	 * 
	 * @return a {@code String} array with the supported commands
	 */
	public String[] getSupportedCommands();
	
	/**
	 * Executes the command.
	 * 
	 * @param request client's HTTP request object
	 * @param params the parsed request's parameters represented as a {@code JsonObject}
	 * @param builder a {@code JsonObjectBuilder} for the result
	 * @throws Throwable all exceptions are given to the container to handle
	 */
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable;
}
