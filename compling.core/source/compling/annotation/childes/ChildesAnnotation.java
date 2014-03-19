// =============================================================================
// File        : ChildesAnnotation.java
// Author      : emok
// Change Log  : Created on Aug 17, 2005
//=============================================================================

package compling.annotation.childes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jdom.Element;
import org.jdom.Text;
import org.jdom.filter.ElementFilter;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.FeatureBasedEntity.Binding;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.annotation.childes.FeatureBasedEntity.SimpleFeatureBasedEntity;
import compling.util.MapSet;
import compling.utterance.UtteranceAnnotation;

//=============================================================================

/**
 * An implementation of <code>UtteranceAnnotation</code>, the ChildesAnnotation captures annotation specific to a
 * Childes Clause. Each annotation consists of multiple tiers (<code>AnnotationTier</code>), currently including but
 * extendable beyond speech act, vernacular, English translation, and the gold standard semantic annotations.
 * 
 * @author emok
 */
public class ChildesAnnotation implements UtteranceAnnotation, ChildesConstants {

	private Element clauseElement = null;
	private static Logger logger = Logger.getLogger(ChildesTranscript.class.getName());
	private ChildesClause clause = null;

	// /-------------------------------------------------------------------------
	/**
	 * Creates an annotation for the given <code>ChildesClause</code>
	 * 
	 * @param clause
	 * @throws AnnotationException
	 */

	public ChildesAnnotation(ChildesClause clause) {
		this.clause = clause;
		clauseElement = clause.getJDOMElement();
		clause.addAnnotation(this);
	}

	public String getAnnotationType() {
		return UtteranceAnnotation.CHILDES;
	}

	public <T> void setAnnotationTier(AnnotationTier<T> at) {
		String type = at.getJDOMElement().getAttributeValue(TYPE);
		Element oldElement = getAnnotationElement(type);
		if (oldElement != null) {
			clauseElement.removeContent(oldElement);
		}
		clauseElement.addContent(at.getJDOMElement());
	}

	public SpeechActTier getSpeechActTier() {
		Element ae = getAnnotationElement(SPEECH_ACT);
		return ae == null ? null : new SpeechActTier(ae);
	}

	public VernacularTier getVernacularTier() {
		Element ae = getAnnotationElement(VERNACULAR);
		return ae == null ? null : new VernacularTier(ae, clause);
	}

	public TranslationTier getTranslationTier() {
		Element ae = getAnnotationElement(TRANSLATION);
		return ae == null ? null : new TranslationTier(ae);
	}

	public GoldStandardTier getGoldStandardTier() {
		Element ae = getAnnotationElement(GOLD_STANDARD);
		return ae == null ? null : new GoldStandardTier(ae, clause);
	}

	public boolean removeTier(AnnotationTier<?> at) {
		return clauseElement.removeContent(at.getJDOMElement());
	}

	protected Element getAnnotationElement(String tierType) throws AnnotationException {
		return ChildesTranscript.xpathSingleQuery(clauseElement, XPATH_ANNOTATION_CHILD + XPATH_OPEN_TYPE_RESTRICTION
				+ tierType + XPATH_CLOSE_RESTRICTION);
	}

	protected Element getJDOMElement() {
		return clauseElement;
	}

	public String toString() {
		return ChildesTranscript.getFormatter().format(this);
	}

	// =============================================================================

	public interface AnnotationTier<T> {

		/**
		 * @return the JDOM element corresponding to the annotation
		 */
		public Element getJDOMElement();

		public T getContent();

	}

	// =============================================================================

	public static class VernacularTier implements AnnotationTier<String> {

		protected Element annotationElement = null;
		private ChildesClause clause = null;

		public VernacularTier(Element ae, ChildesClause clause) {
			if (ae == null)
				throw new AnnotationException("null XML element given for a vernacular tier annotation");
			annotationElement = ae;
			this.clause = clause;
		}

		public VernacularTier(String vernacular) {
			annotationElement = new Element(ANNOTATION);
			annotationElement.addContent(new Text(vernacular));
		}

		public String getContent() {
			if (annotationElement == null) {
				return "";
			}
			else {
				return annotationElement.getText();
			}
		}

		public String getWordsAt(int spanLeft, int spanRight) {
			if (ChildesTranscript.useCompoundWords()) {
				Map<Integer, Integer> compoundToLex = clause.getCompoundToLexSpanMap();
				int lexLeft = compoundToLex.get(spanLeft);
				int lexRight = compoundToLex.get(spanRight);
				return getContent() == "" ? "" : getContent().substring(lexLeft, lexRight);
			}
			else {
				return getContent() == "" ? "" : getContent().substring(spanLeft, spanRight + 1);
			}
		}

		public Element getJDOMElement() {
			return annotationElement;
		}
	}

	// =============================================================================

	public static class TranslationTier implements AnnotationTier<String> {

		protected Element annotationElement = null;

		public TranslationTier(Element ae) {
			if (ae == null)
				throw new AnnotationException("null XML element given for a translation tier annotation");
			annotationElement = ae;
		}

