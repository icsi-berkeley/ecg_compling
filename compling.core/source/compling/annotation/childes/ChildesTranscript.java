// =============================================================================
//File        : ChildesTranscript.java
//Author      : emok
//Change Log  : Created on Jul 21, 2005
//=============================================================================

package compling.annotation.childes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesAnnotation.SpeechActTier;
import compling.annotation.childes.ChildesFilter.Direction;
import compling.annotation.childes.ChildesUtilities.ChildesFormatter;
import compling.annotation.childes.ChildesUtilities.TextChildesFormatter;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.annotation.childes.FeatureBasedEntity.FillerType;
import compling.annotation.childes.FeatureBasedEntity.SimpleFeatureBasedEntity;
import compling.utterance.Utterance;
import compling.utterance.UtteranceAnnotation;
import compling.utterance.Word;

//=============================================================================

//Warning: Buggy! some errors (having to do with encoding of pauses) cause
//XML read errors and lead to infinite loops.

/**
 * <p>
 * <code>ChildesTranscript</code> represents a CHILDES transcript, which can be read in from the XML transcript
 * (available on TalkBank).
 * </p>
 * 
 * <p>
 * <b>Important:</b> Before any iterator functions can be used on the transcript, the transcript must be pre-processed.
 * Pre-processing numbers the utterances and split them into clauses based on pause marks.
 * </p>
 */
public class ChildesTranscript implements ChildesConstants, Iterable<ChildesTranscript.ChildesItem> {

	private Document document;
	private SAXBuilder builder;
	private static ChildesFormatter formatter = new TextChildesFormatter();
	private String filename;

	static boolean useCompoundWords = true;

	private static Logger logger = Logger.getLogger(ChildesTranscript.class.getName());

	// /-------------------------------------------------------------------------
	/**
	 * Constructor accepting an XML transcript
	 * 
	 * @param filename
	 *           the path to the XML transcript
	 * @throws AnnotationException
	 * @throws IOException
	 */
	public ChildesTranscript(String filename) throws IOException {
		this(new File(filename));
	}

	/**
	 * Constructor accepting an XML transcript
	 * 
	 * @param file
	 *           the <code>File</code> corresonding to the XML transcript
	 * @throws IOException
	 */
	public ChildesTranscript(File file) {
		filename = file.getName();
		builder = new SAXBuilder();
		try {
			document = builder.build(file);
		}
		catch (IOException ioe) {
			throw new AnnotationException("IO error in getting the XML document: " + ioe.getLocalizedMessage(), ioe);
		}
		catch (JDOMException jde) {
			throw new AnnotationException("Error(s) occured in parsing the XML document: " + jde.getLocalizedMessage(),
					jde);
		}
	}

	// /-------------------------------------------------------------------------
	/**
	 * <p>
	 * Must run this method on any transcript created from XML transcripts obtained from CHILDES / TalkBank before
	 * performing any other operations.
	 * </p>
	 * 
	 * Preprocess a <code>ChildesTranscript</code> such that
	 * <ol>
	 * <li>each utterance is tagged with an integer ID, and</li>
	 * <li>each utterance is split into <code>ChildesClause</code>s.</li>
	 * </ol>
	 * 
	 * @throws AnnotationException
	 */
	public void preprocessTranscript() throws AnnotationException {

		List<Element> matches = xpathQuery(document, XPATH_UTTERANCE);

		int counter = 1;
		for (Element e : matches) {
			assert (e.getName().equals(UTTERANCE));

			e.setAttribute(ID, ((Integer) counter).toString());
			counter++;

			// turn each clause into its own node.
			List<Element> clauses = splitUtteranceNode(e);
			if (clauses == null) {
				List<Element> prior = matches.get(matches.indexOf(e) - 1).getContent(new ElementFilter(CLAUSE));
				ChildesClause clause = new ChildesClause(prior.get(prior.size() - 1), this);
				throw new AnnotationException("Error while splitting utterance into clauses, "
						+ "occuring around utterance number: " + e.getAttributeValue(ID) + " after the clause: " + clause);
			}

			e.removeContent();
			e.addContent(clauses);

		}
	}

	public boolean hasBeenPreprocessed() throws AnnotationException {
		Element ce = xpathSingleQuery(document, XPATH_CLAUSE);
		return ce != null;
	}

