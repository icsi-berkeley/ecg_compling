package compling.util.html;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

/** Based on ecgs.gui.Toolkit.LinkListener */

public class TagParser {

	// private Vector aTagVector;

	private static Vector parseTagAttributes(Element elt) {
		Vector<ATag> aTags = new Vector<ATag>();
		return parseTagAttributes(elt, aTags);
	}

	private static Vector parseTagAttributes(Element elt, Vector<ATag> aTags) {
		AttributeSet attrs = elt.getAttributes();
		for (Enumeration e = attrs.getAttributeNames(); e.hasMoreElements();) {
			Object nextO = e.nextElement();
			if (nextO instanceof HTML.Tag) {
				if ((((HTML.Tag) nextO).toString()).equals("a")) {
					Object attrib = attrs.getAttribute(nextO);
					parseTag(attrib.toString(), aTags);
				}
			}
		}
		return aTags;
	}

	private static Vector parseTag(String p, Vector<ATag> aTags) {
		StringTokenizer st = new StringTokenizer(p);
		while (st.hasMoreTokens()) {
			String nt = st.nextToken();
			String desc = nt.substring(0, nt.indexOf("="));
			String value = nt.substring(nt.indexOf("=") + 1, nt.length());
			// System.out.println("Desc:" + desc + " and value:" + value);
			aTags.add(new ATag(desc, value));
		}
		return aTags;
	}

	private static String getValue(String desc, Vector tags) {
		for (Enumeration e = tags.elements(); e.hasMoreElements();) {
			ATag nt = (ATag) e.nextElement();
			if (nt.getDescriptor().equals(desc))
				return nt.getValue();
		}
		System.out.println("LinkListener cannot find requested tag descriptor:" + desc);
		return "";
	}

	private static class ATag {
		private String descriptor;
		private String value;

		public ATag(String _desc, String _val) {
			descriptor = _desc;
			value = _val;
		}

		public String getDescriptor() {
			return descriptor;
		}

		public String getValue() {
			return value;
		}
	}

	/**
	 * Retrieve link attribute value for HyperlinkEvent. Currently hardcoded for "a" link clicks only, e.g. to return
	 * <code>x</code> from link encoded as <code><a type=x>...</a></code>.
	 * 
	 * @param event
	 *           HyperlinkEvent with attribute of interest
	 * @param attrName
	 *           attribute of interest
	 * @return String value of given attribute
	 */
	public static String getLinkAttr(HyperlinkEvent event, String attrName) {
		Element elt = event.getSourceElement();
		Vector tags = parseTagAttributes(elt);
		return getValue(attrName, tags);
	}

}
