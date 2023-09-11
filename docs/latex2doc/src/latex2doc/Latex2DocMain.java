package latex2doc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Latex2DocMain {

	final static String[] tagsIgnoreLine = new String[] { "\\maketitle", "\\usepackage", "{document}", "{itemize}", "{enumerate}", "{description}", "{definition}"
			, "\\addbibresource", "\\documentclass", "\\renewcommand", "\\printbibliography"
			, "\\newcolumntype", "\\firstpage"	, "\\makeatletter", "\\setcounter"	, "\\makeatother"
			, "\\pubvolume"	, "\\issuenum", "\\articlenumber", "\\pubyear", "\\copyrightyear"
			, "\\datereceived", "\\daterevised", "\\dateaccepted", "\\titlespacing"
			, "\\datepublished"	, "\\hreflink", "\\newcommand", "\\corres"
			, "\\TitleCitation", "\\Author", "\\AuthorNames", "\\AuthorCitation", "\\address", "\\authorcontributions"
			,"\\vspace", "\\quad", "\\corres", "\\bibliography", "\\PublishersNote", "adjustwidth", "\\appendix", "\\appendixtitles", "\\reftitle"
			, "\\FloatBarrier", "\\providecommand", "\\setalgorithmicfont", "\\pagestyle", "\\setlength"
			};
	final static String[] tagsIgnoreBloc = new String[] { "{algorithm}", "{figure}", "{table}" , "{equation}"};
	final static String[] tagStartIgnoreBloc = addPrefix(tagsIgnoreBloc, "\\begin");
	final static String[] tagEndIgnoreBloc = addPrefix(tagsIgnoreBloc, "\\end");
	final static Map<String, String> tagsSimpleReplace = new HashMap<>();
	static {
		tagsSimpleReplace.put("\\item", " - ");
		tagsSimpleReplace.put("\\noindent", "");
		tagsSimpleReplace.put("~~\\\\", "");
		tagsSimpleReplace.put("\\textcolor{blue}", "");
		tagsSimpleReplace.put("\\textcolor{cyan}", "");
		tagsSimpleReplace.put("\\textcolor{gray}", "");
		tagsSimpleReplace.put("\\begin{scriptsize}", "");
		tagsSimpleReplace.put("\\end{scriptsize}", "");
		tagsSimpleReplace.put("\\begin{abstract}", "");
		tagsSimpleReplace.put("\\end{abstract}", "");
	}
	final static Map<String, Object> tagsReplaceBrakets = new HashMap<>();
	static {
		tagsReplaceBrakets.put("\\label", "");
		tagsReplaceBrakets.put("\\abstract", "%");
		tagsReplaceBrakets.put("\\keywords", "%");
		//tagsReplaceBrakets.put("definition", "%");
		tagsReplaceBrakets.put("\\keyword", "%");
		tagsReplaceBrakets.put("\\section", "%:");
		tagsReplaceBrakets.put("\\subsection", "%:");
		tagsReplaceBrakets.put("\\subsubsection", "%:");
		tagsReplaceBrakets.put("\\title", "%:");
		tagsReplaceBrakets.put("\\Title", "%:");
		tagsReplaceBrakets.put("\\caption", "%:");
		tagsReplaceBrakets.put("\\underline", "%");
		tagsReplaceBrakets.put("\\textbf", "%");
		tagsReplaceBrakets.put("\\textit", "%");
		tagsReplaceBrakets.put("\\author", "%");
		tagsReplaceBrakets.put("\\date", "%");
		tagsReplaceBrakets.put("\\corres", "%");
		tagsReplaceBrakets.put("\\firstnote", "%");
		tagsReplaceBrakets.put("\\institute", "%");
		tagsReplaceBrakets.put("\\footnote",  "[%]");
		tagsReplaceBrakets.put("\\secondnote", "%");
		tagsReplaceBrakets.put("\\dataavailability", "%");
		tagsReplaceBrakets.put("\\acknowledgments", "%");
		tagsReplaceBrakets.put("\\conflictsofinterest", "%");
		tagsReplaceBrakets.put("\\sampleavailability", "%");
		tagsReplaceBrakets.put("\\abbreviations", "%");
		tagsReplaceBrakets.put("\\institutionalreview", "%");
		tagsReplaceBrakets.put("\\funding", "%");
		tagsReplaceBrakets.put("\\institutionalreview", "%");
		tagsReplaceBrakets.put("\\informedconsent", "%");
		tagsReplaceBrakets.put("\\Comment", "%");
		tagsReplaceBrakets.put("\\funding", "%");
		tagsReplaceBrakets.put("\\scriptsize", "%");
		tagsReplaceBrakets.put("~\\ref", " #num_ref#");
		tagsReplaceBrakets.put("~\\cite", " [#num_citation#]");
	}

	private static Map<String, Integer> mapCounters = new HashMap<>();

	private static String[] addPrefix(String[] tagList, String prefix) {
		String[] result = new String[tagList.length];
		// System.arraycopy(tagList, 0, result, 0, tagList.length);
		for (int idx = 0; idx < tagList.length; idx++) {
			result[idx] = prefix + tagList[idx];
		}
		return result;
	}

	private static boolean containsTag(String line, String[] tagList) {
		for (String tag : tagList) {
			if (line.contains(tag)) {
				return true;
			}
		}
		return false;
	}

	private static boolean toIgnore(String line) {
		return line.startsWith("%") || containsTag(line, tagsIgnoreLine);
	}

	private static boolean startsIgnoreBloc(String line) {
		return containsTag(line, tagStartIgnoreBloc);
	}

	private static boolean endsIgnoreBloc(String line) {
		return containsTag(line, tagEndIgnoreBloc);
	}

	private static String replaceBraket(String outputLine, String tagReplaceBraket) {
		String tagBegin = tagReplaceBraket + "{";
		while ((outputLine.contains(tagBegin)) && (outputLine.contains("}"))) {
			int idx1 = outputLine.indexOf(tagBegin);
			int idx2 = outputLine.indexOf("}", idx1);
			if (idx1 >= 0 && idx2 > idx1) {
				String replace = "" + tagsReplaceBrakets.get(tagReplaceBraket);
				String betweenBrakets = outputLine.substring(idx1 + tagReplaceBraket.length() + 1, idx2 - 0);
				String newContent = replace;
				if ("%".equals(replace)) {
					newContent = betweenBrakets;
				} else if(replace.contains("%")) {
					try {
						if(betweenBrakets.contains("$")) {
							betweenBrakets = betweenBrakets.replaceAll("\\$", "");
							newContent = replace.replaceAll("%", betweenBrakets);
						}
						newContent = replace.replaceAll("%", betweenBrakets);
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (newContent.contains("#")) {
					Pattern pattern = Pattern.compile("[#](?<variable>[a-zA-Z_]+)[#]"); // notons le ? supplémentaire
					Matcher matcher = pattern.matcher(newContent);
					if (matcher.find()) {
						int groupCouht = matcher.groupCount();
						String varname = matcher.group("variable");
						String varname2 = "#" + varname + "#";
						for (int groupIdx = 0; groupIdx < groupCouht; groupIdx++) {
							// System.out.println("matcher[" + groupIdx + "] = " + matcher.group(groupIdx));
						}
						Integer value = 1;
						if (mapCounters.containsKey(varname)) {
							value = 1 + mapCounters.get(varname);
						}
						mapCounters.put(varname, value);
						newContent = newContent.replace(varname2, "" + value);
					}
				}
				outputLine = outputLine.substring(0, idx1) + newContent + outputLine.substring(idx2 + 1);
			}
		}
		return outputLine;
	}

	private static String generateOutputLine(String inputLine) {
		String outputLine = inputLine;
		for (String tagReplace : tagsSimpleReplace.keySet()) {
			if (outputLine.contains(tagReplace)) {
				String replace = tagsSimpleReplace.get(tagReplace);
				outputLine = outputLine.replace(tagReplace, replace);
			}
		}
		for (String tagReplaceBraket : tagsReplaceBrakets.keySet()) {
			outputLine = replaceBraket(outputLine, tagReplaceBraket);
		}
		if(outputLine.contains("\\\\")) {
			String lineSep = System.getProperty("line.separator");
			outputLine = outputLine.replaceAll("\\\\", lineSep);
		}
		outputLine = outputLine.trim();
		/*
		if (outputLine.endsWith("\\\\")) {
			int len = outputLine.length() - 2;
			outputLine = outputLine.substring(0, len);
		}
		if (outputLine.startsWith("\\\\")) {
			outputLine = outputLine.substring(1);
		}*/
		if(outputLine.startsWith("{")) {
			outputLine = outputLine.substring(1);
		}
		if(outputLine.endsWith("}")) {
			int len = outputLine.length() -1;
			outputLine = outputLine.substring(0, len);
		}
		return outputLine;
	}

	public static void main(String[] args) {
		Writer writer = null;
		BufferedReader reader;
		int nbBlanks = 0;
		try {
			// String currentDir = System.getProperty("user.dir");
			String lineSep = System.getProperty("line.separator");
			// System.out.println("Current dir using System:" + currentDir);
			reader = new BufferedReader(new FileReader("main.tex"));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("report.txt"), "utf-8"));
			String inputLine = null;
			boolean inIgnoredBloc = false;
			mapCounters = new HashMap<>();
			while ((inputLine = reader.readLine()) != null) {
				// read next line
				if (inputLine.contains("Smart grid system with an energy provider and $M$ ")
				// || inputLine.contains("when applying this indexed pricing policy to the
				// previous example, the 7 unsatisfied agents then defer thei"))
				) {
					System.out.println("Latex2DocMain : for debug");
				}
				if (startsIgnoreBloc(inputLine)) {
					inIgnoredBloc = true;
				} else if(inIgnoredBloc) {
					if (endsIgnoreBloc(inputLine)) {
						inIgnoredBloc = false;
					} else if(inputLine.contains("\\caption") && !toIgnore(inputLine)){
						// Dynamic representation of a consumer.
						String outputLine = "Element caption: " + generateOutputLine(inputLine);						
						writer.append(lineSep).append(outputLine);
						System.out.println(outputLine);
					/*
					} else if(inputLine.contains("\\begin{scriptsize}")
							&& inputLine.contains("\\textcolor{gray}") && !toIgnore(inputLine)){
						//int idx = inputLine.indexOf("\\textcolor{gray}") + "\\textcolor{gray}".length()+1;
						int idx = inputLine.indexOf("\\begin{scriptsize}");
						String outputLine = generateOutputLine(inputLine.substring(idx).trim());
						if(outputLine.startsWith("//")) {
							outputLine = outputLine.substring(2);
						}
						while(outputLine.startsWith(" ")) {
							outputLine = outputLine.substring(1);
						}
						outputLine = "Comment: " + outputLine;
						writer.append(lineSep).append(outputLine);
						System.out.println(outputLine);
					*/
					} else if(inputLine.contains("\\Comment") && !toIgnore(inputLine)){
						int idx = inputLine.indexOf("\\Comment");
						String outputLine = generateOutputLine(inputLine.substring(idx).trim());
						while(outputLine.startsWith(" ")) {
							outputLine = outputLine.substring(1);
						}
						outputLine = "Comment: " + outputLine;
						writer.append(lineSep).append(outputLine);						
						System.out.println(outputLine);
					}
				} else if (!inIgnoredBloc && !toIgnore(inputLine)) {
					String outputLine = generateOutputLine(inputLine);
					boolean skeepLine = false;
					if("".equals(outputLine.trim())) {
						nbBlanks++;
						if(nbBlanks > 2) {
							//writer.append(lineSep).append("nbBlanks = " + nbBlanks);
							//System.out.println("nbBlanks = " + nbBlanks);
							skeepLine = true;
						}
					} else {
						nbBlanks = 0;
					}
					if(!skeepLine) {
						writer.append(lineSep).append(outputLine);
						System.out.println(outputLine);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}
}