		public TranslationTier(String translation) {
			annotationElement = new Element(ANNOTATION);
			annotationElement.setAttribute(TYPE, TRANSLATION);
			annotationElement.addContent(new Text(translation));
		}

		public String getContent() {
			if (annotationElement == null) {
				return "";
			}
			else {
				return annotationElement.getText();
			}
		}

		public Element getJDOMElement() {
			return annotationElement;
		}
	}

	// =============================================================================

	public static class SpeechActTier implements AnnotationTier<FeatureBasedEntity> {

		protected Element annotationElement = null;

		public SpeechActTier(Element ae) {
			if (ae == null)
				throw new AnnotationException("null XML element given for a speech act tier annotation");
			annotationElement = ae;
		}

		public SimpleFeatureBasedEntity getContent() {
			List<Element> speechacts = (List<Element>) annotationElement.getContent(new ElementFilter(
					SPEECH_ACT_ANNOTATION));
			if (speechacts == null || speechacts.isEmpty()) {
				throw new AnnotationException("no speech act annotations found");
			}
			else if (speechacts.size() > 1) {
				logger.warning("multiple speech act annotations found for a clause");
			}
			Element element = speechacts.get(0);
			return new SimpleFeatureBasedEntity(element, element.getName(), element.getAttributeValue(CATEGORY),
					element.getAttributeValue(ID));
		}

		public String getAttribute(String role) {
			return annotationElement.getChildText(role);
		}

		public Set<String> getAttributes() {

			Set<String> roles = new HashSet<String>();
			for (Element child : (List<Element>) annotationElement.getChildren()) {
				roles.add(child.getName());
			}

			return roles;
		}

		public Element getJDOMElement() {
			return annotationElement;
		}

	}

	// =============================================================================

	public static class GoldStandardTier implements AnnotationTier<GoldStandardAnnotation> {

		protected Element annotationElement = null;
		ChildesClause clause;

		public GoldStandardTier(Element ae, ChildesClause clause) {
			if (ae == null)
				throw new AnnotationException("null XML element given for a gold standard tier annotation");
			annotationElement = ae;
			this.clause = clause;
		}

		public GoldStandardAnnotation getContent() {
			if (annotationElement == null) {
				return null;
			}
			else {
				return new GoldStandardAnnotation(annotationElement, clause);
			}
		}

		public Element getJDOMElement() {
			return annotationElement;
		}
	}

	public static class GoldStandardAnnotation {
		ChildesClause clause = null;
		Element annotationElement;
		MapSet<GSPrimitive, ExtendedFeatureBasedEntity> primitives = new MapSet<GSPrimitive, ExtendedFeatureBasedEntity>();
		List<ExtendedFeatureBasedEntity> allAnnotations = new ArrayList<ExtendedFeatureBasedEntity>();

		protected GoldStandardAnnotation(Element element, ChildesClause clause) {
			this.clause = clause;
			annotationElement = element;
			List<Element> sem = (List<Element>) element.getContent(new ElementFilter(SEMANTIC_ANNOTATION));
			if (sem.isEmpty()) {
				logger.warning("No semantic annotation found in the gold standard annotation");
			}
			else if (sem.size() > 1) {
				logger.warning("More than one semantic annotation found in gold standard annotation");
			}

			List<Element> primitiveEs = (List<Element>) sem.get(0).getContent(new ElementFilter());

			for (Element primE : primitiveEs) {
				// new feature based entity
				ExtendedFeatureBasedEntity prim = new ExtendedFeatureBasedEntity(primE);
				allAnnotations.add(prim);
				primitives.put(GSPrimitive.getType(primE.getName()), prim);
			}

		}

		public List<ExtendedFeatureBasedEntity> getArgumentStructureAnnotations() {
			List<ExtendedFeatureBasedEntity> argStructs = new ArrayList<ExtendedFeatureBasedEntity>();
			if (primitives.get(GSPrimitive.TE) != null) {
				argStructs.addAll(primitives.get(GSPrimitive.TE));
			}
			if (primitives.get(GSPrimitive.TS) != null) {
				argStructs.addAll(primitives.get(GSPrimitive.TS));
			}
			return argStructs;
		}

		public Collection<ExtendedFeatureBasedEntity> getAnnotationsOfType(GSPrimitive type) {
			return primitives.get(type);
		}

		public List<ExtendedFeatureBasedEntity> getAllAnnotations() {
			return allAnnotations;
		}

		public Binding getBindingBySpan(int left, int right) {
			int lexLeft, lexRight;
			if (ChildesTranscript.useCompoundWords()) {
				Map<Integer, Integer> compoundToLex = clause.getCompoundToLexSpanMap();
				lexLeft = compoundToLex.get(left);
				lexRight = compoundToLex.get(right);
			}
			else {
				lexLeft = left;
				lexRight = right;
			}

			Element e = ChildesTranscript.xpathSingleQuery(annotationElement,
					XPATH_BINDING + "[@left='" + String.valueOf(lexLeft) + "' and @right='" + String.valueOf(lexRight)
							+ "']");
			return e == null ? null : new Binding(e);
		}

		public String toString() {
			return ChildesTranscript.getFormatter().format(this);
		}

		public Element getJDOMElement() {
			return annotationElement;
		}
	}
}