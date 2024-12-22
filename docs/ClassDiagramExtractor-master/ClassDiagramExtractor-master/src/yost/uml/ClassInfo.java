package yost.uml;

import java.util.List;
import java.util.Map;

public class ClassInfo {
	private String simpleName;
	private String packageName;
	private String rootPath;
	private String packageColor;
	private String superClassName;
	private List<String> interfaces;
	private Map<String, List<String>> properties;
	private boolean isInterface;
	private boolean isAbstract;
	private boolean isDeprecated;
	private boolean isEnum;

	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String shortName) {
		this.simpleName = shortName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public String getPackageColor() {
		return packageColor;
	}

	public void setPackageColor(String packageColor) {
		this.packageColor = packageColor;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isDeprecated() {
		return isDeprecated;
	}

	public void setDeprecated(boolean isDeprecated) {
		this.isDeprecated = isDeprecated;
	}

	public boolean isEnum() {
		return isEnum;
	}

	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public String getCompleteName() {
		return packageName + "." + simpleName;
	}

	public String getSuperClassName() {
		return superClassName;
	}

	public void setSuperClassName(String superClassName) {
		this.superClassName = superClassName;
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(List<String> interfaces) {
		this.interfaces = interfaces;
	}


	public Map<String, List<String>> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, List<String>> properties) {
		this.properties = properties;
	}

	public boolean hasLink() {
		return superClassName != null || !interfaces.isEmpty() || !properties.isEmpty();
	}

	public boolean hasLinkTo(ClassInfo aClass) {
		String className = aClass.getCompleteName();
		if(this.getCompleteName().equals(className))  {
			// aClass is the current instance
			return false;
		}
		if(superClassName != null && superClassName.equals(className)) {
			return true;
		}
		return interfaces.contains(className) || properties.containsKey(className);
	}

	public boolean isLinkedBy(List<ClassInfo> listClasses) {
		if(this.simpleName.contains("Scope")) {
			System.out.println("isLinkedBy For debug ");
		}
		for(ClassInfo nextClass : listClasses) {
			if(nextClass.hasLinkTo(this)) {
				return true;
			}
		}
		return false;
	}

	public String generateGraphLabel() {
		String categoryCell = "";
		String classInfoCategory = "C"; // by default : class
		String bgColor = "#006400"; // dark green
		String complementaryLabel = " ";
		String openingQuotes = "&#60;&#60;";
		String closingQuotes = "&#62;&#62;";
		if(isInterface) {
			classInfoCategory = "I";
			bgColor = "purple";
			complementaryLabel = openingQuotes + "interface" + closingQuotes;
		} else if (isEnum) {
			classInfoCategory = "E";
			bgColor = "brown";
			complementaryLabel = openingQuotes + "enum" + closingQuotes;
		} else if (isAbstract) {
			classInfoCategory = "C*";
			bgColor = "#003100";
			complementaryLabel = openingQuotes + "abstract class" + closingQuotes;
		}
		categoryCell = "<TD BGCOLOR=" + ExtractorUtil.addQuotes(bgColor) + ">"
				+ "<FONT COLOR=\"white\" POINT-SIZE=\"10.0\">&nbsp;" + classInfoCategory + "</FONT></TD>";
		String classLabel = "<<TABLE  border='0' cellborder='0' cellspacing='0'>";
		if(complementaryLabel.trim().length() > 0) {
			classLabel+="<TR><TD colspan='2'>" + "<FONT POINT-SIZE='9'>" + complementaryLabel + "</FONT></TD></TR>";
		}
		classLabel+="<TR>" +categoryCell + "<TD> " + this.getSimpleName() + " </TD></TR>";
		classLabel+="</TABLE>> ";
		return classLabel;
	}

	public String generateGraphNode() {
		String classNode = this.getSimpleName();
		String classLabel = this.generateGraphLabel();
		classNode += "[shape=box,color=" + ExtractorUtil.addQuotes(packageColor) + ",label=" + classLabel + "]";
		return classNode;
	}
}
