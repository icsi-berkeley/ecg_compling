package compling.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class PackageHandler {
	
	// A map of package relations. The key is the package imported by the packages in the array list.
	// E.g., if 'A' imports 'B', it'll look like: {'B': ['A',...]}
	protected HashMap<String, ArrayList<String>> packageRelations =
			new HashMap<String, ArrayList<String>>();
	
	// The list of packages actually declared as part of the grammar. These include all of the packages
	// in the PACKAGE_NAME field in the prefs file, as well as all packages imported by those packages.
	// E.g., if "starter" is in PACKAGE_NAME, and "starter" imports 'B' (found in packageRelations),
	// 'B' will be added to declaredPackages.
	protected ArrayList<String> declaredPackages = new ArrayList<String>(){{
		add("global");
	}};
	

	public String addPackageRelation(String importName) {
		if (packageRelations.containsKey(importName)) {
			if (!packageRelations.get(importName).contains(getPackage())) {
				packageRelations.get(importName).add(getPackage());
			}
		} else {
			packageRelations.put(importName, new ArrayList<String>());
			packageRelations.get(importName).add(getPackage());
		}
		return importName;
	}
	
	public HashMap<String, ArrayList<String>> getPackageRelations() {
		return packageRelations;
	}
	
	public ArrayList<String> getDeclaredPackages() {
		return declaredPackages;
	}
	
	public void sortDeclaredPackages() {
		for (Entry<String, ArrayList<String>> pkg : packageRelations.entrySet()) {
			for (String value : pkg.getValue()) {
				if (declaredPackages.contains(value) &&
						!declaredPackages.contains(pkg.getKey())) {
					declaredPackages.add(pkg.getKey());
				}
			}
		}
	}
	
	public void addToDeclared(String packageName) {
		declaredPackages.add(packageName);
	}
	
	public void addRelations(HashMap<String, ArrayList<String>> relationSet) {
		packageRelations.putAll(relationSet);
	}
	
	
	// OLD STUFF
	
	private String pkg = new String("global");
	private ArrayList<String> pkgs = new ArrayList<String>(){{
		add(new String("global"));
	}};
	private String importRequest = "global";
	private ArrayList<String> importRequests = new ArrayList<String>(){{
		add(new String("global"));
	}};

	/** Sets field for package. */
	public String setPackage(String packageName) {
		pkg = new String(packageName);
		return pkg;
	}
	
	public String getPackage() {
		return pkg;
	}
	
	
	public List<String> getPackages() {
		return pkgs;
	}
	
	public String addPackage(String named) {
		String pkgName = new String(named);
		if (!pkgs.contains(pkgName)) {
			pkgs.add(pkgName);
		}
		return named;
	}
	
	/** Adds import to import list, returns import name. */
	public String addImport(String imported) {
		String importName = new String(imported);
		String p = getPackage();
		if (!importRequests.contains(importName)) {
				//&& getImport().contains(getPackage())) {   // also check if it has current package?
			importRequests.add(importName);
		}
		//importRequest.add(importName);
		importRequest = importName;
		return imported;
	}
	
	public ArrayList<String> getImport() {
		return importRequests;
	}

}
