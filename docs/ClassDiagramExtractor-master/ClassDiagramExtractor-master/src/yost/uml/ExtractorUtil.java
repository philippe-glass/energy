package yost.uml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtractorUtil {

	public static boolean isNumeric(String type) {
		try {
			Integer.parseInt(type);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static String getClassCompleteName(String rootPath, String filepath) {
		String pack = filepath.replace(rootPath, "").replace("\\", ".").replace("$", ".");
		if (pack.startsWith(".")) {
			pack = pack.substring(1);
		}
		if (pack.toLowerCase().endsWith(".class")) {
			pack = pack.substring(0, pack.length() - 6);
		}
		return pack;
	}

	public static String getClassSimpleName(String className) {
		String[] parts = className.replace(";", "").split("\\.");
		return parts[parts.length - 1];
	}

	public static void writeFile(String content, String filename) {
		try {
			Files.writeString(Paths.get(filename), content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String addQuotes(String str) {
		StringBuffer quoted = new StringBuffer();
		quoted.append("\"").append(str).append("\"");
		return quoted.toString();
	}

	public static Map<String, List<String>> auxAddEntry(Map<String, List<String>> properties, String entryKey,
			String entryValue) {
		Map<String, List<String>> result = properties;
		if (!result.containsKey(entryKey)) {
			result.put(entryKey, new ArrayList<String>());
		}
		List<String> listProps = properties.get(entryKey);
		if (!listProps.contains(entryValue)) {
			listProps.add(entryValue);
		}
		return result;
	}

	public static Map<String, List<String>> auxAddProperties(Map<String, List<String>> properties,
			Map<String, List<String>> toAdd) {
		Map<String, List<String>> result = properties;
		for (String key : toAdd.keySet()) {
			List<String> values = toAdd.get(key);
			for (String value : values) {
				result = auxAddEntry(properties, key, value);
			}
		}
		return result;
	}
}
