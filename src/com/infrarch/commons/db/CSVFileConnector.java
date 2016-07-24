package com.infrarch.commons.db;

import java.io.File;

/**
 * A connector capable of saving {@code DataSources} as CSV
 * text files.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class CSVFileConnector extends AbstractFileConnector {

	private final char delimiter;
	
	/**
	 * Constructs a {@code Connector} instance capable of operating
	 * CSV text files. Sets ',' as delimiter.
	 * 
	 * @param file {@code Connector}'s file
	 * @param ds the {@code DataSource} to connect
	 */
	public CSVFileConnector(File file, DataSource ds) {
		super(file, ds);
		this.delimiter = ',';
	}
	
	/**
	 * Constructs a {@code Connector} instance capable of operating
	 * CSV text files.
	 * 
	 * @param file {@code Connector}'s file
	 * @param ds the {@code DataSource} to connect
	 * @param delimiter the delimiter to use
	 */
	public CSVFileConnector(File file, DataSource ds, char delimiter) {
		super(file, ds);
		this.delimiter = delimiter;
	}

	@Override
	public synchronized boolean save() {
		return DataSourceUtils.saveCSV(getDataSource(), getFile(), delimiter);
	}
}
