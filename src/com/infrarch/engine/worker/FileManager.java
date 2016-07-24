package com.infrarch.engine.worker;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

/**
 * The file system manager.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class FileManager implements PeriodicalTask {

	private static final Logger logger = Logger.getLogger(FileManager.class);
	private static final FileManager instance = new FileManager();
	
	private FileManager() {}
	
	/**
	 * Returns the single manager instance.
	 * 
	 * @return manager instance
	 */
	public static FileManager getInstance() {
		return instance;
	}
	
	/**
	 * Returns the document root directory. No files can be uploaded outside this 
	 * directory.
	 * 
	 * @return document root
	 */
	public File getDocRoot() {
		return new File(ConfigurationManager.getInstance().getDocRoot());
	}
	
	/**
	 * Checks if a file belongs to the system.
	 * 
	 * @param f file to check
	 * @return file is system or not
	 */
	public boolean isSystemFile(File f) {
		return false;	//f.getName().startsWith("!");
	}
	
	private long sizeOnDisk = 0;
	private volatile long lastSizeCalcTime = 0;
	private long SIZE_RECALC_PERIOD = 15*60*1000;
	private AtomicBoolean isRebuilding = new AtomicBoolean(false);
	
	/**
	 * Calculates the size on disk taken by the system. That is, the size
	 * of the document root and configuration directory (the backup root
	 * is not included).
	 * 
	 * @return the size on the disk
	 */
	public long getSizeOnDisk() {
		logger.debug("Called getSizeOnDisk()");
		if (lastSizeCalcTime == 0) calculateSize();
		return sizeOnDisk;
	}
	
	private long calculateSize() {
		if (!isRebuilding.get() || lastSizeCalcTime == 0) {		
			synchronized (this) {
				long currTime = System.currentTimeMillis();
				if (currTime - lastSizeCalcTime > SIZE_RECALC_PERIOD) {
					isRebuilding.set(true);
					long s = FileUtils.getSize(getDocRoot());
					sizeOnDisk = s;
					lastSizeCalcTime = System.currentTimeMillis();
					isRebuilding.set(false);
				}
			}
		}
		return sizeOnDisk;
	}
	
	/**
	 * A periodical task to calculate the size taken by the system on the disk.
	 */
	public void runPeriodically() {
		calculateSize();
	}
}
