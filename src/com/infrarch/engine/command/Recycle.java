package com.infrarch.engine.command;

import java.io.File;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.Engine;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Response;

/**
 * This class supports commands {@code Cmd.RECYCLE_FILE} and 
 * {@code Cmd.RECYCLE_DIRECTORY}. 
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Recycle extends AbstractCommand {

	private static final Recycle instance = new Recycle();
	
	public static Command getInstance() {
		return instance;
	}

	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.RECYCLE_FILE, Cmd.RECYCLE_DIRECTORY };
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		
		// check the dir parameter
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
		File fDir = new File(docRoot, dir);
		if (!fDir.exists() || !fDir.isDirectory()) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_DIR);
			return;
		}
		
		// Cmd.RECYCLE_DIRECTORY
		String cmdStr = params.getString(Cmd.Q_COMMAND);
		if (Cmd.RECYCLE_DIRECTORY.equalsIgnoreCase(cmdStr)) {
			boolean b = Worker.recycleDir(fDir);
			if (b) EngineUtils.putStatus(builder, Response.CODE_OK);
			else EngineUtils.putStatus(builder, Response.CODE_ERROR);
			return;
		}
		
		// check file_name parameter
		String fileName;
		try {
			fileName = params.getString(Cmd.Q_FILE_NAME);
		} catch (NullPointerException e) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		if ("".equals(fileName)) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		
		// Cmd.RECYCLE_FILE
		if (Cmd.RECYCLE_FILE.equalsIgnoreCase(cmdStr)) {
			File f = new File(fDir, fileName);
			if (!f.exists()) {
				EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_FILE);
				return;
			}

			if (!Worker.recycleFile(f)) {
				EngineUtils.putStatus(builder, Response.CODE_ERROR);
				return;
			}
		}
		
		// unknown command
		else {
			EngineUtils.putStatus(builder, Response.CODE_UNRECOGNIZED_COMMAND);
			return;
		}
			
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}	
}
