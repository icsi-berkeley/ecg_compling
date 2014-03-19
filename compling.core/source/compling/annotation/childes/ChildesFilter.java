// =============================================================================
// File        : ChildesFilter.java
// Author      : emok
// Change Log  : Created on Jul 19, 2005
//=============================================================================

package compling.annotation.childes;

import java.util.ArrayList;

import compling.annotation.AnnotationException;

//=============================================================================

public class ChildesFilter implements ChildesConstants {

	public enum Direction {

		PRECEDING, FOLLOWING;
	}

	private int fromIndex = -1;
	private int toIndex = -1;
	private String speaker = null;
	private String excludedSpeaker = null;
	private String keyword = null;
	// FUTURE: should switch to Word in case there is POS, etc
	private String regexp = null;

	public ChildesFilter() {
	}

	public ChildesFilter(String s, String k) {
		setSpeaker(s);
		setKeyword(k);
	}

	public void setUtteranceID(int from, int to) {
		fromIndex = from; // inclusive
		toIndex = to; // exclusive
	}

	public void setSpeaker(String s) {
		if (s != null && s.equals("")) {
			s = null;
		}
		speaker = s;
	}

	public void setSpeakerExclusion(String s) {
		if (s != null && s.equals("")) {
			s = null;
		}
		excludedSpeaker = s;
	}

	public void setKeyword(String k) throws AnnotationException {
		if (k != null && k.equals("")) {
			k = null;
		}
		if (k != null && (k.contains("+") || k.contains(" "))) {
			throw new AnnotationException("Multi-word keywords not supported at the moment. Use setRegexp() instead.");
		}
		keyword = k;
	}

	public void setRegexp(String re) {
		if (re != null && re.equals("")) {
			re = null;
		}
		regexp = re;
	}

	public String getSpeaker() {
		return speaker;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getRegexp() {
		return regexp;
	}

	public String getAbsXPathExpr() {
		StringBuffer xpathExpr = new StringBuffer(XPATH_UTTERANCE + generateIndexSpeakerXPath(false));
		xpathExpr.append("/" + XPATH_CLAUSE_TYPE + generateKeywordXPath());
		xpathExpr.append("|" + XPATH_EVENT);
		return xpathExpr.toString();
	}

	public String getRelSiblingXPathExpr(Direction direction) {

		// NOTE: the way that the xml is structured, only clauses can be siblings.
		// Events are always cousins to a clause (because events are at the same level
		// of the tree as the utterances, but can be sibling to another event.

		StringBuffer xpathExpr = new StringBuffer();

		if (direction.equals(Direction.FOLLOWING)) {
			xpathExpr.append("following-sibling::" + XPATH_CLAUSE_TYPE);
			xpathExpr.append(generateKeywordXPath());
		}
		else if (direction.equals(Direction.PRECEDING)) {
			xpathExpr.append("preceding-sibling::" + XPATH_CLAUSE_TYPE);
			xpathExpr.append(generateKeywordXPath());
		}
		else {
			System.err.println("unknown direction");
		}
		return xpathExpr.toString();
	}

	public String getRelCousinXPathExpr(Direction direction) {

		StringBuffer xpathExpr = new StringBuffer();

		if (direction.equals(Direction.FOLLOWING)) {
			xpathExpr.append("following::" + XPATH_UTTERANCE_TYPE);
			xpathExpr.append(generateIndexSpeakerXPath(false));
			xpathExpr.append("/" + XPATH_CLAUSE_TYPE);
			xpathExpr.append(generateKeywordXPath());
			xpathExpr.append("|following::" + XPATH_EVENT_TYPE);
		}
		else if (direction.equals(Direction.PRECEDING)) {
			xpathExpr.append("preceding::" + XPATH_UTTERANCE_TYPE);
			xpathExpr.append(generateIndexSpeakerXPath(true));
			xpathExpr.append("/" + XPATH_CLAUSE_TYPE + "[last()]");
			xpathExpr.append(generateKeywordXPath());
			xpathExpr.append("|preceding::" + XPATH_EVENT_TYPE);
		}
		else {
			System.err.println("unknown direction");
		}

		return xpathExpr.toString();
	}

	protected String generateIndexSpeakerXPath(boolean isSingle) {

		boolean hasFromIndex = (fromIndex != -1);
		boolean hasToIndex = (toIndex != -1);
		boolean hasSpeaker = (speaker != null);
		boolean hasSpeakerExclusion = (excludedSpeaker != null);
		ArrayList<String> strings = new ArrayList<String>();

		if (!(isSingle || hasFromIndex || hasToIndex || hasSpeaker || hasSpeakerExclusion))
			return "";

		if (isSingle)
			strings.add("1"); // used for reverse traversal

		if (hasFromIndex)
			strings.add("@id>=\"" + fromIndex + "\"");

		if (hasToIndex)
			strings.add("@id<\"" + toIndex + "\"");

		if (hasSpeaker)
			strings.add("@who=\"" + speaker + "\"");

		if (hasSpeakerExclusion)
			strings.add("@who!=\"" + excludedSpeaker + "\"");

		StringBuffer ret = new StringBuffer();
		for (String s : strings) {
			if (ret.length() == 0)
				ret.append(s);
			else
				ret.append(" and " + s);
		}

		return "[" + ret.toString() + "]";

	}

	protected String generateKeywordXPath() {
		String s = "";
		if (keyword != null) {
			s = "//w[text()='" + keyword + "']/ancestor::" + XPATH_CLAUSE_TYPE;
		}
		return s;
	}

	public static void main(String argv[]) {
		try {
			ChildesFilter f = new ChildesFilter("MOT", "hao2");
			f.setSpeaker("MOT");
			f.setKeyword("che1");
			f.setUtteranceID(0, 2);
			System.out.println(f.getAbsXPathExpr());
		}
		catch (AnnotationException de) {
			System.err.println(de.getMessage());
		}
	}

}
