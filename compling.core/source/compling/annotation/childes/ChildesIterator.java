// =============================================================================
// File        : ChildesIterator.java
// Author      : emok
// Change Log  : Created on Jul 20, 2005
//=============================================================================

package compling.annotation.childes;

import java.util.Iterator;

import compling.annotation.childes.ChildesTranscript.ChildesItem;

//=============================================================================

public class ChildesIterator implements Iterator<ChildesItem> {

	ChildesFilter filter = null;
	ChildesTranscript transcript = null;
	ChildesItem prevItem = null;
	ChildesItem currentItem = null;
	ChildesItem nextItem = null;

	public ChildesIterator(ChildesTranscript t) {
		super();
		transcript = t;
		filter = new ChildesFilter();
		nextItem = transcript.getNext(null, filter);
	}

	public void setFilter(ChildesFilter f) {
		filter = f;
		nextItem = transcript.getNext(currentItem, filter);
	}

	public boolean hasNext() {
		return nextItem != null ? true : false;
	}

	public boolean hasPrev() {
		return prevItem != null ? true : false;
	}

	public ChildesItem next() {

		prevItem = currentItem;
		currentItem = nextItem;
		nextItem = transcript.getNext(currentItem, filter);

		return currentItem;

	}

	public ChildesItem prev() {

		nextItem = currentItem;
		currentItem = prevItem;

		prevItem = transcript.getPrev(currentItem, filter);
		return currentItem;

	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
