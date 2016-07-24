package com.infrarch.commons.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default {@code DataSource} implementation using a {@code LinkedList}.
 * 
 * @author Assen Antov
 * @version 1.1, 02/2006
 * @version 2.0, 05/2016
 */
public class DefaultDataSource extends AbstractDataSource {

	private String[] fields;
	private Class<?>[] types;
	private List<Row> rows;
	
	public DefaultDataSource(String name, String[] fields, Class<?>[] types) {
		super(name);
		setStructure(fields, types);
		changes.set(0);
	}
	
	/**
	 * Sets a new structure of the {@code DataSource}. This will clear all
	 * contained {@code Row}s.
	 * 
	 * @param fields {@code DataSource}'s fields
	 * @param types {@code DataSource}'s field types
	 */
	public synchronized void setStructure(String[] fields, Class<?>[] types) {
		if (fields == null || types == null) throw new IllegalArgumentException("attempt to set null fields or types");
		if (fields.length != types.length) throw new IllegalArgumentException("fields.length != types.length: " + fields.length + " != " + types.length);
		
		this.fields = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] != null) this.fields[i] = fields[i].toUpperCase();
			else throw new IllegalArgumentException("attempt to set null field at: " + i);
		}
		
		this.types = new Class[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] != null) this.types[i] = types[i];
			else throw new IllegalArgumentException("attempt to set null type at: " + i);
		}
		
		clear0();
		rows = new LinkedList<Row>();
	}

	@Override
	public synchronized String[] getFields() {
		if (fields == null) throw new IllegalStateException("fields not set");
		return Arrays.copyOfRange(fields, 0, fields.length);
	}

	@Override
	public synchronized Class<?>[] getTypes() {
		if (types == null) throw new IllegalStateException("types not set"); 
		return Arrays.copyOfRange(types, 0, types.length);
	}

	@Override
	public synchronized void addField(String field, Class<?> type) {
		if (fields == null) throw new IllegalStateException("fields not set");
		if (types == null) throw new IllegalStateException("types not set");
		if (fields == null || types == null) throw new IllegalArgumentException("attempt to add a null field or type");
		
		fields = append(fields, field.toUpperCase());
		types = append(types, type);
		
		List<Row> newRows = new LinkedList<Row>();
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row row = iter.next();
			newRows.add(new DefaultRow(this, append(row.getData(), null)));
		}
		
		rows = newRows;
		changed();
	}

	@Override
	public synchronized boolean deleteField(String field) {
		int idx = getFieldIndex(field);
		if (idx == -1) return false;

		fields = shrink(fields, idx);
		types = shrink(types, idx);
		
		List<Row> newRows = new LinkedList<Row>();
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row row = iter.next();
			newRows.add(new DefaultRow(this, shrink(row.getData(), idx)));
		}
		
		rows = newRows;
		changed();
		
		return true;
	}

	/**
	 * Appends a item at the end of a data array.
	 * 
	 * @param array data array to append to
	 * @param d item to append
	 * @return a new array
	 */
	private Object[] append(Object[] array, Object d) {
		Object[] newArray = new Object[array.length+1];
		for (int i = 0; i < array.length; i++) {
			newArray[i] = array[i];
		}
		newArray[array.length] = d;
		return newArray;
	}
	
	/**
	 * Appends a item at the end of a data array.
	 * 
	 * @param array data array to append to
	 * @param d item to append
	 * @return a new array
	 */
	private String[] append(String[] array, String d) {
		String[] newArray = new String[array.length+1];
		for (int i = 0; i < array.length; i++) {
			newArray[i] = array[i];
		}
		newArray[array.length] = d;
		return newArray;
	}
	
	/**
	 * Appends a item at the end of a data array.
	 * 
	 * @param array data array to append to
	 * @param d item to append
	 * @return a new array
	 */
	private Class<?>[] append(Class<?>[] array, Class<?> d) {
		Class<?>[] newArray = new Class<?>[array.length+1];
		for (int i = 0; i < array.length; i++) {
			newArray[i] = array[i];
		}
		newArray[array.length] = d;
		return newArray;
	}
	
	/**
	 * Deletes an element of an array.
	 * 
	 * @param array data array to shrink
	 * @param idx index of the element to delete
	 * @return a new array
	 */ 
	private Object[] shrink(Object[] array, int idx) {
		Object[] newArray = new Object[array.length-1];
		for (int i = 0; i < array.length; i++) {
			if (i < idx) newArray[i] = array[i];
			else if (i > idx) newArray[i-1] = array[i];
		}
		return newArray;
	}

	/**
	 * Deletes an element of an array.
	 * 
	 * @param array data array to shrink
	 * @param idx index of the element to delete
	 * @return a new array
	 */ 
	private String[] shrink(String[] array, int idx) {
		String[] newArray = new String[array.length-1];
		for (int i = 0; i < array.length; i++) {
			if (i < idx) newArray[i] = array[i];
			else if (i > idx) newArray[i-1] = array[i];
		}
		return newArray;
	}
	
	/**
	 * Deletes an element of an array.
	 * 
	 * @param array data array to shrink
	 * @param idx index of the element to delete
	 * @return a new array
	 */ 
	private Class<?>[] shrink(Class<?>[] array, int idx) {
		Class<?>[] newArray = new Class<?>[array.length-1];
		for (int i = 0; i < array.length; i++) {
			if (i < idx) newArray[i] = array[i];
			else if (i > idx) newArray[i-1] = array[i];
		}
		return newArray;
	}
	
	@Override
	public synchronized Iterator<Row> get(String field, Object value) {
		if (fields == null) throw new IllegalStateException("fields not set");
		
		int idx = getFieldIndex(field);
		if (idx == -1) {
			throw new IllegalArgumentException("no such field: " + field);
		}
		
		List<Row> result = new LinkedList<Row>();
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row row = iter.next();
			Object val = row.get(idx);
			if ((val == null && value == null) || (val != null && val.equals(value))) {
				result.add(row);
			}
		}
		
		if (getOrderingField() != null) {
			sort0(result, getOrderingField(), getOrderAscending());
		}
	
		return new ImmutableListIterator<Row>(result.listIterator());
	}
	
	@Override
	public synchronized Row getFirst(String field, Object value) {
		if (fields == null) throw new IllegalStateException("fields not set");
		
		int idx = getFieldIndex(field);
		if (idx == -1) {
			throw new IllegalArgumentException("no such field: " + field);
		}
		
		Row result = null;
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row row = iter.next();
			Object val = row.get(idx);
			if ((val == null && value == null) || (val != null && val.equals(value))) {
				result = row;
				break;
			}
		}
		return result;
	}
	
	@Override
	public synchronized Iterator<Row> getAll() {
		if (fields == null) throw new IllegalStateException("fields not set");
		
		List<Row> result = new ArrayList<Row>(rows);
		if (getOrderingField() != null) {
			sort0(result, getOrderingField(), getOrderAscending());
		}
		return new ImmutableListIterator<Row>(result.listIterator());
	}
	
	@Override
	public synchronized Object lookup(String keyField, Object key, String valueField) {
		int idxKey = getFieldIndex(keyField);
		if (idxKey == -1) {
			throw new IllegalArgumentException("no such field: " + keyField);
		}
		
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row current = iter.next();
			Object keyFieldValue = current.get(idxKey);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key)))
				return current.get(valueField);
		}
		return null;
	}
		
	@Override
	public synchronized int set(String keyField, Object key, String field, Object value) {
		int ch = 0;
		
		int idxKey = getFieldIndex(keyField);
		if (idxKey == -1) {
			throw new IllegalArgumentException("no such field: " + keyField);
		}
		
		int idxField = getFieldIndex(field);
		if (idxField == -1) {
			throw new IllegalArgumentException("no such field: " + field);
		}
		
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row current = iter.next();
			Object keyFieldValue = current.get(idxKey);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key))) {
				if (current.set(idxField, value)) ch++;
			}
		}
		
		return ch;
	}

	@Override
	public synchronized int setFirst(String keyField, Object key, String field, Object value) {
		int idxKey = getFieldIndex(keyField);
		if (idxKey == -1) {
			throw new IllegalArgumentException("no such field: " + keyField);
		}
		
		int idxField = getFieldIndex(field);
		if (idxField == -1) {
			throw new IllegalArgumentException("no such field: " + field);
		}
		
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row current = iter.next();
			Object keyFieldValue = current.get(idxKey);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key))) {
				if (current.set(idxField, value)) return 1;
				else return 0;
			}
		}
		
		return 0;
	}

	@Override
	public synchronized int delete(String keyField, Object key) {
		int ch = 0;
		
		int idxKey = getFieldIndex(keyField);
		if (idxKey == -1) {
			throw new IllegalArgumentException("no such field: " + keyField);
		}
		
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row current = iter.next();
			Object keyFieldValue = current.get(idxKey);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key))) {
				iter.remove();
				ch++;
			}
		}
		
		return ch;
	}

	@Override
	public synchronized int deleteFirst(String keyField, Object key) {
		int idxKey = getFieldIndex(keyField);
		if (idxKey == -1) {
			throw new IllegalArgumentException("no such field: " + keyField);
		}
		
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row current = iter.next();
			Object keyFieldValue = current.get(idxKey);
			if ((keyFieldValue == null && key == null) || (keyFieldValue != null && keyFieldValue.equals(key))) {
				iter.remove();
				return 1;
			}
		}
		
		return 0;
	}
	
	@Override
	public synchronized int getFieldIndex(String field) {
		if (fields == null) throw new IllegalStateException("fields not set");
		
		String s = field.toUpperCase();
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equals(s)) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public synchronized Row append(Object[] data) {
		if (data.length != fields.length) throw new IllegalArgumentException("illegal data length: " + data.length);
		for (int i = 0; i < data.length; i++) {
			if (data[i] != null && !types[i].isInstance(data[i])) {
				throw new IllegalStateException("wrong data type at column index "+i+": "+data[i].getClass() + " (should be "+types[i]+")");
			}
		}
		Row row = new DefaultRow(this, data);
		rows.add(row);
		changed();
		return row;
	}
	
	@Override
	/**
	 * Does nothing but {@link #flush()}.
	 * 
	 * @see DataSource#close()
	 */
	public void close() {
		flush();
	}
	
	@Override
	public synchronized int size() {
		return rows.size();
	}
	
	@Override
	public synchronized void clear() {
		if (rows != null) {
			int count = 0;
			Iterator<Row> iter = getAll();
			while (iter.hasNext()) {
				((DefaultRow) iter.next()).delete0();
				count++;
			}
			changes.addAndGet(count-1);
			changed();
		}
	}
	
	/**
	 * Used in initialisers. The same as {@link #clear()} but does not register
	 * changes and does not invoke a flush.
	 */
	private void clear0() {
		if (rows != null) {
			Iterator<Row> iter = getAll();
			while (iter.hasNext()) {
				((DefaultRow) iter.next()).delete0();
			}
		}
	}
	
	/**
	 * Sorts the {@code DataSource}'s {@code Row}s by the parameter field.
	 * 
	 * @param field field to sort by
	 * @param ascending <code>true</code> for ascending order
	 * @return <code>false</code>, if wrong index or field type not {@code Comparable}
	 */
	public synchronized boolean sort(String field, boolean ascending) {
		return sort0(rows, field, ascending);
	}
	
	/**
	 * Sorts a list of {@code Row}s by field and order.
	 */
	private boolean sort0(List<Row> rows, String field, boolean ascending) {
		int idx = getFieldIndex(field);
		if (idx == -1) return false;
		
		Class<?>[] interf = types[idx].getInterfaces();
		boolean b = false;
		for (Class<?> c : interf) {
			if (c == Comparable.class) b = true;
		}
		if (!b) return false;
		
		Collections.sort(rows, new Comparator<Row>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public int compare(Row a, Row b) {
				return ascending? 
						((Comparable) a.getData()[idx]).compareTo(b.getData()[idx]) : 
						-((Comparable) a.getData()[idx]).compareTo(b.getData()[idx]) ;
			}
		});
		return true;
	}
	
	@Override
	public synchronized String toString() {
		return super.toString();
	}
	
	/**
	 * The default {@code Row} implementation to contain the data.
	 */
	private class DefaultRow extends AbstractRow implements Serializable {
		
		private static final long serialVersionUID = 3775986814027560934L;
		private AtomicBoolean deleted = new AtomicBoolean(false);
		
		DefaultRow(AbstractDataSource ds, Object[] data) {
			super(ds, data);
		}
		
		@Override
		public boolean delete() {
			synchronized (ds) {
				boolean b = rows.remove(this);
				if (b) {
					deleted.set(true);
					ds.changed();
				}
				return b;
			}
		}
		
		/**	Does not increase the changes counter. */
		public boolean delete0() {
			synchronized (ds) {
				boolean b = rows.remove(this);
				if (b) deleted.set(true);
				return b;
			}
		}

		@Override
		public boolean isDeleted() {
			return deleted.get();
		}
	}
	
	private static final long serialVersionUID = -4546107906563673883L;
	
	private void writeObject(java.io.ObjectOutputStream out)
					  throws java.io.IOException {
		int code = 0;
		out.writeByte(code);
		
		out.writeObject(fields);
		out.writeObject(types);
		
		if (rows == null) {
			out.writeInt(0);
			return;
		}
		
		out.writeInt(rows.size());
		Iterator<Row> iter = rows.iterator();
		while (iter.hasNext()) {
			Row row = iter.next();
			out.writeObject(row.getData());
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		int code = in.readByte();

		setStructure((String[]) in.readObject(), (Class[]) in.readObject());
		
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			Object[] data = (Object[]) in.readObject();
			Row row = new DefaultRow(this, data);
			rows.add(row);
		}
	}
}