package com.infrarch.commons.db;

import java.util.ListIterator;

/**
 * An {@code ListIterator} wrapper that does not allow modifying the
 * underlying list.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class ImmutableListIterator<E> implements ListIterator<E> {

	private final ListIterator<E> iter;
	
	public ImmutableListIterator(ListIterator<E> iter) {
		this.iter = iter;
	}
	
	@Override
	public boolean hasNext() {return iter.hasNext(); }

	@Override
	public E next() { return iter.next(); }

	@Override
	public boolean hasPrevious() { return iter.hasPrevious(); }

	@Override
	public E previous() { return iter.previous(); }

	@Override
	public int nextIndex() { return iter.nextIndex(); }

	@Override
	public int previousIndex() { return iter.previousIndex(); }

	@Override
	public void remove() { throw new UnsupportedOperationException(); }

	@Override
	public void set(Object e) { throw new UnsupportedOperationException(); }

	@Override
	public void add(Object e) { throw new UnsupportedOperationException(); }
}
