package compling.util.fileutil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Accepts files based on a list of extensions.
 * 
 * @author John Bryant
 */
public class ExtensionFileFilter implements FileFilter {

	List<String> extensions;
	boolean recurse = true;

	public boolean accept(File pathname) {
		if (pathname.isDirectory()) {
			return recurse;
		}
		String name = pathname.getName();
		for (String extension : extensions) {
			if (name.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	public ExtensionFileFilter(String extensions) {
		StringTokenizer st = new StringTokenizer(extensions);
		this.extensions = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			this.extensions.add(st.nextToken());
		}
	}

	public ExtensionFileFilter(List<String> extensions) {
		this.extensions = extensions;
	}
}
