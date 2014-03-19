// =============================================================================
//File        : SetFactory.java
//Author      : emok
//Change Log  : Created on Sep 19, 2007
//=============================================================================

package compling.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

//=============================================================================

public abstract class SetFactory<V> implements Serializable {

	public static class HashSetFactory<V> extends SetFactory<V> {

		private static final long serialVersionUID = 1L;

		public Set<V> buildSet() {
			return new HashSet<V>();
		}
	}

	public static class LinkedHashSetFactory<V> extends SetFactory<V> {

		private static final long serialVersionUID = 1L;

		public Set<V> buildSet() {
			return new LinkedHashSet<V>();
		}
	}

	public abstract Set<V> buildSet();

}