	private List<Element> splitUtteranceNode(Element ue) throws AnnotationException {

		List<Element> clauses = new ArrayList<Element>();
		List<Element> pauseElements = xpathQuery(ue, XPATH_PAUSE);

		List<Element> content = (List<Element>) ue.getContent(new ElementFilter());
		List<Object> clonedChildren = (List<Object>) ue.cloneContent();

		int fromIndex = 0;
		int toIndex = 0;

		for (Element pe : pauseElements) {
			toIndex = content.indexOf(pe);
			if (toIndex < fromIndex) {
				logger.warning("toIndex is less than fromIndex when splitting utterance: " + toIndex + ", " + fromIndex);
				return null;
			}
			clauses.add(createClauseElement(clonedChildren.subList(fromIndex, toIndex + 1)));
			fromIndex = toIndex + 1;
		}

		toIndex = content.size();
		clauses.add(createClauseElement(clonedChildren.subList(fromIndex, toIndex)));

		return clauses;
	}

	private Element createClauseElement(List<Object> children) {
		Element ce = new Element(CLAUSE, NAMESPACE);
		ce.addContent(children);
		return ce;
	}

	// /-------------------------------------------------------------------------
	/*
	 * implements java.lang.Iterable#iterator()
	 */
	public ChildesIterator iterator() {
		return new ChildesIterator(this);
	}

	protected ChildesItem getNext(ChildesItem refItem, ChildesFilter filter) {
		return getAdj(refItem, filter, Direction.FOLLOWING);
	}

	protected ChildesItem getPrev(ChildesItem refItem, ChildesFilter filter) {
		return getAdj(refItem, filter, Direction.PRECEDING);
	}

	private ChildesItem getAdj(ChildesItem refItem, ChildesFilter filter, Direction direction)
			throws AnnotationException {

		Element ce = null;

		if (refItem == null && direction.equals(Direction.PRECEDING)) {
			return null;
		}
		else if (refItem == null && direction.equals(Direction.FOLLOWING)) {
			ce = xpathSingleQuery(document, filter.getAbsXPathExpr());
		}
		else {
			// First try to get sibling clauses (under the same utterance)
			ce = xpathSingleQuery(refItem.getJDOMElement(), filter.getRelSiblingXPathExpr(direction));
			// If no siblings found, try to get cousin clauses
			// (under the previous utterance)
			if (ce == null) {
				ce = xpathSingleQuery(refItem.getJDOMElement(), filter.getRelCousinXPathExpr(direction));
			}
		}

		if (ce == null) {
			return null;
		}

		if (ce.getName().equalsIgnoreCase(CLAUSE)) {
			ChildesClause clause = new ChildesClause(ce, this);

			// Check for regular expression match, if specified
			if (filter.getRegexp() != null) {
				if (clause.matches(filter.getRegexp())) {
					return clause;
				}
				else {
					// Throw out the current clause and look for the next one
					return getAdj(clause, filter, direction);
				}
			}
			else {
				return clause;
			}
		}
		else if (ce.getName().equalsIgnoreCase(EVENT)) {

			ChildesEvent event = new ChildesEvent(ce);
			return event;
		}
		else {
			return null;
		}
	}

	/**
	 * Returns all <code>ChildesClause</code>s in the transcript. In most situations, the use of the
	 * <code>ChildesIterator</code> is preferred.
	 * 
	 * @param filter
	 *           a <code>ChildesFilter</code> that specifies clause selection
	 * @return all clauses, in transcript order.
	 * @throws AnnotationException
	 */
	public List<ChildesClause> getAll(ChildesFilter filter) throws AnnotationException {

		List<ChildesClause> clauses = new ArrayList<ChildesClause>();
		List<Element> elements = xpathQuery(document, filter.getAbsXPathExpr());

		for (Element ce : elements) {
			ChildesClause clause = new ChildesClause(ce, this);

			if (filter.getRegexp() == null || clause.matches(filter.getRegexp())) {
				clauses.add(clause);
			}
		}

		return null;
	}

	public List<ChildesClause> getAllClauses() {
		return getClausesMatching(XPATH_CLAUSE, null);
	}

	/**
	 * xpathExpr must be a valid xpath expression retrieving Element nodes
	 */
	public List<ChildesClause> getClausesMatching(String xpathExpr, String textRegex) {
		List<ChildesClause> clauses = new ArrayList<ChildesClause>();

		List<Element> elements = xpathQuery(document, xpathExpr);
		for (Element clauseE : elements) {
			ChildesClause clause = new ChildesClause((Element) clauseE, this);
			if (textRegex == null || clause.matches(textRegex)) {
				clauses.add(clause);
			}
		}
		return clauses;
	}

