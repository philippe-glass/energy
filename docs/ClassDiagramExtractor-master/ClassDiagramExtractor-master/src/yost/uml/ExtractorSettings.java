package yost.uml;

import java.util.ArrayList;
import java.util.List;

public class ExtractorSettings {
	private List<String> classFolders = new ArrayList<String>();
	private List<String> filterPackage = new ArrayList<String>();
	private boolean isFilterPackageStrict = false;
	private List<String> packageColors = new ArrayList<String>();
	private String graphvizFilename = null;
	private boolean removeIsolatedClasses = false;
	private List<String> excludedClasses = new ArrayList<String>();

	public List<String> getClassFolders() {
		return classFolders;
	}

	public void setClassFolders(List<String> classFolders) {
		this.classFolders = classFolders;
	}

	public List<String> getFilterPackage() {
		return filterPackage;
	}

	public void setFilterPackage(List<String> filterPackage) {
		this.filterPackage = filterPackage;
	}

	public List<String> getPackageColors() {
		return packageColors;
	}

	public void setPackageColors(List<String> packageColors) {
		this.packageColors = packageColors;
	}

	public String getGraphvizFilename() {
		return graphvizFilename;
	}

	public void setGraphvizFilename(String graphvizFilename) {
		this.graphvizFilename = graphvizFilename;
	}

	public boolean isFilterPackageStrict() {
		return isFilterPackageStrict;
	}

	public void setFilterPackageStrict(boolean isFilterPackageStrict) {
		this.isFilterPackageStrict = isFilterPackageStrict;
	}

	public boolean isRemoveIsolatedClasses() {
		return removeIsolatedClasses;
	}

	public void setRemoveIsolatedClasses(boolean removeIsolatedClasses) {
		this.removeIsolatedClasses = removeIsolatedClasses;
	}

	public String getPackageColor(int colorIdx) {
		if (packageColors == null || packageColors.size() == 0) {
			return null;
		}
		int packageIndex = colorIdx % packageColors.size();
		String color = packageColors.get(packageIndex);
		return color;
	}

	public List<String> getExcludedClasses() {
		return excludedClasses;
	}

	public void setExcludedClasses(List<String> excludedClasses) {
		this.excludedClasses = excludedClasses;
	}

	public boolean matchesFilterPackage(String classCompleteName) {
		for (String filterExclude : excludedClasses) {
			if (classCompleteName.contains(filterExclude)) {
				return false;
			}
		}
		for (String nextPackageFilter : filterPackage) {
			if (isFilterPackageStrict) {
				int indexDot = classCompleteName.lastIndexOf('.');
				if (indexDot > 0) {
					String packageName = classCompleteName.substring(0, indexDot);
					if (packageName.equals(nextPackageFilter)) {
						return true;
					}
				}
			} else {
				if (classCompleteName.contains(nextPackageFilter)) {
					return true;
				}
			}
		}
		return false;
	}
/*
	public boolean isExcluded(String classCompleteName) {
		for (String filterExclude : excludedClasses) {
			if (classCompleteName.contains(filterExclude)) {
				return true;
			}
		}
		return false;
	}
*/
}
