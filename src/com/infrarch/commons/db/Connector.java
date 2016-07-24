package com.infrarch.commons.db;

/**
 * {@code Connector}s are used to allow {@code DataSource}s to
 * be saved to different protocols and formats. The {@link #save()} method
 * is expected to be used by {@link DataSource#flush()} when needed.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public interface Connector {

	/**
	 * Saves the {@code DataSource} associated with this {@code Connector}. 
	 * 
	 * @return success or failure
	 */
	public boolean save();
}
