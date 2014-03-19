package compling.util.fileutil;

import java.util.Iterator;

/**
 * This class represents an iterator that takes another iterator as its argument and provides an iteration over the
 * specified range of elements.
 * 
 * @author Branimir Ciric
 */
public class NumberRangeIterator implements Iterator {

	Iterator iter;
	Object nextObject;
	int first, last, next;

	public NumberRangeIterator(int f, int l, Iterator input) {
		first = f;
		last = l;
		iter = input;
		for (next = 0; next < first; next++) {
			if (iter.hasNext()) {
				iter.next();
			}
		}
		nextObject = iter.next();
	}

	public Object next() {
		Object o = nextObject;
		if (iter.hasNext() && next < last) {
			nextObject = iter.next();
			next++;
		}
		else {
			nextObject = null;
		}
		return o;
	}

	public boolean hasNext() {
		return nextObject != null;
	}

	public void remove() {
		throw new UnsupportedOperationException("NumberRangeIterator does not support the remove() method.");
	}

	/*
	 * public static void main(String[] args) { NamedEntityAnnotationIterator neai = new
	 * NamedEntityAnnotationIterator(args[0]); NumberRangeIterator nri = new
	 * NumberRangeIterator(Integer.parseInt(args[1]), Integer.parseInt(args[2]), neai); while (nri.hasNext()) {
	 * System.out.println(nri.next()); } }
	 */
}
