// =============================================================================
// File        : LookupTable.java
// Author      : emok
// Change Log  : Created on Apr 27, 2007
//=============================================================================

package compling.util;

//=============================================================================

// REFACTOR: I think this is redundent with CounterMap

public class LookupTable<S, T> extends MapMap<S, T, Integer> {
	// <constituent, <construction, count>>

	private static final long serialVersionUID = 1L;

	public LookupTable() {
		super();
	}

	public void incrementCount(S a, T b, int increment) {
		int count = getCount(a, b);
		setCount(a, b, count + increment);
	}

	public void decrementCount(S a, T b, int decrement) {
		int count = getCount(a, b);
		setCount(a, b, count - decrement);
	}

	public void setCount(S a, T b, int count) {
		put(a, b, count);
	}

	public int getCount(S a, T b) {
		return (get(a, b) == null) ? 0 : get(a, b);
	}
}
