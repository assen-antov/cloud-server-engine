package com.infrarch.commons.db;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The basis for <code>DataSource</code> implementations.
 * 
 * @author Assen Antov
 * @version 1.1, 09/2005
 * @version 2.0, 05/2016
 */
public abstract class AbstractDataSource implements DataSource, Serializable {

	private String name;	// guarded by 'lock'
	protected AtomicInteger changes = new AtomicInteger(0);
	private int autoFlushThreshold = 300;	// guarded by 'this'
	private Connector connector;	// guarded by 'lock'
	protected Object lock = new Object();	// flush lock
	private String orderingField = "";
	private boolean ascending = true;
	
	public AbstractDataSource(String name) {
		setName(name);
	}
	
	@Override
	public String getName() {
		synchronized (lock) {
			return name;
		}
	}
	
	/**
	 * Sets the name of the {@code DataSource}.
	 * 
	 * @param name new name of the {@code DataSource}
	 */
	public void setName(String name) {
		synchronized (lock) {
			if (name != null) this.name = name;
			else name = "";
		}
	}
	
	@Override
	public abstract String[] getFields();

	@Override
	public abstract Class<?>[] getTypes();

	@Override
	public abstract Iterator<Row> get(String field, Object value);

	@Override
	public Row getFirst(String field, Object value) {
		Iterator<Row> iter = get(field, value);
		if (iter.hasNext()) return iter.next();
		else return null;
	}
	
	@Override
	public abstract Iterator<Row> getAll();

	@Override
	public abstract Object lookup(String keyField, Object key, String valueField);
	
	@Override
	public abstract Row append(Object[] data);

	@Override
	public abstract int getFieldIndex(String field);
	
	@Override
	public void flush() {
		synchronized (lock) {
			int oldChanges = changes.get(); 
			if (oldChanges == 0) return;
			if (connector == null) {
				log("no Connector set for DataSource: " + name);
				return;
			}
			log("flushing; thread: " + Thread.currentThread().getName() + "; changes: " + changes);
			if (!connector.save()) {
				log("could not flush DataSource: " + name);
				return;
			}
			changes.getAndAdd(-oldChanges); // changes may have occurred during writing
		}
	}

	@Override
	public abstract void close();
	
	@Override
	public abstract int size();

	@Override
	public abstract void addField(String field, Class<?> type);
	
	@Override
	public abstract boolean deleteField(String field);
	
	public Connector getConnector() {
		synchronized (lock) {
			return connector;
		}
	}

	public void setConnector(Connector connector) {
		synchronized (lock) {
			this.connector = connector;
		}
	}
	
	/**
	 * Returns the number of changes to the {@code DataSource} that trigger
	 * automatic flush.
	 * 
	 * @return the number of changes for automatic flush
	 */
	public synchronized int getAutoFlushThreshold() {
		return autoFlushThreshold;
	}

	/**
	 * Sets the number of changes to the {@code DataSource} that trigger
	 * automatic flush. If set to a negative number, no automatic flushes will be
	 * allowed.
	 * 
	 * @param autoFlushThreshold the number of changes for automatic flush
	 */
	public synchronized void setAutoFlushThreshold(int autoFlushThreshold) {
		this.autoFlushThreshold = autoFlushThreshold;
	}

	@Override
	public synchronized void changed() {
		int ch = changes.incrementAndGet();
		if (autoFlushThreshold < 0) return;
		if (ch >= autoFlushThreshold) {
			flush();
		}
	}
	
	@Override
	public int hasChanged() {
		return changes.get();
	}
	
	@Override
	public int set(String keyField, Object key, Object value) {
		return set(keyField, key, keyField, value);
	}
	
	@Override
	public abstract int set(String keyField, Object key, String field, Object value);

	@Override
	public int setFirst(String keyField, Object key, Object value) {
		return setFirst(keyField, key, keyField, value);
	}

	@Override
	public abstract int setFirst(String keyField, Object key, String field, Object value);

	@Override
	public abstract int delete(String keyField, Object key);

	@Override
	public abstract int deleteFirst(String keyField, Object key);

	@Override
	public abstract void clear();

	@Override
	public synchronized void setOrderingField(String field) {
		if (field != null) orderingField = field;
		else orderingField = "";
	}
	
	@Override
	public synchronized String getOrderingField() {
		if (orderingField == null || orderingField.equals("")) return null;
		return orderingField; 
	}
	
	@Override
	public synchronized void setOrderAscending(boolean b) { 
		ascending = b;
	}
	
	@Override
	public synchronized boolean getOrderAscending() { 
		return ascending; 
	}
	
	@Override
	public String toString() {
		synchronized (lock) {
			return name;
		}
	}
	
	/**
	 * Used to log events. May be overridden by superclasses.
	 * 
	 * @param s the event to log
	 */
	protected void log(String s) {
		System.err.println("[" + getClass().getName() + ", \"" + getName()+ "\"]: " + s);
	}
	
	private static final long serialVersionUID = -2124428032121679833L;
	
	/**
	 * Writes object's fields to the <code>ObjectOutputStream</code>. 
	 * <p>
	 * The method will first write one byte containing bit-coded information,
	 * regarding the serialized data.
	 * </p>
	 *
	 * @param out object stream to write to
	 * @throws java.io.IOException in case of I/O error
	 */
	private void writeObject(java.io.ObjectOutputStream out)
					  throws java.io.IOException {
		int code = 1;
		out.writeByte(code);

		out.writeUTF(name);
		out.writeInt(autoFlushThreshold);
		
		// version 1
		out.writeUTF(orderingField);
		out.writeBoolean(ascending);
	}

	/**
	 * Reads object's fields from the <code>ObjectInputStream</code>.
	 * <p>
	 * The method will first read the serialization version identifier and try
	 * to deserialize accordingly.
	 * </p>
	 *
	 * @param in object stream to read from
	 * @throws java.io.IOException in case of I/O error
	 * @throws ClassNotFoundException error instantiating the serialized  class
	 */
	private void readObject(java.io.ObjectInputStream in)
					 throws java.io.IOException, ClassNotFoundException {
		int code = in.readByte();
		
		name = in.readUTF();
		autoFlushThreshold = in.readInt();
		
		// version 1
		if (code >= 1) {
			orderingField = in.readUTF();
			ascending = in.readBoolean();
		}
		
		// initialize other fields
		lock = new Object();
		changes = new AtomicInteger(0);
	}
}