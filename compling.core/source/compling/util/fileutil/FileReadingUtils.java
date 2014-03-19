// =============================================================================
// File : FileReadingUtils.java
// Author : emok
// Change Log : Created on Oct 22, 2006
// =============================================================================

package compling.util.fileutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

// =============================================================================

public class FileReadingUtils {

	public static StringBuffer ReadFileIntoStringBuffer(File file) throws IOException {
		return ReadFileIntoStringBuffer(file, Charset.defaultCharset());
	}

	public static StringBuffer ReadFileIntoStringBuffer(File file, Charset charSet) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charSet));
		StringBuffer sb = new StringBuffer();
		while (reader.ready()) {
			sb.append(reader.readLine() + "\n");
		}
		return sb;
	}

	public static List<String> ReadFileIntoList(File file) throws IOException {
		return ReadFileIntoList(file, Charset.defaultCharset());
	}

	public static List<String> ReadFileIntoList(File file, Charset charSet) throws IOException {
		List<String> fileContent = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charSet));
		while (reader.ready()) {
			fileContent.add(reader.readLine());
		}
		return fileContent;
	}

}
