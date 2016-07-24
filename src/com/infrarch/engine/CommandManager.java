package com.infrarch.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * {@code Engine}'s commands are registered and operated by the 
 * {@code CommandManager}.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class CommandManager {

	private final Logger logger = Logger.getLogger(getClass());
	
	private static final CommandManager instance = new CommandManager();
	private final List<Command> commands = new LinkedList<Command>();
	
	public static CommandManager getInstance() {
		return instance;
	}
	
	/**
	 * Registers a {@link Command} to the manager.
	 * 
	 * @param	cmd {@code Command} to register
	 * @return	<code>true</code>, if newly added; <code>false</code>, if has
	 * 			already been added
	 */
	public synchronized boolean registerCommand(Command cmd) {
		if (isRegistered(cmd.getClass())) {
			logger.warn("Attempting to register command which is already registered: " + cmd.getClass());
			return false;
		}
		boolean b = commands.add(cmd);
		if (b) logger.info("Registered command: " + cmd.getClass());
		else logger.warn("Attempting to register command which is already registered: " + cmd.getClass());
		return b;
	}
	
	/**
	 * Deregisters a {@link Command} from the manager.
	 * 
	 * @param	cmd {@code Command} to deregister
	 * @return	<code>true</code>, if deregistered; <code>false</code>, if the
	 * 			command was not found
	 */
	public synchronized boolean deregisterCommand(Command cmd) {
		boolean b = commands.remove(cmd);
		if (b) logger.info("Deregistered command: " + cmd.getClass());
		else logger.warn("Attempting to deregister command which has not been registered: " + cmd.getClass());
		return b;
	}
	
	/**
	 * Queries if a {@code Command} has been registered. 
	 * 
	 * @param cl command's {@code Class}
	 * @return registered or not
	 */
	public synchronized boolean isRegistered(Class<?> cl) {
		Iterator<Command> iter = commands.iterator();
		while (iter.hasNext()) {
			if (iter.next().getClass() == cl) return true;
		}
		return false;
	}
	
	/**
	 * Returns the supporting {@code Command} for the parameter command
	 * name.
	 * 
	 * @param	cmdStr command name
	 * @return	the first supporting {@code Command} instance found or <code>null</code>, 
	 * 			if no supporting {@code Command} is registered
	 */
	public synchronized Command getSupportingCommand(String cmdStr) {
		Iterator<Command> iter = commands.iterator();
		while (iter.hasNext()) {
			Command cmd = iter.next();
			if (isSupported(cmdStr, cmd)) return cmd;
		}
		return null;
	}
	
	private boolean isSupported(String cmdStr, Command cmd) {
		String[] commands = cmd.getSupportedCommands();
		for (String c: commands) {
			if (c.equalsIgnoreCase(cmdStr)) return true;
		}
		return false;
	}
	
	/**
	 * Returns an array of all command names supported by the engine.
	 * 
	 * @return an array of all supported commands
	 */
	public synchronized String[] getSupportedCommands() {
		List<String> cmds = new LinkedList<String>();
		Iterator<Command> iter = commands.iterator();
		while (iter.hasNext()) {
			Command cmd = iter.next();
			String[] cmdStrings = cmd.getSupportedCommands();
			for (String c: cmdStrings) cmds.add(c);
		}
		return cmds.toArray(new String[0]);
	}
}
