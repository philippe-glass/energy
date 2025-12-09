package latex2doc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Latex2DocMain {
	private static boolean readThesisResport = false;
	private static boolean useChapters = readThesisResport ? true : false;// true;
	private static String sourceTexDir =readThesisResport ? "PhD-PhilippeGlass/chapters" : null;
	private static boolean activateDebug = false;// true;

	final static String[] tagsIgnoreLine = new String[] { "\\maketitle", "\\usepackage", "{document}", "{itemize}",
			"{enumerate}", "{description}", "{definition}", "\\addbibresource", "\\documentclass", "\\renewcommand",
			"\\printbibliography", "\\hline", "\\newcolumntype", "\\firstpage", "\\makeatletter", "\\setcounter",
			"\\makeatother", "\\pubvolume", "\\issuenum", "\\articlenumber", "\\pubyear", "\\copyrightyear",
			"\\datereceived", "\\daterevised", "\\dateaccepted", "\\titlespacing", "\\bigskip", "\\datepublished",
			"\\hreflink", "\\newcommand", "\\corres", "\\includegraphics", "\\TitleCitation", "\\Author",
			"\\AuthorNames", "\\AuthorCitation", "\\address", "\\authorcontributions", "\\vspace", "\\quad", "\\corres",
			"\\bibliography", "\\PublishersNote", "adjustwidth", "\\appendix", "\\appendixtitles", "\\reftitle",
			"\\FloatBarrier", "\\providecommand", "\\setalgorithmicfont", "\\pagestyle", "\\setlength", "\\pagebreak",
			"\\tableofcontents", "\\today", "\\geometry", "\\hspace", "a4paper", "\\titleformat", "\\titlespacing", "\\normalfont"
			, "\\rlap", "\\chaptertitlename", "\\MakeUppercase" , "\\addcontentsline", "\\horrule{2pt}", "0pt3.25ex"
			};
	final static String[] tagsIgnoreBloc = new String[] { "{algorithm}", "{figure}", "{table}", "{equation}", "{equation}", "{comment}"};
	final static String[] tagStartIgnoreBloc = addPrefix(tagsIgnoreBloc, "\\begin");
	final static String[] tagEndIgnoreBloc = addPrefix(tagsIgnoreBloc, "\\end");
	final static Map<String, String> tagsSimpleReplace = new HashMap<>();
	final static String lineSep = System.getProperty("line.separator");
	static {
		tagsSimpleReplace.put("\\item", " - ");
		tagsSimpleReplace.put("\\noindent", "");
		tagsSimpleReplace.put("~~\\\\", "");
		tagsSimpleReplace.put("\\textcolor{blue}", "");
		tagsSimpleReplace.put("\\textcolor{black}", "");
		tagsSimpleReplace.put("\\textcolor{red}", "");
		tagsSimpleReplace.put("\\textcolor{orange}", "");
		tagsSimpleReplace.put("\\textcolor{Green}", "");
		tagsSimpleReplace.put("\\textcolor{Black}", "");
		tagsSimpleReplace.put("\\textcolor{OliveGreen}", "");
		tagsSimpleReplace.put("\\textcolor{cyan}", "");
		tagsSimpleReplace.put("\\textcolor{gray}", "");
		tagsSimpleReplace.put("\\begin{scriptsize}", "");
		tagsSimpleReplace.put("\\begin{center}", "");
		tagsSimpleReplace.put("\\end{scriptsize}", "");
		tagsSimpleReplace.put("\\begin{abstract}", "");
		tagsSimpleReplace.put("\\end{abstract}", "");
		tagsSimpleReplace.put("\\small", "");
		tagsSimpleReplace.put("\\centering", "");
		tagsSimpleReplace.put("\\Large", "");
		tagsSimpleReplace.put("\\Huge", "");
		tagsSimpleReplace.put("\\huge", "");
		tagsSimpleReplace.put("\\normalsize", "");
		tagsSimpleReplace.put("\\sep", ",");
		//tagsSimpleReplace.put("\\caption{", "");
	}
	final static Map<String, Object> tagsReplaceBraceBlocs = new HashMap<>();
	static String varNumChampter = useChapters ? "#num_chapter#." : "";
	static {
		tagsReplaceBraceBlocs.put("\\label", "");
		tagsReplaceBraceBlocs.put("\\abstract", "Abstract:" + lineSep + "%:");
		if(useChapters) {
			tagsReplaceBraceBlocs.put("\\chapter"	, "Chapter #num_chapter++##num_section=0##num_subsection=0##num_subsubsection=0##num_ref=0#) %:");
		}
		tagsReplaceBraceBlocs.put("\\section"		, varNumChampter + "#num_section++##num_subsection=0##num_subsubsection=0#) %:");
		tagsReplaceBraceBlocs.put("\\subsection"	, varNumChampter + "#num_section#.#num_subsection++##num_subsubsection=0#) %:");
		tagsReplaceBraceBlocs.put("\\subsubsection"	, varNumChampter + "#num_section#.#num_subsection#.#num_subsubsection++##num_subsubsubsection=0#) %:");
		tagsReplaceBraceBlocs.put("\\subsubsubsection"
													, varNumChampter + "#num_section#.#num_subsection#.#num_subsubsection#.#num_subsubsubsection++#) %:");
		tagsReplaceBraceBlocs.put("\\title", "%:");
		tagsReplaceBraceBlocs.put("\\Title", "%:");
		tagsReplaceBraceBlocs.put("\\large", "%");
		tagsReplaceBraceBlocs.put("\\Large", "%");
		tagsReplaceBraceBlocs.put("\\textbf", "%");
		tagsReplaceBraceBlocs.put("\\huge", "%");
		tagsReplaceBraceBlocs.put("\\Huge", "%");
		tagsReplaceBraceBlocs.put("\\textit", "%");
		tagsReplaceBraceBlocs.put("\\textsc", "%");
		tagsReplaceBraceBlocs.put("\\caption", "%:");
		tagsReplaceBraceBlocs.put("\\math", "%:");
		tagsReplaceBraceBlocs.put("\\footnote", "(Footnote: %)");
		tagsReplaceBraceBlocs.put("\\color", "");
		tagsReplaceBraceBlocs.put("\\href", "");
		tagsReplaceBraceBlocs.put("\\hspace", "");
		if(useChapters) {
			tagsReplaceBraceBlocs.put("\\ref", "#num_chapter#.#num_ref++#");
		} else {
			tagsReplaceBraceBlocs.put("\\ref", "#num_ref++#");
		}
		tagsReplaceBraceBlocs.put("\\cite", "[#num_citation++#]");
	}

	static Map<String, String> mapEndReplacements = new HashMap<String, String>();
	static {
		mapEndReplacements.put("\\caption{", "");
		mapEndReplacements.put("\\\\", lineSep);
		mapEndReplacements.put("\\_", "_");
		mapEndReplacements.put("\\&", "&");
		mapEndReplacements.put("\\%", "%");
		mapEndReplacements.put("\\#", "%");
	}

	private static Pattern commentPattern1 = Pattern.compile("^(?<blank>[\s\t]*)%"); // notons le ?

	private static Map<String, Integer> mapCounters = new HashMap<>();
	private static BraceBlock retainedBraceBlock = null;
	private static List<String> retainedLines = new ArrayList<>();

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
		if(line.contains("%")) {
			Matcher matcher = commentPattern1.matcher(line);
			if(matcher.find()) {
				return true;
			}
		}
		return containsTag(line, tagsIgnoreLine);
	}

	private static boolean startsIgnoreBloc(String line) {
		return containsTag(line, tagStartIgnoreBloc);
	}

	private static boolean endsIgnoreBloc(String line) {
		return containsTag(line, tagEndIgnoreBloc);
	}

	private static void refreshRetainedBraceBlock(String inputLine) {
		List<BraceBlock> listBraceBlocks = auxFindBraceBlocks(inputLine);
		List<BraceBlock> listOpenBraceBlocks = getOpenBraceBlocks(listBraceBlocks);
		if (retainedBraceBlock == null) {
			// Check if there is not open brace block
			if (listOpenBraceBlocks.size() > 0) {
				retainedBraceBlock = listOpenBraceBlocks.get(0);
			}
		} else {
			auxTryCloseRetainedBraceBlock(inputLine);
		}
	}

	private static String replaceVariables(String input) {
		String newContent = input;
		List<LtxVariableInstruction> listFoundVariables = LtxVariableInstruction.findVariables(input);
		for (LtxVariableInstruction foundVariable : listFoundVariables) {
			if (activateDebug && input.contains("__cite") && activateDebug) {
				System.out.println("replaceVariables : for debug");
			}
			String varname = foundVariable.getVariable();
			Integer value = foundVariable.getIncrement();
			if (mapCounters.containsKey(varname)) {
				value = foundVariable.getIncrement() + mapCounters.get(varname);
				if (foundVariable.isReset()) {
					value = 0;
				}
			}
			String toReplace = foundVariable.getContent();
			String replacement = "" + (foundVariable.isReset() ? "" : value);
			if (newContent.contains("~#")) {
				toReplace = "~" + toReplace;
				replacement = " " + value;
			} else if (newContent.contains("~[#")) {
				toReplace = "~[" + toReplace + "]";
				replacement = " [" + value + "]";
			}
			mapCounters.put(varname, value);
			newContent = newContent.replace(toReplace, replacement);
		}
		return newContent;
	}

	private static List<BraceBlock> getClosedBraceBlocks(List<BraceBlock> listBrakeceBlocks) {
		List<BraceBlock> result = new ArrayList<>();
		for (BraceBlock nextBraceBlock : listBrakeceBlocks) {
			if (nextBraceBlock.isClosed()) {
				result.add(nextBraceBlock);
			}
		}
		return result;
	}

	private static List<BraceBlock> getOpenBraceBlocks(List<BraceBlock> listBrakeceBlocks) {
		List<BraceBlock> result = new ArrayList<>();
		for (BraceBlock nextBraceBlock : listBrakeceBlocks) {
			if (nextBraceBlock.isOpen()) {
				result.add(nextBraceBlock);
			}
		}
		return result;
	}

	private static BraceBlock getNextLeafBraceBlock(List<BraceBlock> listBrakeceBlocks, boolean onlyClosed) {
		if (listBrakeceBlocks.size() > 0) {
			// List<String> reversedList = Lists.reverse(listBrakeceBlocks);
			for (int idx = listBrakeceBlocks.size() - 1; idx >= 0; idx--) {
				BraceBlock nextBraceBlock = listBrakeceBlocks.get(idx);
				if (nextBraceBlock.isLeaf()) {
					if (!onlyClosed || !nextBraceBlock.isLeaf()) {
						return nextBraceBlock;
					}
				}
			}
		}
		return null;
	}

	private static BraceBlock getLastOpenBraceBlock(List<BraceBlock> listBrakeceBlocks) {
		if (listBrakeceBlocks.size() > 0) {
			for (int idx = listBrakeceBlocks.size() - 1; idx >= 0; idx--) {
				BraceBlock nextBraceBlock = listBrakeceBlocks.get(idx);
				if (nextBraceBlock.isOpen()) {
					return nextBraceBlock;
				}
			}
		}
		return null;
	}

	private static boolean auxTryCloseRetainedBraceBlock(String inputLine) {
		if (!inputLine.contains("}")) {
			return false;
		}
		boolean result = false;
		for (int charIdx = 0; charIdx < inputLine.length(); charIdx++) {
			char c = inputLine.charAt(charIdx);
			if (c == '}') {
				if (retainedBraceBlock != null && charIdx >= retainedBraceBlock.getIdxOpen()) {
					retainedBraceBlock.setIdxClose(charIdx);
					String textInBraceBlock;
					try {
						textInBraceBlock = inputLine.substring(1 + retainedBraceBlock.getIdxOpen(),
								retainedBraceBlock.getIdxClose());
						retainedBraceBlock.setInnerText(textInBraceBlock);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					result = true;
				}
			}
		}
		return result;
	}

	private static List<BraceBlock> auxFindBraceBlocks(String inputLine) {
		if (!inputLine.contains("{")) {
			return new ArrayList<>();
		}
		List<BraceBlock> result = new ArrayList<>();
		for (int charIdx = 0; charIdx < inputLine.length(); charIdx++) {
			char c = inputLine.charAt(charIdx);
			if (c == '{') {
				// Open new BraceBlock
				String beforeOpen = inputLine.substring(0, charIdx);
				int idxLastClose = beforeOpen.lastIndexOf("}");
				int idxBbackSlash = beforeOpen.lastIndexOf("\\");
				BraceBlock newBraceBlock = new BraceBlock();
				newBraceBlock.setIdxOpen(charIdx);
				if (idxBbackSlash >= 0 && idxBbackSlash > idxLastClose) {
					String tag = beforeOpen.substring(idxBbackSlash, charIdx - 0);
					newBraceBlock.setTagName(tag);
					String replacement = "%";
					if (tagsReplaceBraceBlocs.containsKey(tag)) {
						replacement = "" + tagsReplaceBraceBlocs.get(tag);
					}
					newBraceBlock.setReplacement(replacement);
				}
				result.add(newBraceBlock);

			} else if (c == '}') {
				BraceBlock lastOpenBraceBlock = getLastOpenBraceBlock(result);
				if (lastOpenBraceBlock != null) {
					lastOpenBraceBlock.setIdxClose(charIdx);
					String textInBraceBlock = inputLine.substring(1 + lastOpenBraceBlock.getIdxOpen(),
							lastOpenBraceBlock.getIdxClose());
					lastOpenBraceBlock.setInnerText(textInBraceBlock);
				}
			}
		}
		return result;
	}

	private static String generateOutputLine(String inputLine, boolean inIgnoredBloc) {
		if (activateDebug && inputLine.indexOf("_MakeUppercase") > 0) {
			System.out.println("generateOutputLine_NEW for debug");
		}
		String outputLine = inputLine;
		for (String tagReplace : tagsSimpleReplace.keySet()) {
			if (outputLine.contains(tagReplace)) {
				String replace = tagsSimpleReplace.get(tagReplace);
				outputLine = outputLine.replace(tagReplace, replace);
			}
		}
		List<BraceBlock> listBraceBlocks = auxFindBraceBlocks(outputLine);
		List<BraceBlock> listClosedBraceBlocks = getClosedBraceBlocks(listBraceBlocks);
		if (activateDebug && inputLine.contains("") && inputLine.contains("Review of coordination models:")) {
			System.out.println("generateOutputLine : for debug");
		}
		while (listClosedBraceBlocks.size() > 0) {
			BraceBlock leafBraceBlock = getNextLeafBraceBlock(listClosedBraceBlocks, true);
			if (leafBraceBlock == null) {
				leafBraceBlock = listClosedBraceBlocks.get(0);
			}
			outputLine = leafBraceBlock.transformLine(outputLine);
			outputLine = replaceVariables(outputLine);
			listClosedBraceBlocks = getClosedBraceBlocks(auxFindBraceBlocks(outputLine));
		}
		for(String toReplace: mapEndReplacements.keySet()) {
			String remplacement = mapEndReplacements.get(toReplace);
			boolean continueReplacement = outputLine.contains(toReplace);
			while (continueReplacement) {
				int sizeBefore = outputLine.length();
				outputLine = outputLine.replace(toReplace, remplacement);
				int sizeAfter = outputLine.length();
				continueReplacement = outputLine.contains(toReplace) && (sizeBefore != sizeAfter);
			}
		}
		/*
		if (outputLine.contains("caption{")) {
			outputLine = outputLine.replace("caption{", "");
		}
		while (outputLine.contains("\\_")) {
			outputLine = outputLine.replace("\\_", "_");
		}
		while (outputLine.contains("\\\\")) {
			outputLine = outputLine.replace("\\\\", lineSep);
		}*/
		// \&
		if (inIgnoredBloc && outputLine.trim().startsWith("\\State $")) {
			int commentIdx = 2 + outputLine.lastIndexOf('$');
			if (commentIdx > 20) {
				outputLine = "Comment of algorithm: " + outputLine.substring(commentIdx);
			}
			/*
			 * Pattern patternState = Pattern.
			 * compile("(?<blank>[\\s]*)\\State $(?<state>[0-9a-zA-Z_()\\-\s]+)$(?<comment>[0-9a-zA-Z_\s]+)"
			 * ); // notons le ? supplémentaire // patternState = Pattern.
			 * compile("    \\State \\$listProducers \\gets sortProducers(chosenPolicy) \\$ Designate producers to stop first"
			 * ); // notons le ? supplémentaire patternState =
			 * Pattern.compile("\\$ Designate producers to stop first"); // notons le ?
			 * supplémentaire
			 * 
			 * Matcher matcher = patternState.matcher(outputLine); boolean test =
			 * matcher.find(); if(matcher.find()) { String comment =
			 * matcher.group("comment"); outputLine = comment; }
			 */

		}
		outputLine = outputLine.trim();
		/*
		 * if (outputLine.endsWith("\\\\")) { int len = outputLine.length() - 2;
		 * outputLine = outputLine.substring(0, len); } if
		 * (outputLine.startsWith("\\\\")) { outputLine = outputLine.substring(1); }
		 */
		/*
		 * if(outputLine.startsWith("{")) { outputLine = outputLine.substring(1); }
		 * if(outputLine.endsWith("}")) { int len = outputLine.length() -1; outputLine =
		 * outputLine.substring(0, len); }
		 */
		return outputLine;
	}

	public static String cleanLine(String input) {
		String output = input;
		Map<String, String> replaceTable = new HashMap<String, String>();
		replaceTable.put("\u00e2" + "\u20ac" + "\u02dc", "'"); // opening quote
		replaceTable.put("\u00e2" + "\u20ac" + "\u2122", "'"); // closing quote
		replaceTable.put("\u00e2" + "\u20ac" + "\u0153", "\""); // opening double-quote
		replaceTable.put("\u00e2" + "\u20ac" + "\ufffd", "\""); // closing double-quote
		replaceTable.put("\u00c3" + "\u00a8", "ê");
		replaceTable.put("\u00c3" + "\u00aa", "è");
		replaceTable.put("\u00c3" + "\u00a9", "é");
		replaceTable.put("\u00c3" + "\u00a0", "à");
		replaceTable.put("\u00c3" + "\u00a2", "â");
		replaceTable.put("\u00c3" + "\u00b4", "ô");
		replaceTable.put("\u00c3" + "\u00a7", "ç");
		replaceTable.put("\u00c5" + "\u201c", "oe");
		for (String toReplace : replaceTable.keySet()) {
			if (output.contains(toReplace)) {
				String replacement = replaceTable.get(toReplace);
				output = output.replace(toReplace, replacement);
			}
		}

		String sequence1 = "\u00e2" + "\u20ac" + "\u02dc";
		String sequence2 = "\u00e2" + "\u20ac" + "\u2122";

		if (output.contains(sequence1)) {
			output = output.replace(sequence1, "'");
			if (activateDebug) {
				System.out.println("cleanLine: output = " + output);
			}
		}
		if (output.contains(sequence2)) {
			output = output.replace(sequence2, "'");
			if (activateDebug) {
				System.out.println("cleanLine: output = " + output);
			}
		}
		String toFind = "In the Gamma coordination model, ";
		if (activateDebug && output.indexOf(toFind) >= 0) {
			String strLocation = toFind;
			System.out.println("test for debug : output = " + output);
			int idx = output.indexOf(strLocation);
			if (idx >= 0) {
				String test = "" + output.substring(idx + strLocation.length());
				String seq = "\u00c3" + "\u00a8";
				System.out.println("find " + "sequence " + " = " + test.contains(seq));
				System.out.println("test = " + test);
				for (int charIdx = 0; charIdx < Math.min(25, test.length()); charIdx++) {
					char nextChar = test.charAt(charIdx);
					System.out.println("nextChar = " + nextChar + " asccii = " + (int) nextChar + " hex = "
							+ String.format("%04x", (int) nextChar));
				}
			}
		}
		return output;
	}

	private static void generateMainTex(String sourceTexDir)  {
		File sourceDirectory = new File(sourceTexDir);
		File[] files = sourceDirectory.listFiles();
		// Print name of the all files present in that path
		if (files != null) {
			Writer mainTexWriter = null;
			try {
				mainTexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("main.tex"), "UTF8"));
				for (File file : files) {
					String fileName = file.getName();
					if(fileName.endsWith(".tex")) {
						System.out.println(fileName);
						BufferedReader reader = new BufferedReader(new FileReader(sourceTexDir + "/" + fileName));
						String inputLine = null;
						while ((inputLine = reader.readLine()) != null) {
							if(!toIgnore(inputLine)) {
								inputLine = cleanLine(inputLine);
								mainTexWriter.append(lineSep).append(inputLine);
							}
						}
						reader.close();
					}
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					mainTexWriter.close();
				} catch (Exception ex) {
					/* ignore */}
			}
		}
	}

	public static void main(String[] args) {
		Writer writer = null;
		BufferedReader reader;
		int nbBlanks = 0;
		retainedBraceBlock = null;
		try {
			// String currentDir = System.getProperty("user.dir");
			// System.out.println("Current dir using System:" + currentDir);
			if(sourceTexDir != null) {
				generateMainTex(sourceTexDir);
			}
			reader = new BufferedReader(new FileReader("main.tex"));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("report.txt"), "UTF8"));
			String inputLine = null;
			boolean inIgnoredBloc = false;
			mapCounters = new HashMap<>();
			while ((inputLine = reader.readLine()) != null) {
				// read next line
				inputLine = cleanLine(inputLine);
				if (activateDebug && inputLine.contains("caption{")
				// || inputLine.contains("when applying this indexed pricing policy to the
				// previous example, the 7 unsatisfied agents then defer thei"))
				) {
					System.out.println("Latex2DocMain : for debug");
				}
				if (startsIgnoreBloc(inputLine)) {
					inIgnoredBloc = true;
				} else if (inIgnoredBloc) {
					if (endsIgnoreBloc(inputLine)) {
						inIgnoredBloc = false;
					} else if (!toIgnore(inputLine)) {
						boolean skipIgnore = inputLine.contains("\\caption") || inputLine.contains("\\Comment")
						// || (retainedBrceBlock != null)
						;
						if (skipIgnore) {
							if (activateDebug && retainedBraceBlock != null) {
								System.out.println("For debug : skipIgnore");
							}
							// Dynamic representation of a consumer.
							handleLine(inputLine, nbBlanks, writer, inIgnoredBloc);
						}
					}
				} else if (!inIgnoredBloc && !toIgnore(inputLine)) {
					handleLine(inputLine, nbBlanks, writer, inIgnoredBloc);
				}
			}
			reader.close();
		} catch (IOException e) {
			String userDirectory = System.getProperty("user.dir");
			System.out.println("userDirectory = " + userDirectory);
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}

	private static void handleLine(String inputLine, int nbBlanks, Writer writer, boolean inIgnoredBloc)
			throws IOException {
		String additionalBuffer = "";
		refreshRetainedBraceBlock(inputLine);
		if (retainedBraceBlock != null && retainedBraceBlock.isClosed()) {
			retainedBraceBlock = null;
			additionalBuffer = String.join(lineSep, retainedLines) + lineSep;
			retainedLines.clear();
		}
		boolean isRetained = (retainedBraceBlock != null);
		if (isRetained) {
			retainedLines.add(inputLine);
		} else {
			String outputLine = generateOutputLine(additionalBuffer + inputLine, inIgnoredBloc);
			boolean skeepLine = false;
			if ("".equals(outputLine.trim())) {
				nbBlanks++;
				if (nbBlanks > 2) {
					// writer.append(lineSep).append("nbBlanks = " + nbBlanks);
					// System.out.println("nbBlanks = " + nbBlanks);
					skeepLine = true;
				}
			} else {
				nbBlanks = 0;
			}
			if (!skeepLine) {
				writer.append(lineSep).append(outputLine);
				System.out.println(outputLine);
			}
		}
	}
}
