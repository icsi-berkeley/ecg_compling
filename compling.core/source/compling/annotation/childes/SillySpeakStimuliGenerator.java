// =============================================================================
// File        : SillySpeakStimuliGenerator.java
// Author      : emok
// Change Log  : Created on May 31, 2008
//=============================================================================

package compling.annotation.childes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.annotation.AnnotationException;
import compling.util.Pair;
import compling.util.Triplet;
import compling.util.fileutil.TextFileLineIterator;

//=============================================================================

public class SillySpeakStimuliGenerator {

	public class Scene {
		Vocabulary lexicon = null;

		boolean isTransitive = false;
		boolean isNegated = false;
		int sceneType = 0;
		int arg1Type = 0;
		int arg2Type = 0;

		public Scene(boolean isTransitive, boolean isNegated, int sceneType, int arg1Type, int arg2Type,
				Vocabulary lexicon) {
			this.lexicon = lexicon;
			this.isTransitive = isTransitive;
			this.isNegated = isNegated;
			this.sceneType = sceneType;
			this.arg1Type = arg1Type;
			this.arg2Type = arg2Type;
		}

		public String toString() {
			return toString(0);
		}

		public String toString(int indent) {
			Pair<String, String> scene = lexicon.getVerb(sceneType);
			Pair<String, String> arg1 = lexicon.getNoun(arg1Type);
			String arg1SemType = arg1.getSecond().charAt(0) != '@' ? arg1.getSecond() : arg1.getSecond().substring(1);
			String arg1ID = arg1SemType.toLowerCase();
			Pair<String, String> arg2 = null;
			String arg2SemType = null, arg2ID = null;
			if (isTransitive) {
				arg2 = lexicon.getNoun(arg2Type);
				arg2SemType = arg2.getSecond().charAt(0) != '@' ? arg2.getSecond() : arg2.getSecond().substring(1);
				arg2ID = arg2SemType.toLowerCase();
			}

			String blockIndent = makeIndent(indent);
			String addnIndent = makeIndent(1);
			StringBuffer sb = new StringBuffer();

			sb.append(blockIndent).append("<Setting>\n");
			sb.append(blockIndent).append(addnIndent).append("<entity cat=\"").append(arg1SemType).append("\" id=\"")
					.append(arg1ID).append("\">\n");
			if (isTransitive) {
				sb.append(blockIndent).append("<entity cat=\"").append(arg2SemType).append(" id=\"").append(arg2ID)
						.append("\">\n");
			}
			sb.append(blockIndent).append("</Setting>\n\n");

			sb.append(blockIndent).append("<event cat=\"").append(scene.getSecond());
			sb.append("\" id=\"").append(scene.getSecond().toLowerCase()).append("01\">\n");
			sb.append(blockIndent).append(addnIndent).append("<binding field=\"protagonist\" ref=\"").append(arg1ID)
					.append("\">\n");
			if (isTransitive) {
				sb.append(blockIndent).append(addnIndent).append("<binding field=\"protagonist2\" ref=\"").append(arg1ID)
						.append("\">\n");
			}
			sb.append(blockIndent).append("</event>\n\n");

			sb.append(blockIndent).append("<u who=\"INV\" id=\"01\">\n");
			sb.append(blockIndent).append(addnIndent).append("<clause>\n");
			sb.append(blockIndent).append(addnIndent).append(addnIndent); // (neg) V S det-S (O det-O)
			if (isNegated) {
				sb.append("<w>").append(lexicon.getNeg()).append("</w>");
			}
			sb.append("<w>").append(scene.getFirst()).append("</w>");
			sb.append("<w>").append(arg1.getFirst()).append("</w>");
			sb.append("<w>").append(lexicon.isClass1(arg1Type) ? lexicon.getDets(1) : lexicon.getDets(2)).append("</w>");
			if (isTransitive) {
				sb.append("<w>").append(arg2.getFirst()).append("</w>");
				sb.append("<w>").append(lexicon.isClass1(arg2Type) ? lexicon.getDets(1) : lexicon.getDets(2))
						.append("</w>");
			}
			sb.append("\n");
			sb.append(blockIndent).append(addnIndent).append("</clause>\n");
			sb.append(blockIndent).append("</u>\n\n");
			return sb.toString();
		}