	public List<String> getParticipantIDs() {

		List<String> participants = new ArrayList<String>();

		List<Element> participantElements = xpathQuery(document, XPATH_PARTICIPANT);

		for (Element p : participantElements) {
			String id = p.getAttributeValue(ID);
			if (id != null) {
				participants.add(id);
			}
			else {
				logger.warning("One or more participants are declared without an id");
			}
		}
		return participants;
	}

	public List<SimpleFeatureBasedEntity> getSettingEntitiesAndBindings() throws AnnotationException {
		// Entities and Bindings in the Setting are assumed to hold throughout the duration of the transcript
		List<Element> elements = xpathQuery(document, XPATH_SETTING);
		return getEntitiesAndBindings(elements);
	}

	public List<SimpleFeatureBasedEntity> getSetupEntitiesAndBindings() throws AnnotationException {
		// Entities and Bindings in the Setup block are expected to change in the course of the transcript
		List<Element> elements = xpathQuery(document, XPATH_SETUP);
		return getEntitiesAndBindings(elements);
	}

	protected List<SimpleFeatureBasedEntity> getEntitiesAndBindings(List<Element> elements) throws AnnotationException {
		List<SimpleFeatureBasedEntity> entities = new ArrayList<SimpleFeatureBasedEntity>();

		for (Element element : elements) {
			String type = element.getName();
			String id = element.getAttributeValue(ID);
			String cat = element.getAttributeValue(CATEGORY);
			SimpleFeatureBasedEntity entity = new SimpleFeatureBasedEntity(element, type, cat, id);

			if (type.equals(ENTITY)) {
				if (id == null) {
					logger.warning("One or more entities in the Setting are declared without an id");
				}
				if (cat == null) {
					logger.warning("One or more entities in the Setting are declared without a type");
				}
			}
			else if (type.equals(BINDING)) {
				String field = element.getAttributeValue(FIELD);
				entity.addFiller(FIELD, FillerType.REFERENCE, field);

				String source_ref = element.getAttributeValue(SOURCE_REF);
				entity.addFiller(SOURCE_REF, FillerType.REFERENCE, source_ref);

				if (element.getAttribute(REFERENCE) != null) {
					entity.addFiller(VALUE, FillerType.REFERENCE, element.getAttributeValue(REFERENCE));
				}
				else if (element.getAttribute(VALUE) != null) {
					entity.addFiller(VALUE, FillerType.VALUE, element.getAttributeValue(VALUE));
				}
			}

			entities.add(entity);

		}

		return entities;
	}

	public FeatureBasedEntity<?> getPrecedingAnnotationWithID(String ID, GoldStandardAnnotation currentAnnotation) {
		List<Element> elements = xpathQuery(currentAnnotation.getJDOMElement(), XPATH_ANNOTATION_BY_ID + ID
				+ XPATH_CLOSE_RESTRICTION);
		if (elements.isEmpty()) {
			return null;
		}
		if (elements.size() > 1) {
			throw new AnnotationException("More than one item with the ID " + ID + " declared in the transcript");
		}
		Element element = elements.get(0);
		if (element.getName().equals(ChildesConstants.SPEECH_ACT_ANNOTATION)) {
			return new SpeechActTier(element.getParentElement()).getContent();
		}
		else if (element.getName().equals(ChildesConstants.EVENT)) {
			return new ChildesEvent(element);
		}
		else if (GSPrimitive.getType(element.getName()) != null) {
			return new ExtendedFeatureBasedEntity(element);
		}
		else {
			return null;
		}
	}

	public static List<Element> xpathQuery(Object context, String xpathExpr) {
		try {
			XPath xpath = XPath.newInstance(xpathExpr);
			xpath.addNamespace(NS, NAMESPACE);

			List<?> matches = xpath.selectNodes(context);
			List<Element> elements = new ArrayList<Element>();

			for (Object o : matches) {
				if (o instanceof Element) {
					elements.add((Element) o);
				}
			}
			return elements;

		}
		catch (JDOMException jde) {
			throw new AnnotationException("Error(s) in evaluating the XPath expression: " + xpathExpr, jde);
		}
	}

	public static Element xpathSingleQuery(Object context, String xpathExpr) {
		try {
			XPath xpath = XPath.newInstance(xpathExpr);
			xpath.addNamespace(NS, NAMESPACE);

			Object match = xpath.selectSingleNode(context);
			if (match instanceof Element) {
				return (Element) match;
			}
			else {
				return null;
			}
		}
		catch (JDOMException jde) {
			throw new AnnotationException("Error(s) in evaluating the XPath expression: " + xpathExpr, jde);
		}
	}

