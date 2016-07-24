package com.infrarch.engine.command;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Response;

/**
 * The ping command is used to check whether the cloud server engine is 
 * still available and to keep the session alive.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Ping extends AbstractCommand {

	private static final Ping instance = new Ping();
	
	public static Command getInstance() {
		return instance;
	}

	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.PING };
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
}
