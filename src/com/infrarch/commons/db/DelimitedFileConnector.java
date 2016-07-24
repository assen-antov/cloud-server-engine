package com.infrarch.commons.db;

import java.io.File;

/**
 * A connector capable of saving {@code DataSources} as delimited
 * text files.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class DelimitedFileConnector extends AbstractFileConnector {

	/**
	 * Constructs a {@code Connector} instance capable of operating
	 * delimited text files.
	 * 
	 * @param file {@code Connector}'s file
	 * @param ds the {@code DataSource} to connect
	 */
	public DelimitedFileConnector(File file, DataSource ds) {
		super(file, ds);
	}

	@Override
	public synchronized boolean save() {
		return DataSourceUtils.saveDelimited(getDataSource(), getFile());
	}
}
