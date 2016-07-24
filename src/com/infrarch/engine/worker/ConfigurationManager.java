package com.infrarch.engine.worker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.infrarch.engine.Engine;

/**
 * Configuration manager. The {@link #getInstance()} method should be called 
 * to initialize manager's default property values. The method will not load the
 * configuration file as at this time it does not yet know its location. A servlet should then
 * configure the configuration directory using {@link #setConfigRoot(String)} and 
 * call {@link #initialize()}. Calling {@code initialize()} will result in the 
 * manager checking whether the main directories exist, creating them, if needed,
 * and loading the configuration file from the configuration root directory.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class ConfigurationManager {

    private static final Logger logger = Logger.getLogger(ConfigurationManager.class);
    
    private static final String CONFIG_FILE = "config.properties";
    
	public static final String KEY_DOC_BASE_DEFAULT = "dir.docs";
	public static final String KEY_CONFIG_BASE = "dir.config";
	public static final String KEY_MAX_UPL_FILE = "max.upload.file";
	public static final String KEY_CHECK_USER_IP = "ip.check.user";
	public static final String KEY_CHECK_ADMIN_IP = "ip.check.admin";
	public static final String KEY_IP_WHITELIST = "ip.whitelist";
	public static final String KEY_IP_BLACKLIST = "ip.blacklist";
	public static final String KEY_INCORRECT_LOGIN_DELAY = "system.login.delay";
	
	private static ConfigurationManager instance = null;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
			
	private final Properties properties;
	

	private ConfigurationManager(Properties p) {
		properties = new Properties(p);
	}

	/**
	 * Returns an instance of the configuration manager initialized with 
	 * default property values. 
	 * 
	 * @return an instance of the configuration manager
	 */
	public static synchronized ConfigurationManager getInstance() {
		if (instance == null) {
			instance = new ConfigurationManager(generateDefaultSettings());
		}
		return instance;
	}
	
	/**
	 * Returns a {@code Properties} instance with default values.
	 */
	private static Properties generateDefaultSettings() {
		Properties p = new Properties();

		p.setProperty(KEY_DOC_BASE_DEFAULT, "/cloud-server-engine/docs");
		p.setProperty(KEY_CONFIG_BASE, "/cloud-server-engine/config");

		p.setProperty(KEY_MAX_UPL_FILE, "" + 70*1024*1024);

		p.setProperty(KEY_CHECK_USER_IP, "false");
		p.setProperty(KEY_INCORRECT_LOGIN_DELAY, "5");
	    
	    return p;
	}
	
	/**
	 * Configuration initialized or not.
	 * 
	 * @return initialized or not
	 */
	public boolean isInitialized() {
		return initialized.get(); 
	}
	
	/**
	 * Initializes the configuration. Prior to calling this method the 
	 * configuration root folder must have been set to its correct values.
	 * The method will check whether the document root directory exists and attempt 
	 * to create it, if it does not. It will also load the configuration file.
	 */
	public void initialize() {
		if (initialized.get()) return;
		logger.info("Initializing configuration...");
		
		// create document root, if does not exist
		File docRoot = new File(getDocRoot());
		if (!docRoot.isDirectory() && !docRoot.mkdirs()) {
			logger.fatal("Failed creating document root directory: " + getDocRoot());
			return;
		}
		
		// load the configuration file
		File configRoot = new File(getConfigRoot());
		File cf = new File(configRoot, CONFIG_FILE);
		logger.info("Loading settings from file: " + cf.getAbsolutePath());
		if (!load(cf)) logger.error("Failed to load configuration from file: " + cf.getAbsolutePath() + "; using default settings");
		else logger.info("Configuration initialized");

		initialized.set(true);
	}
	
	/**
	 * Loads the configuration file to {@code properties}.
	 */
	private boolean load(File cf) {
		try {
			properties.load(new FileReader(cf));
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Stores the configuration file. Should be called after
	 * changing properties.
	 */
	public void store() {
		File cf = new File(getConfigRoot(), CONFIG_FILE);
		try {
			properties.store(new FileWriter(cf), Engine.VERSION);
			logger.info("Configuration saved to file: " + cf.getAbsolutePath());
		} catch (IOException e) {
			logger.error("Could not store configuration to file: " + cf.getAbsolutePath());
		}
	}
	
	/**
	 * Returns all property names.
	 * 
	 * @return an array of property names
	 */
	public String[] getKeys() {
		Set<String> set = properties.stringPropertyNames();
		String[] keys = new String[set.size()];
		keys = set.toArray(keys);
		return keys;
	}
	
	/**
	 * Returns a configuration property.
	 * 
	 * @param key property name to lookup
	 * @return property's value
	 */
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * Returns a configuration property.
	 * 
	 * @param key property name to lookup
	 * @param def default value, if not found 
	 * @return property's value
	 */
	public String get(String key, String def) {
		String res = properties.getProperty(key);
		if (res == null) return def;
		return res;
	}
	
	/**
	 * Returns a property as integer.
	 * 
	 * @param key key to lookup
	 * @param def default value, if not found or could not be parsed
	 * @return the property value
	 */
	public int getInt(String key, int def) {
		Integer i;
		String s = properties.getProperty(key);
		if (s == null) return def;
		try {
			i = new Integer(s);
		} catch (NumberFormatException e) {
			return def;
		}
		return i;
	}
	
	/**
	 * Saves a key/value pair.
	 * 
	 * @param key key to use
	 * @param value value to store
	 */
	public void set(String key, String value) {
		properties.setProperty(key, value);
	}
	
	private String normalizePath(String path) {
		return path.endsWith("\\") || path.endsWith("/")? path : path + File.separator;
	}

	public String getDocRoot() {
		return normalizePath(get(KEY_DOC_BASE_DEFAULT));
	}
	
	public void setDocRoot(String path) {
		set(KEY_DOC_BASE_DEFAULT, normalizePath(path));
	}
	
	public String getConfigRoot() {
		return normalizePath(get(KEY_CONFIG_BASE));
	}
	
	public void setConfigRoot(String path) {
		set(KEY_CONFIG_BASE, normalizePath(path));
	}
	
	public int getMaxUploadSize() {
		return getInt(KEY_MAX_UPL_FILE, 70*1024*1024);
	}
	
	public void setMaxUploadSize(int size) {
		set(KEY_MAX_UPL_FILE, "" + size);
	}

	public void setWhitelist(String s) {
		set(KEY_IP_WHITELIST, s);
	}
	
	public String[] getWhitelist() {
		String ips = get(KEY_IP_WHITELIST);
		return ips == null? null : ips.split(",");
	}
	
	public boolean isWhitelisted(String ip) {
		String[] ips = getWhitelist();
		if (ips == null) return false;
		if (ip.equalsIgnoreCase("localhost")) return true;
		for (String s : ips) {
			if (s.equals(ip)) return true;
		}
		return false;
	}
	
	public boolean addToWhitelist(String ip) {
		if (!isIP(ip)) return false;
		
		String ips = get(KEY_IP_WHITELIST);
		if (ips == null || ips.equals("")) set(KEY_IP_WHITELIST, ip);
		else set(KEY_IP_WHITELIST, ips + "," + ip);
		
		return true;
	}
	
	public boolean removeFromWhitelist(String ip) {
		String[] ips = get(KEY_IP_WHITELIST).split(",");
		String result = "";
		boolean b = false;
		for (int i = 0; i < ips.length; i++) {
			String s = ips[i];
			if (s.equals(ip)) {
				b = true;
				continue;
			}
			if (i != 0) result += ",";
			result += s;
		}
		set(KEY_IP_WHITELIST, result);
		return b;
	}
	
	private boolean isIP(String ip) {
		return ip.matches("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
	}
	
	/**
	 * Saves the properties instance at server shutdown (for just in case).
	 */
	public static void destroyInstance() {
		if (instance != null) {
			Logger.getLogger(ConfigurationManager.class).debug("1.1");
			instance.store();
		}
	}
}
