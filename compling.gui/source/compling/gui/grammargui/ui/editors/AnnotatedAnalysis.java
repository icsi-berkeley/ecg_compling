package compling.gui.grammargui.ui.editors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TableViewer;

import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.gui.grammargui.Application;
import compling.gui.grammargui.util.HtmlFeatureStructureFormatter;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.TextEmitter;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;

public class AnnotatedAnalysis {
	
	protected static final String CSS_ENTRY = "css";
	protected static final String JS_ENTRY = "js";
	protected static final String ROOT_ENTRY = "/";

	protected String cssURL;
	protected String jsURL;
	protected String rootURL;
	protected TextEmitter emitter;
	
	public AnnotatedAnalysis() {
		initData();
	}
	
	
	protected void initData() {
		emitter = new TextEmitter(1);
		//formatter = new HtmlFeatureStructureFormatter(emitter);
	
		URL cssEntry = Platform.getBundle(Application.PLUGIN_ID).getEntry(CSS_ENTRY);
		URL jsEntry = Platform.getBundle(Application.PLUGIN_ID).getEntry(JS_ENTRY);
		URL rootEntry = Platform.getBundle(Application.PLUGIN_ID).getEntry(ROOT_ENTRY);

		try {
			this.cssURL = FileLocator.toFileURL(cssEntry).toExternalForm();
		}
		catch (IOException e) {
			Log.logError(e, "Impossible to retrieve %s", CSS_ENTRY);
			this.cssURL = CSS_ENTRY;
		}
		try {
			this.jsURL = FileLocator.toFileURL(jsEntry).toExternalForm();
		}
		catch (IOException e) {
			Log.logError(e, "Impossible to retrieve %s", JS_ENTRY);
			this.jsURL = JS_ENTRY;
		}
		try {
			this.rootURL = FileLocator.toFileURL(rootEntry).toExternalForm();
		}
		catch (IOException e) {
			Log.logError(e, "Impossible to retrieve %s", ROOT_ENTRY);
			this.rootURL = "/";
		}
	}
	
	protected void emitHtmlPrologue(String sentence) {
		emitter.sayln(0,
				"<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>");
		emitter.sayln(1, String.format("<title>Semantic Specification: %s</title>", sentence));
		emitter.sayln(1, "<base href='%s'>", rootURL);
		// emitter.sayln(1,
		// "<link rel=StyleSheet href='%ssemspec.css' type='text/css' MEDIA=screen>",
		emitter.sayln(1, "<link rel=StyleSheet href='%ssemspec.css' type='text/css'>", cssURL);
		emitter.sayln(1, "<script src='%sprototype.js' type='text/javascript'></script>", jsURL);
		emitter.sayln(1, "<script src='%sscriptaculous.js' type='text/javascript'></script>", jsURL);
		emitter.sayln(1, "<script src='%ssemspec.js' type='text/javascript'></script>", jsURL);
		// emitter.sayln(0,
		// "</head><body onLoad='do_highlighting()' onContextMenu='return false'>");
		emitter.sayln(0, "</head><body onLoad='do_highlighting()'>");
	}
	
	protected void emitHtmlEpilogue() {
		emitter.sayln(0, "</body></html>");
	}
	
	public String getAnnotatedText(String sentence, Analysis a) {
		emitHtmlPrologue(sentence);
		
		
		ArrayList<LinkedHashMap<String, String>> annotated = annotate(a.getSpans(), sentence, a);
		emitter.sayln(annotated.toString());
		
		emitHtmlEpilogue();
		
		return emitter.getOutput();
	}
	
	public ArrayList<LinkedHashMap<String, String>> annotate(List<CxnalSpan> spans, String sentence, Analysis a) {
		
		LinkedHashMap<String, String> wordMap = new LinkedHashMap<String, String>();
		
		FeatureStructureSet fss = a.getFeatureStructure();
		String annotated = "";
		String[] split = sentence.split(" ");
		
		for (int i=0; i<split.length; i++) {
			String spanText = "";
			for (CxnalSpan span : spans) {
				//System.out.println(span.)
				if (span.left == i && span.right == i+1) {
					wordMap.put(split[i], span.getType().getName());
				}
			}
			annotated += split[i] + ": " + spanText + " \n";
			//map.put(split[i], null);
		}
		
		ArrayList<LinkedHashMap<String, String>> allAnnotations = new ArrayList<LinkedHashMap<String, String>>();
		allAnnotations.add(wordMap);
		return allAnnotations;
	}
	
	public String formatMap(LinkedHashMap<String, String> slot) {
		return null;
	}

}
