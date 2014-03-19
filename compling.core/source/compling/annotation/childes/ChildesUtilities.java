// =============================================================================
//File        : ChildesUtilities.java
//Author      : emok
//Change Log  : Created on Aug 24, 2005
//=============================================================================

package compling.annotation.childes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import compling.annotation.childes.ChildesAnnotation.GoldStandardAnnotation;
import compling.annotation.childes.ChildesAnnotation.GoldStandardTier;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesTranscript.ChildesEvent;
import compling.annotation.childes.ChildesTranscript.ChildesItem;
import compling.annotation.childes.FeatureBasedEntity.Binding;
import compling.annotation.childes.FeatureBasedEntity.ExtendedFeatureBasedEntity;
import compling.util.Counter;
import compling.utterance.Word;

//=============================================================================

/**
 * A collection of statistics and formatting utilities for the Childes corpus.
 * 
 * @author emok
 */
public class ChildesUtilities implements ChildesConstants {

	public static class ChildesStatistics extends Counter<Word> {

		private static final long serialVersionUID = -3183470402135512709L;

		public ChildesStatistics() {
			super();
		}

		public void ProcessClause(ChildesClause clause) {
			List<Word> text = clause.getElements();

			for (Word w : text) {
				incrementCount(w, 1);
			}
		}

		public void ProcessClause(ChildesTranscript transcript) {
			ProcessClause(transcript, null);
		}

		public void ProcessClause(ChildesTranscript transcript, ChildesFilter filter) {

			ChildesIterator iterator = transcript.iterator();
			if (filter != null) {
				iterator.setFilter(filter);
			}

			while (iterator.hasNext()) {
				ChildesItem item = iterator.next();

				if (item instanceof ChildesClause) {
					ChildesClause clause = (ChildesClause) item;
					ProcessClause(clause);
				}
			}
		}

		public List<Word> getSortedList() {
			Set<Word> set = keySet();
			ArrayList<Word> list = new ArrayList<Word>(set);
			Collections.sort(list);
			return list;
		}

		public String toString() {
			return ChildesTranscript.getFormatter().format(this);
		}

	}

	/**
	 * A formatter implementing this interface is used by the <code>toString</code> methods for
	 * <code>ChildesTranscript</code>, <code>ChildesClause</code>, <code>ChildesAnnotation</code>, and
	 * <code>ChildesStatistics</code>.
	 * 
	 * @author emok
	 */
	public static interface ChildesFormatter {

		public String format(ChildesClause clause);

		public String format(ChildesTranscript transcript);

		public String format(ChildesStatistics statistics);

		public String format(ChildesAnnotation annotation);

		// public String format (ChildesEvent event);

		public <T> String format(FeatureBasedEntity<T> fbe);

		public String format(GoldStandardTier tier);

		public String format(GoldStandardAnnotation annotation);

		public String format(ExtendedFeatureBasedEntity entity);

		/*
		 * public String format (SpeechActTier tier);
		 * 
		 * public String format (TranslationTier tier);
		 * 
		 * public String format (VernacularTier tier);
		 */

	}

	public static class HTMLChildesFormatter implements ChildesFormatter {

		public String format(ChildesClause clause) {

			StringBuffer sb = new StringBuffer("<html>");
			List<Word> text = clause.getElements();
			for (Word w : text) {
				sb.append(w.getOrthography());
				sb.append(DEFAULT_SEPERATOR);
			}
			sb.append("</html>");
			return sb.toString();
		}

		public String format(ChildesTranscript transcript) {
			StringBuffer sb = new StringBuffer("<html>");

			for (ChildesItem item : transcript) {
				sb.append("<p>");
				if (item instanceof ChildesClause) {
					sb.append(format((ChildesClause) item));
				}
				else if (item instanceof ChildesEvent) {
					sb.append(format((ChildesEvent) item));
				}
				sb.append("</p>");
			}
			sb.append("</html>");

			return sb.toString();
		}

		public String format(ChildesStatistics statistics) {
			StringBuffer sb = new StringBuffer("<html><table>");

			List<Word> words = statistics.getSortedList();
			for (Word w : words) {
				sb.append("<tr><td>");
				sb.append(w.getOrthography());
				sb.append("</td><td>");
				sb.append(statistics.getCount(w));
				sb.append("</td></tr>");
			}
			sb.append("</table></html>");
			return sb.toString();
		}

		public String format(ChildesAnnotation annotation) {
			StringBuffer sb = new StringBuffer("<html>");
			sb.append("</html>");
			return sb.toString();
		}

		public <T> String format(FeatureBasedEntity<T> fbe) {
			StringBuffer sb = new StringBuffer("<html>");
			/*
			 * sb.append("<table><tr><td colspan=2>"); sb.append(fbe.getCategory()); sb.append("</td></tr>");
			 * 
			 * for (String role : fbe.getRoles()) { sb.append("<td>"); sb.append(role); sb.append("</td><td>"); for
			 * (Pair<FillerType, String> filler : fbe.getFiller(role)) { sb.append(); } sb.append("</tr>"); }
			 * sb.append("</table>");
			 */
			sb.append("</html>");
			return sb.toString();
		}