	// /-------------------------------------------------------------------------
	/**
	 * Exports the transcript to an XML file
	 * 
	 * @param file
	 *           The <code>File</code> to output to
	 * @throws IOException
	 */
	public void outputXML(File file) throws IOException {

		FileOutputStream fos = new FileOutputStream(file);
		XMLOutputter outputter = new XMLOutputter();
		outputter.output(getJDOMDocument(), fos);
		fos.close();
	}

	public static void setFormatter(ChildesFormatter formatter) {
		ChildesTranscript.formatter = formatter;
	}

	public static ChildesFormatter getFormatter() {
		return formatter;
	}

	public String toString() {
		return formatter.format(this);
	}

	public static boolean useCompoundWords() {
		return useCompoundWords;
	}

	public String getSource() {
		return filename;
	}

	protected Document getJDOMDocument() {
		return document;
	}

	// =============================================================================

	public interface ChildesItem {
		Element getJDOMElement();

		String getID();
	}

	/*
	 * // =============================================================================
	 * 
	 * public class ChildesEntity extends SimpleFeatureBasedEntity implements ChildesItem {
	 * 
	 * protected ChildesEntity(Element element) { super(element, element.getAttributeValue(CATEGORY),
	 * element.getAttributeValue(ID));
	 * 
	 * List<Element> bindings = (List<Element>) getJDOMElement().getContent(new ElementFilter(BINDING)); for (Element e :
	 * bindings) { extractFillerFromBinding(e); } }
	 * 
	 * }
	 */
	// =============================================================================

	/**
	 * <p>
	 * <code>ChildesClause</code> represents a clause in a CHILDES transcript, which is a segment in an utterance. Each
	 * clause can be tagged with multiple <code>UtteranceAnnotation</code>s, one type of which is a
	 * <code>ChildesAnnotation</code>. Multiple tiers of annotation can reside underneath the ChildesAnnotation, however.
	 * 
	 * @author emok
	 */
	public class ChildesClause implements ChildesItem, Utterance<Word, String> {

		// Note that the toString method of this class relies on a
		// private variable (formatter) of its containing class.

		private Element element = null;
		private List<Word> text = null;
		private ChildesTranscript transcript = null;
		private HashMap<String, UtteranceAnnotation> annotations = new HashMap<String, UtteranceAnnotation>();
		private Map<Integer, Integer> compoundToLexSpanMap = new TreeMap<Integer, Integer>();
		private Map<Integer, Integer> lexToCompoundSpanMap = new TreeMap<Integer, Integer>();
		int lexSize = 0;

		protected ChildesClause(Element ce, ChildesTranscript transcript) throws AnnotationException {
			// super(ChildesItemType.CLAUSE, ce);
			this.element = ce;
			this.transcript = transcript;
			if (!ce.getName().equalsIgnoreCase(CLAUSE)) {
				throw new AnnotationException("The supplied element is not a clause");
			}
			extractText(useCompoundWords());
			addAnnotation(new ChildesAnnotation(this));
		}

		public int getLexSize() {
			return lexSize;
		}

		protected void extractText(boolean useCompoundWords) throws AnnotationException {

			// FIXME: how to display punctuations in clauses?

			List<Element> wordElements = xpathQuery(getJDOMElement(), XPATH_WORD_DESCENDANT);
			String separator = " ";
			Element lastWn = null;
			Element currentWn = null;

			text = new ArrayList<Word>();
			String s = "";

			int lexLeft = 0;
			int compoundLeft = 0;
			if (useCompoundWords) {
				for (Element we : wordElements) {
					Element retrace = xpathSingleQuery(we, XPATH_RETRACE_CHILD);
					if (retrace == null) {
						currentWn = xpathSingleQuery(we, XPATH_WORDNET_PARENT);
						if (currentWn != null && currentWn == lastWn) {
							separator = COMPOUND_SEPERATOR;
						}
						else {
							separator = DEFAULT_SEPERATOR;
							compoundToLexSpanMap.put(compoundLeft, lexLeft);
							lexToCompoundSpanMap.put(lexLeft, compoundLeft);
							compoundLeft++;
						}

						String word;
						Element replacement = xpathSingleQuery(we, XPATH_REPLACEMENT_CHILD);
						if (replacement == null) {
							word = we.getText();
						}
						else {
							word = replacement.getText();
						}

						lastWn = currentWn;
						s += separator;
						s += word;
						lexLeft++;
					}
				}
				compoundToLexSpanMap.put(compoundLeft, lexLeft);
				lexToCompoundSpanMap.put(lexLeft, compoundLeft);
				lexSize = lexLeft;

				s = s.trim();
				if (s.length() != 0) {
					List<String> words = Arrays.asList(s.split(DEFAULT_SEPERATOR));
					for (String w : words) {
						text.add(new Word(w));
					}
				}
			}
			else {
				for (Element we : wordElements) {
					Element retrace = xpathSingleQuery(we, XPATH_RETRACE_CHILD);
					if (retrace == null) {
						Element replacement = xpathSingleQuery(we, XPATH_REPLACEMENT_CHILD);
						if (replacement == null) {
							text.add(new Word(we.getText()));
						}
						else {
							text.add(new Word(replacement.getText()));
						}
					}
				}
			}
		}

