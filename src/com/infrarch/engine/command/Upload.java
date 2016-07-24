package com.infrarch.engine.command;

import java.io.File;
import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.infrarch.engine.worker.ExtendedMultipartRequest;
import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.Engine;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Response;

/**
 * {@code Cmd.UPLOAD}
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Upload extends AbstractCommand {

	private static final Upload instance = new Upload();
	
	public static Command getInstance() {
		return instance;
	}
	
	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.UPLOAD };
				
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		
		// get the dir parameter
		String dir;
		try {
			dir = params.getString(Cmd.Q_DIR);
		} catch (NullPointerException e) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		if ("".equals(dir)) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		dir = EngineUtils.urlDecode(dir);
		
		// check if the user has access to this directory
		HttpSession session = request.getSession();
		String user = (String) session.getAttribute(Engine.ATTRIBUTE_USER_NAME);
		if (!Worker.checkUserAccess(user, dir)) {
			EngineUtils.putStatus(builder, Response.CODE_NO_ACCESS);
			return;
		}

		// construct a File for the directory
		File docRoot = Worker.getDocRoot();
		File f = new File(docRoot, dir);
		if (!f.exists() || !f.isDirectory()) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_DIR);
			return;
		}
		
		// upload
		@SuppressWarnings("unused")
		ExtendedMultipartRequest multi = null;
		try {
			multi = Worker.getMultipartRequest(request, f.getAbsolutePath(), false);
		} catch (IOException e) {
			logger.error("Error processing multipart request", e);
			EngineUtils.putStatus(builder, Response.CODE_ERROR);
			return;
		}
		
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
}
