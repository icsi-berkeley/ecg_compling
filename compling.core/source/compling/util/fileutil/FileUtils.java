package compling.util.fileutil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains static functions for interacting with files.
 * 
 * @author John Bryant
 */
public class FileUtils {

	public static List<File> getFilesUnder(String path, FileFilter fileFilter) {
		return getFilesUnder(new File(path), fileFilter);
	}

	public static List<File> getFilesUnder(File root, FileFilter fileFilter) {
		List<File> files = new ArrayList<File>();
		addFilesUnder(root, files, fileFilter);
		return files;
	}

	private static void addFilesUnder(File root, List<File> files, FileFilter fileFilter) {
		if (!fileFilter.accept(root))
			return;
		if (root.isFile()) {
			files.add(root);
			return;
		}
		if (root.isDirectory()) {
			File[] children = root.listFiles();
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				addFilesUnder(child, files, fileFilter);
			}
		}
	}

	public static List<File> getFilesUnder(File parent, List<String> paths, FileFilter fileFilter) {
		List<File> files = new ArrayList<File>();

		List<File> absolutePaths = new ArrayList<File>();
		for (String pathName : paths) {
			File absOrRel = new File(pathName);
			if (absOrRel.isAbsolute()) {
				absolutePaths.add(absOrRel);
			}
			else {
				absolutePaths.add(new File(parent, pathName));
			}
		}

		for (File fileOrDir : absolutePaths) {
			// if (fileOrDir.isDirectory()) {
			files.addAll(FileUtils.getFilesUnder(fileOrDir, fileFilter));
			// } else {
			// files.add(fileOrDir);
			// }
		}
		return files;
	}

}