		private String makeIndent(int indent) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < indent; i++) {
				sb.append("\t");
			}
			return sb.toString();
		}
	}

	public class Vocabulary {

		private Set<Integer> class1Nouns = new HashSet<Integer>();
		private Set<Integer> class2Nouns = new HashSet<Integer>();

		private Map<Integer, Pair<String, String>> verbs = new LinkedHashMap<Integer, Pair<String, String>>();
		private Map<Integer, Pair<String, String>> nouns = new LinkedHashMap<Integer, Pair<String, String>>();
		private Map<Integer, String> dets = new LinkedHashMap<Integer, String>();
		private String neg = "";

		private int linenum = 1;

		public Vocabulary(TextFileLineIterator vocabularyIterator) {
			SillySpeakGrammaticalCategories currentBlock = SillySpeakGrammaticalCategories.DET;
			while (vocabularyIterator.hasNext()) {
				String line = vocabularyIterator.next();
				line = line.trim();
				if (line == "")
					continue;
				if (line.startsWith("//"))
					continue;
				if (line.startsWith("BEGIN BLOCK")) {
					currentBlock = SillySpeakGrammaticalCategories.valueOf(line.replaceAll("BEGIN BLOCK", "").trim());
				}
				else if (line.startsWith("END BLOCK")) {
					currentBlock = null;
				}
				else if (line.startsWith("CLASS1 MEMBERS:")) {
					String[] members = line.replace("CLASS1 MEMBERS:", "").split("\\t");
					for (String member : members) {
						class1Nouns.add(Integer.valueOf(member.trim()));
					}
				}
				else if (line.startsWith("CLASS2 MEMBERS:")) {
					String[] members = line.replace("CLASS2 MEMBERS:", "").split("\\t");
					for (String member : members) {
						class2Nouns.add(Integer.valueOf(member.trim()));
					}
				}
				else {
					Triplet<Integer, String, String> word = extractWord(linenum, line);
					switch (currentBlock) {
					case DET:
						dets.put(word.getFirst(), word.getSecond());
						break;
					case NEG:
						neg = word.getSecond();
						break;
					case NOUN:
						nouns.put(word.getFirst(), new Pair<String, String>(word.getSecond(), word.getThird()));
						break;
					case VERB:
						verbs.put(word.getFirst(), new Pair<String, String>(word.getSecond(), word.getThird()));
						break;
					default:
						throw new AnnotationException("No blocks currently defined");
					}
				}
				linenum++;
			}
		}

		public Triplet<Integer, String, String> extractWord(int linenum, String line) {
			String[] parts = line.split("\\t");
			if (parts.length < 2) {
				throw new AnnotationException("Sillyspeak vocab not in the correct format on line " + linenum + ": " + line);
			}
			Integer num = Integer.valueOf(parts[0].trim());
			return parts.length == 3 ? new Triplet<Integer, String, String>(num, parts[1], parts[2])
					: new Triplet<Integer, String, String>(num, parts[1], "");
		}

		public boolean isClass1(int nounID) {
			return class1Nouns.contains(nounID);
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Verbs:").append(verbs.values());
			sb.append("Nouns:").append(nouns.values());
			sb.append("Dets:").append(dets);
			sb.append("Neg:").append(neg);
			return sb.toString();
		}

		public Pair<String, String> getVerb(int verbID) {
			return verbs.get(verbID);
		}

		public Pair<String, String> getNoun(int nounID) {
			return nouns.get(nounID);
		}

		public String getDets(int detID) {
			return dets.get(detID);
		}

		public String getNeg() {
			return neg;
		}
	}

	public class Stimuli {
		List<Scene> scenes = new ArrayList<Scene>();
		Vocabulary lexicon = null;

		public Stimuli(TextFileLineIterator stimuliIterator, Vocabulary lexicon) {
			this.lexicon = lexicon;
			int linenum = 1;
			while (stimuliIterator.hasNext()) {
				boolean negated = false;
				String line = stimuliIterator.next();
				line = line.trim();
				if (line == "")
					continue;
				if (line.startsWith("//"))
					continue;
				if (line.startsWith("n")) {
					negated = true;
					line = line.replaceFirst("n", "").trim();
				}
				String[] parts = line.split("\\t");
				if (parts.length == 2) { // intransitive scene
					Scene scene = new Scene(false, negated, Integer.valueOf(parts[0].trim()), Integer.valueOf(parts[1]
							.trim()), 0, lexicon);
					scenes.add(scene);
				}
				else if (parts.length == 3) { // transitive scene
					Scene scene = new Scene(true, negated, Integer.valueOf(parts[0].trim()),
							Integer.valueOf(parts[1].trim()), Integer.valueOf(parts[2].trim()), lexicon);
					scenes.add(scene);
				}
				else {
					throw new AnnotationException("Unsupported number of arguments in a scene: " + linenum + ": " + line);
				}
				linenum++;
			}
		}

		public void generateTranscripts(String transcriptPath) {
			for (Scene scene : scenes) {
				System.out.println(scene.toString(1));
			}
		}
	}

	public enum SillySpeakGrammaticalCategories {
		VERB, NOUN, DET, NEG;
	}

	public SillySpeakStimuliGenerator(String vocabPath, String stimuliPath, String transcriptPath) throws IOException {
		Vocabulary lexicon = new Vocabulary(new TextFileLineIterator(vocabPath));
		Stimuli stimuli = new Stimuli(new TextFileLineIterator(stimuliPath), lexicon);
		stimuli.generateTranscripts(transcriptPath);
	}

	public static void main(String[] args) throws IOException {
		SillySpeakStimuliGenerator generator = new SillySpeakStimuliGenerator(args[0], args[1], args[2]);
	}
}
