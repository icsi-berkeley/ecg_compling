package compling.gui.grammargui.builder;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;

import java_cup.runtime.Symbol;

import org.eclipse.core.resources.IFile;

import compling.context.MiniOntologyReader;
import compling.context.Yylex;
import compling.grammar.ecg.ecgreader.IErrorListener;
import compling.grammar.ecg.ecgreader.IErrorListener.Severity;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.gui.grammargui.model.PrefsManager;
import compling.gui.grammargui.util.ISpecificationReader;

public class OntologyReader extends MiniOntologyReader implements ISpecificationReader {

	private IErrorListener errorListener;

	public OntologyReader() {
		super();
	}

	@Override
	public Object read(IFile from, IErrorListener listener) throws Exception {
		this.errorListener = listener;

		Charset encoding = Charset.forName(from.getCharset());
		Yylex scanner = new Yylex(new InputStreamReader(from.getContents(), encoding));
		scanner.file = file = from.getName();
		setScanner(scanner);

		Symbol result = null;
		try {
			result = parse();
			Collection<? extends TypeSystemNode> types = m.getTypeSystem().getAllTypes();
			PrefsManager.instance().updateNodeMap(types, from);
		}
		catch (Exception e) {
			errorListener.notify(e.getMessage(), scanner.getLocation(), Severity.FATAL);
		}
		return result;
	}

	@Override
	public void report_error(String message, Object info) {
		errorListener.notify(message, ((Yylex) getScanner()).getLocation(), Severity.ERROR);
	}

}