		public String format(GoldStandardTier tier) {
			return format(tier.getContent());
		}

		public String format(GoldStandardAnnotation annotation) {
			return "";
		}

		public String format(ExtendedFeatureBasedEntity efbe) {
			return "";
		}
	}

	public static class TextChildesFormatter implements ChildesFormatter {

		public String format(ChildesClause clause) {
			StringBuffer sb = new StringBuffer();
			List<Word> text = clause.getElements();
			for (Word w : text) {
				sb.append(w.getOrthography());
				sb.append(DEFAULT_SEPERATOR);
			}
			return sb.toString().trim();
		}

		/*
		 * public String format(ChildesEvent event) { StringBuffer sb = new StringBuffer();
		 * 
		 * sb.append(event.getCategory()); sb.append("\n");
		 * 
		 * for (String role : event.getRoles()) { sb.append("\t"); sb.append(role); sb.append(" <--> ");
		 * sb.append(event.getFiller(role)); sb.append("\n"); }
		 * 
		 * return sb.toString(); }
		 */

		public String format(ChildesTranscript transcript) {
			StringBuffer sb = new StringBuffer();
			for (ChildesItem item : transcript) {
				if (item instanceof ChildesClause) {
					sb.append(format((ChildesClause) item));
				}
				else if (item instanceof ChildesEvent) {
					sb.append(format((ChildesEvent) item));
				}
				sb.append("\n");

			}
			return sb.toString();
		}

		public String format(ChildesStatistics statistics) {
			StringBuffer sb = new StringBuffer();

			List<Word> words = statistics.getSortedList();
			for (Word w : words) {
				sb.append(w.getOrthography());
				sb.append("\t");
				sb.append(statistics.getCount(w));
				sb.append("\n");
			}
			return sb.toString();
		}

		public String format(ChildesAnnotation annotation) {
			String s = "";
			return s;
		}

		public <T> String format(FeatureBasedEntity<T> fbe) {
			StringBuffer sb = new StringBuffer();
			if (fbe.getID() != null) {
				sb.append(fbe.getID());
			}
			else {
				sb.append("anonymous");
			}
			if (fbe.getCategory() != null) {
				sb.append(" cat=").append(fbe.getCategory());
			}
			sb.append("\n");
			for (String field : fbe.getRoles()) {
				Set<T> fillers = fbe.getBinding(field);
				sb.append(field).append(" : ");
				for (T filler : fillers) {
					/*
					 * sb.append(filler.getSecond()).append("@"); if (filler.getFirst() == FillerType.REFERENCE) {
					 * sb.append("REF"); } else { sb.append("VAL"); }
					 */
					sb.append(filler);
					sb.append(", ");
				}
				sb.delete(sb.length() - 2, sb.length());
				sb.append("\n");
			}
			return sb.toString();
		}

		public String format(GoldStandardTier tier) {
			return format(tier.getContent());
		}

		public String format(GoldStandardAnnotation annotation) {
			StringBuffer sb = new StringBuffer();

			for (ExtendedFeatureBasedEntity fbe : annotation.getAllAnnotations()) {
				sb.append(format(fbe));
				sb.append("\n");
			}
			return sb.toString();
		}

		public String format(ExtendedFeatureBasedEntity efbe) {
			StringBuffer sb = new StringBuffer();
			if (efbe.getID() != null) {
				sb.append(efbe.getID());
			}
			else {
				sb.append("anonymous");
			}
			if (efbe.getCategory() != null) {
				sb.append(" cat=").append(efbe.getCategory());
			}
			if (efbe.getSpanLeft() != null) {
				sb.append(" (").append(efbe.getSpanLeft());
			}
			if (efbe.getSpanRight() != null) {
				sb.append(",").append(efbe.getSpanRight()).append(")");
			}
			sb.append(": ");
			for (String attribute : efbe.getAttributes()) {
				sb.append(attribute).append("=").append(efbe.getAttributeValue(attribute)).append(" ");
			}
			sb.append("\n");
			for (String role : efbe.getRoles()) {
				for (Binding binding : efbe.getBinding(role)) {
					sb.append(format(binding)).append("\n");
				}
			}
			return sb.toString();
		}

		public String format(Binding binding) {
			StringBuffer sb = new StringBuffer();
			sb.append(binding.getField());
			if (binding.getSpanLeft() != null) {
				sb.append(" (").append(binding.getSpanLeft());
			}
			if (binding.getSpanRight() != null) {
				sb.append(",").append(binding.getSpanRight()).append(")");
			}
			sb.append(": ");
			for (String attribute : binding.getAttributes()) {
				sb.append(attribute).append("=").append(binding.getAttributeValue(attribute)).append(" ");
			}
			return sb.toString();
		}
	}
}