// =============================================================================
//File        : FeatureBasedEntity.java
//Author      : emok
//Change Log  : Created on Aug 15, 2007
//=============================================================================

package compling.annotation.childes;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import compling.util.MapFactory.LinkedHashMapFactory;
import compling.util.MapSet;
import compling.util.Pair;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public interface FeatureBasedEntity<T> {

	public Set<String> getRoles();

	public Set<T> getBinding(String role);

	public String getType();

	public String getCategory();

	public String getID();

	public String toString();

	public enum FillerType {
		REFERENCE, VALUE;

		public String toString() {
			return this.name();
		}
	}

// =============================================================================

	public static class SimpleFeatureBasedEntity implements FeatureBasedEntity<Pair<String, FillerType>>,
			ChildesConstants {

		protected Element element = null;
		protected String type = null;
		protected String id = null;
		protected String category = null;
		protected MapSet<String, Pair<String, FillerType>> roles;

		private static Logger logger = Logger.getLogger(ChildesTranscript.class.getName());

		public SimpleFeatureBasedEntity(Element element, String type, String category, String id) {
			this.element = element;
			this.type = type;
			this.category = category;
			this.id = id;
			roles = new MapSet<String, Pair<String, FillerType>>(
					new LinkedHashMapFactory<String, Set<Pair<String, FillerType>>>(),
					new LinkedHashSetFactory<Pair<String, FillerType>>());
			List<Element> xmlRoles = (List<Element>) getJDOMElement().getContent(new ElementFilter(BINDING));
			for (Element e : xmlRoles) {
				extractFillerFromBinding(e);
			}
		}

		public String getType() {
			return type;
		}

		public String getCategory() {
			return category;
		}

		public String getID() {
			return id;
		}

		public void addFiller(String feature, FillerType type, String filler) {
			if (roles.put(feature, new Pair<String, FillerType>(filler, type)) > 1) {
				logger.info("There has been an attempt to associate more than one filler with the role " + feature + " in "
						+ getID() + " of category " + getCategory());
			}
		}

		public Set<String> getRoles() {
			return roles.keySet();
		}

		public Set<Pair<String, FillerType>> getBinding(String role) {
			return roles.get(role);
		}

		public Element getJDOMElement() {
			return element;
		}

		protected void extractFillerFromBinding(Element bindingElement) {
			String field = bindingElement.getAttributeValue(ChildesConstants.FIELD);
			if (bindingElement.getAttribute(ChildesConstants.REFERENCE) != null) {
				addFiller(field, FillerType.REFERENCE, bindingElement.getAttributeValue(ChildesConstants.REFERENCE));
			}
			else if (bindingElement.getAttribute(ChildesConstants.VALUE) != null) {
				addFiller(field, FillerType.VALUE, bindingElement.getAttributeValue(ChildesConstants.VALUE));
			}
		}

		public String toString() {
			return ChildesTranscript.getFormatter().format(this);
		}
	}

	public static class Binding {
		protected Element element = null;
		protected Map<String, String> attributes = new LinkedHashMap<String, String>();
		protected Integer spanLeft = null;
		protected Integer spanRight = null;
		protected String field = null;

		public Binding(Element element) {
			for (Attribute attribute : (List<Attribute>) element.getAttributes()) {
				if (attribute.getName().equals(ChildesConstants.FIELD)) {
					field = attribute.getValue();
				}
				else if (attribute.getName().equals(ChildesConstants.SPAN_LEFT)) {
					spanLeft = Integer.valueOf(attribute.getValue());
				}
				else if (attribute.getName().equals(ChildesConstants.SPAN_RIGHT)) {
					spanRight = Integer.valueOf(attribute.getValue());
				}
				else {
					attributes.put(attribute.getName(), attribute.getValue());
				}
			}
		}

		public String getField() {
			return field;
		}

		public Integer getSpanLeft() {
			return spanLeft;
		}

		public Integer getSpanRight() {
			return spanRight;
		}

		public Element getJDOMElement() {
			return element;
		}

		public String getAttributeValue(String attribute) {
			return attributes.get(attribute);
		}

		public Set<String> getAttributes() {
			return attributes.keySet();
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(field);
			if (spanLeft != null && spanRight != null) {
				sb.append("(").append(spanLeft).append(", ").append(spanRight).append(")");
			}
			sb.append(": ");
			for (String attribute : attributes.keySet()) {
				sb.append(attribute).append("=").append(attributes.get(attribute)).append(" ");
			}
			return sb.toString();
		}
	}

	public static class ExtendedFeatureBasedEntity implements FeatureBasedEntity<Binding>, ChildesConstants {

		protected Element element = null;
		protected String type = null;
		protected String id = null;
		protected String category = null;
		protected Integer spanLeft = null;
		protected Integer spanRight = null;
		private MapSet<String, Binding> bindings = new MapSet<String, Binding>(
				new LinkedHashMapFactory<String, Set<Binding>>(), new LinkedHashSetFactory<Binding>());
		protected Map<String, String> attributes = new LinkedHashMap<String, String>();

		public ExtendedFeatureBasedEntity(Element element) {
			this.element = element;
			type = element.getName();

			for (Attribute attribute : (List<Attribute>) element.getAttributes()) {
				if (attribute.getName().equals(ChildesConstants.CATEGORY)) {
					category = attribute.getValue();
				}
				else if (attribute.getName().equals(ChildesConstants.ID)) {
					id = attribute.getValue();
				}
				else if (attribute.getName().equals(ChildesConstants.SPAN_LEFT)) {
					spanLeft = Integer.valueOf(attribute.getValue());
				}
				else if (attribute.getName().equals(ChildesConstants.SPAN_RIGHT)) {
					spanRight = Integer.valueOf(attribute.getValue());
				}
				else {
					attributes.put(attribute.getName(), attribute.getValue());
				}
			}

			List<Element> xmlRoles = (List<Element>) element.getContent(new ElementFilter(BINDING));
			for (Element e : xmlRoles) {
				Binding binding = new Binding(e);
				if (binding.getField() != null) {
					bindings.put(binding.getField(), binding);
				}
			}

		}

		public String getID() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getCategory() {
			return category;
		}

		public Integer getSpanLeft() {
			return spanLeft;
		}

		public Integer getSpanRight() {
			return spanRight;
		}

		public Element getJDOMElement() {
			return element;
		}

		public void setSpan(int spanLeft, int spanRight) {
			this.spanLeft = spanLeft;
			this.spanRight = spanRight;
		}

		public String getAttributeValue(String attribute) {
			return attributes.get(attribute);
		}

		public Set<String> getAttributes() {
			return attributes.keySet();
		}

		public Set<Binding> getBinding(String role) {
			return bindings.get(role);
		}

		public Set<Binding> getAllBindings() {
			Set<Binding> allBindings = new HashSet<Binding>();
			for (Set<Binding> b : bindings.values()) {
				allBindings.addAll(b);
			}
			return allBindings;
		}

		public Set<String> getRoles() {
			return bindings.keySet();
		}

		public String toString() {
			return ChildesTranscript.getFormatter().format(this);
		}
	}
}
