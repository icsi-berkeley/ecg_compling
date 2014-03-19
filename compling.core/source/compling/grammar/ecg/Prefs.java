// =============================================================================
// File        : Prefs.java
// Author      : emok
// Change Log  : Created on Dec 10, 2007
//=============================================================================

package compling.grammar.ecg;

import java.util.List;

//=============================================================================

public interface Prefs {

	public static enum Datatype {
		BOOL, STRING, INTEGER, DOUBLE, LISTSTRING;
	}

	public static interface Property {

		public Datatype getDataType();

	}

	// public HashMap<Property, String> getSettingsTable();

	public String getSetting(Property property);

	public List<String> getList(Property property);

	public Prefs clone();

	public String setSetting(Property property, String value);

}
