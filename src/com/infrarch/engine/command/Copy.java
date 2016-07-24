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
 * This class supports three commands: {@code Cmd.COPY}, {@code Cmd.MOVE}. 
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Copy extends AbstractCommand {

	private static final Copy instance = new Copy();
	
	public static Command getInstance() {
		return instance;
	}
	
	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.COPY_FILE, Cmd.MOVE_FILE };
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		
		// get parameters
		String srcDir, destDir, fileName, overwriteStr = null;
		try { 
			srcDir = params.getString(Cmd.Q_SRC_DIR);
			destDir = params.getString(Cmd.Q_DEST_DIR);
			fileName = params.getString(Cmd.Q_FILE_NAME);
		} catch (NullPointerException e) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		try {
			overwriteStr = params.getString(Cmd.Q_OVERWRITE);
		} catch (NullPointerException e) {}	// optional
		if ("".equals(srcDir) || "".equals(destDir) || "".equals(fileName)) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		srcDir = EngineUtils.urlDecode(srcDir);
		destDir = EngineUtils.urlDecode(destDir);
		fileName = EngineUtils.urlDecode(fileName);
		
		// overwrite parameter
		boolean overwrite = new Boolean(overwriteStr);
		
		// check if the user has access to both source and destination directories
		HttpSession session = request.getSession();
 		String user = (String) session.getAttribute(Engine.ATTRIBUTE_USER_NAME);
		if (!Worker.checkUserAccess(user, srcDir) || !Worker.checkUserAccess(user, destDir)) {
			EngineUtils.putStatus(builder, Response.CODE_NO_ACCESS);
			return;
		}
		
		// construct a File for the source and destination directories
		File docRoot = Worker.getDocRoot();
		File f1 = new File(docRoot, srcDir);
		File f2 = new File(docRoot, destDir);
		
		// check if dirs exist
		if (!f1.exists() || !f1.isDirectory() || !f2.exists() || !f2.isDirectory()) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_DIR);
			return;
		}
		
		// check if the file exists
		File cf = new File(f1, fileName);
		if (!cf.exists()) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_FILE);
			return;
		}
		
		// Cmd.COPY_FILE
		String cmdStr = params.getString(Cmd.Q_COMMAND);
		if (Cmd.COPY_FILE.equalsIgnoreCase(cmdStr)) {
			if (Worker.copyFile(cf, f2, overwrite) == null)
				EngineUtils.putStatus(builder, Response.CODE_ERROR);
		}
		
		// Cmd.MOVE_FILE
		else if (Cmd.MOVE_FILE.equalsIgnoreCase(cmdStr)) {
			if (Worker.moveFile(cf, f2, overwrite) == null)
				EngineUtils.putStatus(builder, Response.CODE_ERROR);
		}
		
		else {
			EngineUtils.putStatus(builder, Response.CODE_UNRECOGNIZED_COMMAND);
			return;
		}
			
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
}
