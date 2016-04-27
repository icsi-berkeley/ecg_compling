package compling.gui.grammargui.ui.editors;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

import compling.gui.grammargui.Application;
import compling.gui.grammargui.util.HtmlFeatureStructureFormatter;
import compling.gui.grammargui.util.Log;
import compling.gui.grammargui.util.TextEmitter;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Arrays;

public class AnalysisHtmlBuilder {
	protected static final String CSS_ENTRY = "css";
	protected static final String JS_ENTRY = "js";
	protected static final String ROOT_ENTRY = "/";

	protected String cssURL;
	protected String jsURL;
	protected String rootURL;
	protected TextEmitter emitter;
	protected HtmlFeatureStructureFormatter formatter;

	public AnalysisHtmlBuilder() {
		initData();
	}

	protected void initData() {
		emitter = new TextEmitter(1);
		formatter = new HtmlFeatureStructureFormatter(emitter);
	
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
	
	protected HashMap<String, String> matchSpansToText(String sentence, Analysis a) {
		
		HashMap<String, String> spansToText = new HashMap<String, String>();
		List<CxnalSpan> spans = a.getSpans();
		
		List<String> split_test = Arrays.split(sentence);
		//System.out.println(split_test);
		
		//String[] split = sentence.replace("?", " ? ").replace("!", " ! ").replace(".", " . ").replace(",", " , ").trim().split(" ");
		
		//String[] split = sentence.replace("", " ").trim().split(" ");

		for (CxnalSpan span : spans) {
			if (span.getType() != null) {
				String text = "";
				String type = span.getType().getName() + "[" + span.getSlotID()+ "]";
				//System.out.println(type);
				int left = span.left;
				int right = span.right;
				for (int index=left; index<right; index++) {
					text += split_test.get(index) + " ";
					//System.out.println(text);
				}
				spansToText.put(type, text);
			}
		}
		
		
		
		return spansToText;
	}

	protected String getHtmlText(String sentence, Analysis a) {
		emitter.reset();
		
		HashMap<String, String> spansToText = matchSpansToText(sentence, a);
	
		emitHtmlPrologue(sentence);
		formatter.format(a.getFeatureStructure(), spansToText);
		emitHtmlEpilogue();
	
		return emitter.getOutput();
	}

}