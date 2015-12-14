package compling.gui.util;

import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.AnalysisUtilities.AnalysisFormatter;

public class AnnotationAnalysisFormatter implements AnalysisFormatter {
	
	private Analysis analyses;
	private StringBuilder code;
	
	protected void emit(String s) {
		code.append(s);
	}

	@Override
	public String format(Analysis a) {
		code.append(a.toString());
		return code.toString();
	}

}
