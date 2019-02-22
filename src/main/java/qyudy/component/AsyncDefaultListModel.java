package qyudy.component;

import java.util.Enumeration;

import javax.swing.ListModel;

public interface AsyncDefaultListModel<E> extends ListModel<E> {
	void copyInto(Object anArray[]);
	void trimToSize();
	void ensureCapacity(int minCapacity);
	void setSize(int newSize);
	int capacity();
	int size();
	boolean isEmpty();
	Enumeration<E> elements();
	boolean contains(Object elem);
	int indexOf(Object elem);
	int indexOf(Object elem, int index);
	int lastIndexOf(Object elem);
	int lastIndexOf(Object elem, int index);
	E elementAt(int index);
	E firstElement();
	E lastElement();
	void setElementAt(E element, int index);
	void removeElementAt(int index);
	void insertElementAt(E element, int index);
	void addElement(E element);
	boolean removeElement(Object obj);
	void removeAllElements();
	String toString();
	Object[] toArray();
	E get(int index);
	E set(int index, E element);
	void add(int index, E element);
	E remove(int index);
	void clear();
	void removeRange(int fromIndex, int toIndex);
}