		public String getText(String separator) {
			StringBuffer sb = new StringBuffer();
			int size = size();
			if (size > 0) {
				for (Word w : text.subList(0, size - 1)) {
					sb.append(w.getOrthography());
					sb.append(separator);
				}
				sb.append(text.get(size - 1));
			}
			return sb.toString();
		}

		public List<String> getText() {
			List<String> textArray = new ArrayList<String>();
			for (Word w : text) {
				textArray.add(w.getOrthography());
			}
			return textArray;
		}

		public String getSpeaker() {
			return ((Element) getJDOMElement().getParent()).getAttributeValue(SPEAKER);
		}

		protected boolean matches(String regexp) {
			String s = getText(DEFAULT_SEPERATOR);
			return s.matches(regexp);
		}

		public int size() {
			return text.size();
		}

		public List<Word> getElements() {
			return text;
		}

		public Word getElement(int i) {
			return text.get(i);
		}

		public void setElements(List<Word> words) {
			text = words;
		}

		public ChildesTranscript getContainingTranscript() {
			return transcript;
		}

		public String getSource() {
			return getContainingTranscript().getSource();
		}

		public void setSource(String source) {
			throw new UnsupportedOperationException();
		}

		public int getSourceOffset() {
			return Integer.valueOf(((Element) element.getParent()).getAttributeValue(ID));
		}

		public void setSourceOffset(int offset) {
			throw new UnsupportedOperationException();
		}

		public void addAnnotation(UtteranceAnnotation sa) {
			annotations.put(sa.getAnnotationType(), sa);
		}

		public UtteranceAnnotation getAnnotation(String annotationType) {
			return annotations.get(annotationType);
		}

		public ChildesAnnotation getChildesAnnotation() {
			return (ChildesAnnotation) annotations.get(UtteranceAnnotation.CHILDES);
		}

		public boolean hasAnnotation(String annotationType) {
			return annotations.containsKey(annotationType);
		}

		public Element getJDOMElement() {
			return element;
		}

		public String getID() {

			String parentID = ((Element) element.getParent()).getAttributeValue(ID);
			String clauseID = String.valueOf(((Element) element.getParent()).getContent(new ElementFilter(CLAUSE))
					.indexOf(element));
			return parentID + "c" + clauseID;
		}

		public String toString() {
			return formatter.format(this);
		}

		public Map<Integer, Integer> getCompoundToLexSpanMap() {
			return compoundToLexSpanMap;
		}

		public Map<Integer, Integer> getLexToCompoundSpanMap() {
			return lexToCompoundSpanMap;
		}

	}

	// =============================================================================

	public class ChildesEvent extends SimpleFeatureBasedEntity implements ChildesItem {

		protected ChildesEvent(Element ce) {
			super(ce, ce.getName(), ce.getAttributeValue(CATEGORY), ce.getAttributeValue(ID));
		}

		public String toString() {
			return formatter.format(this);
		}

	}

	// =============================================================================

	public static void main(String argv[]) {

		if (argv.length < 1) {
			System.err.println("Usage: java ChildesTranscript in_filename [out_filename]");
			System.exit(1);
		}

		ChildesTranscript transcript = null;

		try {
			transcript = new ChildesTranscript(argv[0]);
			assert (transcript != null);
			transcript.preprocessTranscript();

			if (argv.length >= 2) {
				transcript.outputXML(new File(argv[1]));
			}
		}
		catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
		catch (AnnotationException de) {
			System.err.println(de.getMessage());
		}
	}

}
