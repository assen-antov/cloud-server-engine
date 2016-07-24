package com.infrarch.commons.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Utility methods to work with {@code DataSource}s.
 * 
 * @author Ivan Peikov, Assen Antov
 * @version 2.0, 05/2016
 */
public class DataSourceUtils {
	
	private static Logger logger = Logger.getLogger(DataSourceUtils.class);
	
	/**
	 * Creates an empty {@code DefaultDataSource} and saves it to a binary file.
	 * 
	 * @param file the {@code File} to save to
	 * @param fields field names
	 * @param types data types
	 * @return an empty {@code DefaultDataSource}
	 */
	public static DefaultDataSource createDefaultDataSource(File file, String[] fields, Class<?>[] types) {
		DefaultDataSource ds = new DefaultDataSource(file.getName(), fields, types);
		saveBinary(ds, file);
		return ds;
	}
	
	/**
	 * Loads a {@code DataSource} from a file.
	 * 
	 * @param file the {@code File} to load from
	 * @return a {@code DefaultDataSource} or {@code null}
	 */
	public static DataSource loadBinary(File file) {
		DataSource ds = null;
		if (!file.exists() || !file.isFile() || !file.canRead()) return null;
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			ds = (DataSource) in.readObject();
			in.close();
		} catch (Exception e) {
			logger.error("Failed to load data source file: " + file.getAbsolutePath(), e);
			return null;
		}
		return ds;
	}
	
	/**
	 * Saves a {@code DataSource} to a binary file.
	 * 
	 * @param ds the {@code DataSource} to save
	 * @param file the {@code File} to save to
	 * @return true if success
	 */
	public static boolean saveBinary(DataSource ds, File file) {
		try {
			File p = file.getParentFile();
			if (p != null) p.mkdirs();
			ObjectOutputStream outstr = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			outstr.writeObject(ds);
			outstr.close();
		} catch (Exception e) {
			logger.error("Failed to save data source file: " + file.getAbsolutePath(), e);
			return false;
		}
		return true;
	}
	
	/**
	 * Loads a {@code DataSource} from a delimited text file.
	 * 
	 * @param file the {@code File} to load from
	 * @return a {@code DefaultDataSource} or {@code null}
	 */
	public static DataSource loadDelimited(File file) {
		if (file == null) {
			logger.error("File not set");
			return null;
		}
		
		LineNumberReader r = null;
		try {
			r = new LineNumberReader(new FileReader(file));
		} catch (FileNotFoundException fnfe) {
			logger.error("File not found: " + file.getAbsolutePath());
			return null;
		}
		
		// read field names
		List<String> tokens = DataSourceUtils.readLine(r);
		if (tokens == null) {
			logger.error("Unexpected end of file at line: " + r.getLineNumber());
			return null;
		}
		
		String[] fields = new String[tokens.size()];
		Iterator<String> iter = tokens.iterator();
		int i = 0;
		while (iter.hasNext()) {
			fields[i++] = ((String) iter.next()).toUpperCase();
		}
		
		// read types
		tokens = readLine(r);
		if (tokens == null) {
			logger.error("Unexpected end of file at line: " + r.getLineNumber());
		}
		Class<?>[] types = new Class[tokens.size()];
		iter = tokens.iterator();
		i = 0;
		while (iter.hasNext()) {
			String type = (String) iter.next();
			if (type.equals("int")) {
				types[i++] = Integer.class;
			}
			else if (type.equals("long")) {
				types[i++] = Long.class;
			}
			else if (type.equals("string")) {
				types[i++] = String.class;
			}
			else if (type.equals("boolean")) {
				types[i++] = Boolean.class;
			}
			else if (type.equals("double")) {
				types[i++] = Double.class;
			}
			else {
				logger.error("Unrecognised type: " + type);
			}
		}
		
		DataSource ds = new DefaultDataSource(file.getName(), fields, types);
		
		// read rows
		int nrow = 1;
		while ((tokens = readLine(r)) != null) {
			Object[] data = new Object[tokens.size()];
			iter = tokens.iterator();
			i = 0;
			while (iter.hasNext()) {
				String val = (String) iter.next();
				try {
					if (types[i] == String.class) {
						data[i++] = val;
					}
					else if (types[i] == Integer.class) {
						data[i++] = new Integer(Integer.parseInt(val));
					}
					else if (types[i] == Double.class) {
						data[i++] = new Double(Double.parseDouble(val));
					}
					else if (types[i] == Long.class) {
						data[i++] = new Long(Long.parseLong(val));
					}
					else if (types[i] == Boolean.class) {
						data[i++] = new Boolean(Boolean.parseBoolean(val));
					}
				} catch (NumberFormatException nfe) {
					logger.error("Number format error: " + val + ", row " + nrow + ", pos " + i);
				}
			}
			ds.append(data);
			nrow++;
		}
		return ds;
	}
	
	private static List<String> readLine(LineNumberReader r) {
		String line = null;
		try {
			// read and skip comments
			do {
				line = r.readLine();
			} while (line != null && (line.startsWith("#") || line.startsWith("/")));
		} catch (IOException ioe) {
			logger.error("I/O error");
			throw new IllegalStateException("I/O error", ioe);
		}
		
		// return null on EOF
		if (line == null) return null;

		List<String> result = new ArrayList<String>();
		@SuppressWarnings("unused")
		int tokens = 0;
		String token = "";
		int state = START;
		String delimiters = " \t";
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);

			// skip delimiters
			if (delimiters.indexOf(ch) != -1) {
				if (state == START || state == DELIMITER) {
					continue;
				}
				else if (state == STRING_START) {
					token += ch;
					continue;
				}
				else {
					state = DELIMITER;
					result.add(token);
					tokens++;
					token = "";
					continue;
				}
			}
			
			// read string token
			else if (ch == '"'){
				if (state == STRING_START) {
					state = STRING_END;
					continue;
				}
				else if (state == STRING_END || state == TOKEN) {
					state = STRING_START;
					result.add(token);
					tokens++;
					token = "";
					continue;
				}
				else {
					state = STRING_START;
					continue;
				}
			}
			
			// other tokens 
			else {
				if (state == DELIMITER || state == START) {
					state = TOKEN;
					token += ch;
					continue;
				}
				else if (state == STRING_END) {
					state = TOKEN;
					result.add(token);
					tokens++;
					token = "" + ch;
					continue;
				}
				else if (state == TOKEN || state == STRING_START){
					token += ch;
					continue;
				}
				else {
					logger.error("Line " + r.getLineNumber() + ": " + token + " " + state + " " + i);
				}
			}
			
		}
		
		// save last token read
		if (state != DELIMITER && state != START) result.add(token);
		
		if (state == STRING_START) {
			logger.error("Line " + r.getLineNumber() + ": unclosed string " + token);
		}
		
		return result;
	}
	
	private static int START = 0;
	private static int DELIMITER = 1;
	private static int TOKEN = 2;
	private static int STRING_START = 3;
	private static int STRING_END = 4;

	/**
	 * Saves a delimited text file to a data source. The first line of the file
	 * should contain the names of its fields. If a field name contains space it
	 * should be enclosed in ". The second line should describe field types.
	 * Acceptable type names are: "int", "long", "double", "string", "boolean".
	 * Strings, containing delimiters, must be enclosed in ". The " symbol acts
	 * as a delimiter as well (it is not necessary a string to be preceded or
	 * follower by another delimiter). If a line starts with # or / it will be
	 * ignored as a comment. By default valid delimiters are space and \t.
	 * Numbers must be in a format suitable for parsing by the parser methods of
	 * their respective wrapper types (Integer, Double, etc.). Number format or 
	 * decimal separator will be the locale defaults.
	 * 
	 * @param ds the {@code DataSource} to save
	 * @param file the {@code File} to save to
	 * @return true if success
	 */
	public static boolean saveDelimited(DataSource ds, File file) {
		PrintWriter out = preparePrintWriter(file);
		if (out == null) return false;
		DataSourceUtils.dumpDelimited(ds, out);
		out.close();
		return true;
	}

	/**
	 * Dumps the contents of the {@code DataSource} in tab delimited form. The
	 * dumped data can be read by {@link #loadDelimited(File)}.
	 * 
	 * @param ds the {@code DataSource} to dump
	 * @param out a {@code PrintWriter} to write to
	 */
	public static void dumpDelimited(DataSource ds, PrintWriter out) {
		Iterator<Row> iter;
		Class<?>[] types;
		
		out.println("# DataSource.name: " + ds.getName());
		out.println("# DataSource.class: " + ds.getClass().getName());
		out.println("# Date: " + new Date());
		
		String[] fields = ds.getFields();
		types = ds.getTypes();
		
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].indexOf(' ') != -1) out.print("\"" + fields[i] + "\"\t");
			out.print(fields[i] + "\t");
		}
		out.println();
		
		for (int i = 0; i < types.length; i++) {
			if (types[i] == String.class) {
				out.print("string");
			}
			else if (types[i] == Integer.class) {
				out.print("int");
			}
			else if (types[i] == Double.class) {
				out.print("double");
			}
			else if (types[i] == Long.class) {
				out.print("long");
			}
			else if (types[i] == Boolean.class) {
				out.print("boolean");
			}
			else {
				out.print(types[i].getName());
			}
			out.print("\t");
		}
		out.println();
		iter = ds.getAll();

		while (iter.hasNext()) {
			Row row = iter.next();
			Object[] data = row.getData();
			
			for (int i = 0; i < data.length; i++) {
				if (types[i] == String.class) {
					out.print("\"" + data[i] + "\"\t");
				}
				else {
					out.print(data[i] + "\t");
				}
			}
			out.println();
		}
	}

	/**
	 * Saves a CSV text file to a data source. The first line of the file
	 * should contain the names of its fields. Strings are sought for the 
	 * delimiter and it is removed from them. Numbers must be in a format 
	 * suitable for parsing by the parser methods of their respective wrapper 
	 * types (Integer, Double, etc.). Number format or decimal separator 
	 * will be the locale defaults.
	 * 
	 * @param ds the {@code DataSource} to save
	 * @param file the {@code File} to save to
	 * @param delimiter the delimiter to use
	 * @return true if success
	 */
	public static boolean saveCSV(DataSource ds, File file, char delimiter) {
		return DataSourceUtils.saveCSV(ds, file, delimiter, ds.getFields());
	}

	/**
	 * Saves a CSV text file to a data source. The first line of the file
	 * should contain the names of its fields. Strings are sought for the 
	 * delimiter and it is removed from them. Numbers must be in a format 
	 * suitable for parsing by the parser methods of their respective wrapper 
	 * types (Integer, Double, etc.). Number format or decimal separator 
	 * will be the locale defaults. Only the parameter fields are exported. 
	 * 
	 * @param ds the {@code DataSource} to save
	 * @param file the {@code File} to save to
	 * @param delimiter the delimiter to use
	 * @param fields the fields to export
	 * @return true if success
	 */
	public static boolean saveCSV(DataSource ds, File file, char delimiter, String[] fields) {
		PrintWriter out = preparePrintWriter(file);
		if (out == null) return false;
		DataSourceUtils.dumpCSV(ds, out, delimiter, fields);
		out.close();
		return true;
	}
	
	/**
	 * Dumps the contents of the {@code DataSource} in CSV form. 
	 * 
	 * @param ds the {@code DataSource} to dump
	 * @param out a {@code PrintWriter} to write to
	 * @param delimiter the delimiter to use
	 */
	public static void dumpCSV(DataSource ds, PrintWriter out, char delimiter) {
		DataSourceUtils.dumpCSV(ds, out, delimiter, ds.getFields());
	}
	
	/**
	 * Dumps the contents of the {@code DataSource} in CSV form. Only the
	 * parameter fields are exported. 
	 * 
	 * @param ds the {@code DataSource} to dump
	 * @param out a {@code PrintWriter} to write to
	 * @param delimiter the delimiter to use
	 * @param fields the fields to export
	 */
	public static void dumpCSV(DataSource ds, PrintWriter out, char delimiter, String[] fields) {
		Iterator<Row> iter;
		Class<?>[] types = new Class<?>[fields.length];
		int[] fieldIdx = new int[fields.length];
		Class<?>[] allTypes;
		
		allTypes = ds.getTypes();
		
		for (int i = 0; i < fields.length; i++) {
			fieldIdx[i] = ds.getFieldIndex(fields[i]);
			types[i] = allTypes[fieldIdx[i]];
			
			String f = fields[i];
			if (f.indexOf(delimiter) != -1) f.replace(delimiter, '_');
			if (i < fields.length-1) out.print(f + delimiter); 
			else out.print(f);
		}
		out.println();
		iter = ds.getAll();

		while (iter.hasNext()) {
			Row row = iter.next();
			for (int i = 0; i < fields.length; i++) {
				Object val = row.get(fieldIdx[i]);
				if (val == null) val = "";
				
				if (types[i] == String.class) {
					String r = (String) val;
					if (r.indexOf(delimiter) != -1) r.replace(delimiter, '_');
					if (i < fields.length-1) out.print(r + delimiter);
					else out.print(r);
				}
				else {
					if (i < fields.length-1) out.print(val.toString() + delimiter);
					else out.print(val.toString());
				}
			}
			out.println();
		}
	}
	
	private static PrintWriter preparePrintWriter(File file) {
		if (file == null) {
			logger.error("File name not set");
			return null;
		}

		PrintWriter out = null;
		try {
			File p = file.getParentFile();
			if (p != null) p.mkdirs();
			
			out = new PrintWriter(file, "UTF-8");
		} catch (FileNotFoundException fnfe) {
			logger.error("File not found: " + file.getAbsolutePath());
			return null;
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("encoding UTF-8 not supported", uee);
		}
		return out;
	}
	
	/**
	 * Returns an array with the types of the parameter {@code fields}. 
	 * 
	 * @param ds the {@code DataSource} to investigate
	 * @param fields field names
	 * @return an array of field types
	 */
	public static Class<?>[] getTypesForFields(DataSource ds, String[] fields) {
		synchronized (ds) {
			Class<?>[] types = new Class[fields.length];
			Class<?>[] allTypes = ds.getTypes();
			for (int i = 0; i < fields.length; i++) {
				types[i] = allTypes[ds.getFieldIndex(fields[i])];
			}
			return types;
		}
	}
	
	/**
	 * Searches {@code rows} to return the value of field {@code valueField} of the 
	 * {@code Row} which {@code keyField} is equal to the {@code key}. 
	 * 
	 * @param rows {@code Iterator} to search
	 * @param keyField the key field
	 * @param valueField the field to lookup 
	 * @param key key to find
	 * @return value of {@code valueField}
	 */
	public static Object lookup(Iterator<Row> rows, String keyField, String valueField, String key) {
		while (rows.hasNext()) {
			Row currency = rows.next();
			Object keyFieldValue = currency.get(keyField);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key)))
				return currency.get(valueField);
		}
		return null;
	}
}
