// TextFileLineIterator.java

package compling.util.fileutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import compling.grammar.ecg.ECGConstants;

/**
 * A simple class to turn a text file into a string iterator (line by line).
 * 
 * Each call to next() or hasNext() returns a line of the file, ignoring blank lines.
 * 
 * Unfortunately, there is a bug in this code. The class gets unhappy when there are multiples blank lines at the end of
 * the file. I think it's easy to fix, but I haven't yet had the time.
 * 
 * @author John Bryant
 */

public class TextFileLineIterator implements Iterator<String> {

	private String nextLine = null;
	private BufferedReader br;

	/**
	 * Constructs a line-by-line iterator for the file, which is interpreted according to the ECG file encoding.
	 * 
	 * @param path
	 *           Path to the file.
	 * @throws IOException
	 * @see compling.grammar.ecg.ECGConstants#DEFAULT_ENCODING
	 */
	public TextFileLineIterator(String path) throws IOException {
		this(path, ECGConstants.DEFAULT_ENCODING);
	}

	/**
	 * Constructs a line-by-line iterator for the file, which is interpreted according to the provided encoding.
	 * 
	 * @param path
	 *           Path to the file.
	 * @param encoding
	 *           Desired encoding. If null or empty, the system default encoding will be used.
	 * @throws IOException
	 */
	public TextFileLineIterator(String path, String encoding) throws IOException {
		if (encoding == null || encoding.equals(""))
			// System default encoding
			br = new BufferedReader(new FileReader(path));
		else
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path), encoding));
		setupNextLine();
	}

	/**
	 * Constructs a line-by-line iterator for the file, which is interpreted according to the ECG file encoding.
	 * 
	 * @param file
	 * @see compling.grammar.ecg.ECGConstants#DEFAULT_ENCODING
	 */
	public TextFileLineIterator(File file) {
		this(file, ECGConstants.DEFAULT_ENCODING);
	}

	/**
	 * Constructs a line-by-line iterator for the file, which is interpreted according to the provided encoding.
	 * 
	 * @param file
	 * @param encoding
	 *           Desired encoding. If null or empty, the system default encoding will be used.
	 */
	public TextFileLineIterator(File file, String encoding) {
		try {
			if (encoding == null || encoding.equals(""))
				// System default encoding
				br = new BufferedReader(new FileReader(file));
			else
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
			setupNextLine();
		}
		catch (IOException ioe) {
			throw new IllegalArgumentException(ioe);
		}
	}

	/**
	 * Constructs a line-by-line iterator for the buffer.
	 * 
	 * @param buffer
	 */
	public TextFileLineIterator(StringBuffer buffer) {
		try {
			br = new BufferedReader(new StringReader(buffer.toString()));
			setupNextLine();
		}
		catch (IOException ioe) {
			throw new IllegalArgumentException(ioe);
		}
	}

	public boolean hasNext() {
		return nextLine != null;
	}

	public String next() throws NoSuchElementException {
		if (!hasNext())
			throw new NoSuchElementException();
		String line = nextLine;
		try {
			setupNextLine();
		}
		catch (IOException ioe) {
			throw new NoSuchElementException("cannot set up next element");
		}
		return line;
	}

	public void remove() {
		throw new UnsupportedOperationException("TextFileLineIterator " + "does not support the remove method");
	}

	protected void setupNextLine() throws IOException {
		nextLine = br.readLine();
		if (nextLine == null) {
			br.close();
		}
		else {
			nextLine = nextLine.trim();
		}
		while (nextLine != null && nextLine.equals("")) {
			setupNextLine();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println(">> la niña salio de la clase");

		TextFileLineIterator tfli = new TextFileLineIterator(args[0], ECGConstants.DEFAULT_ENCODING);
		while (tfli.hasNext()) {
			System.out.println(tfli.next());
		}
	}
}
