package to_sqlite;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 
 * 
 * MIgration to SQLITE :
-- ID -> 	ID INTEGER PRIMARY KEY AUTOINCREMENT  + remove the line PRIMARY KEY
--  UNSIGNED does not exists
-- remove all constrain names
-- UNIQUE KEY -> UNIQUE
-- current_timestamp()  -> current_timestamp 
-- FOREIGN KEY ??? ->   FOREIGN KEY(trackartist) REFERENCES XXXX(id)
-- ENGINE=InnoDB : to remove
-- CHARSET=utf8  to remove
-- COMMENT ON column/table : to remove (or put after --)
-- KEY -> CREATE INDEX <name_index> ON <table>(field1, file2, ...., fieldN)

-- can keep "`" characters : still OK

 * 
 * */

public class createTable2sqlite {

	final static String[] tagsIgnoreLine = new String[] { "DELIMITER §" };
	private static String currentTableName = "XXXXX";
	static List<String> listTables = new ArrayList<String>();
	static Map<String, TableBloc> mapTableBlocs = new HashMap<String, TableBloc>();

	public static TableBloc getCurrentTableBloc() {
		if (mapTableBlocs.containsKey(currentTableName)) {
			return mapTableBlocs.get(currentTableName);
		}
		return null;
	}

	private static void addInstruction(String instruction) {
		TableBloc tableBloc = getCurrentTableBloc();
		if (tableBloc != null) {
			tableBloc.addInstruction(instruction);
		}
	}

	private static void addPreInstruction(String instruction) {
		TableBloc tableBloc = getCurrentTableBloc();
		if (tableBloc != null) {
			tableBloc.addPreInstruction(instruction);
		}
	}

	private static void addPostInstruction(String instruction) {
		TableBloc tableBloc = getCurrentTableBloc();
		if (tableBloc != null) {
			tableBloc.addPostInstruction(instruction);
		}
	}

	public static String transInstruction(String inputLine) {
		if (inputLine.toUpperCase().contains("CREATE")) {
			Pattern patternCreateTable = Pattern.compile("(\s)*CREATE(\s)+TABLE(\s)+(?<tablename>[a-zA-Z0-9_`]+)(\s)*");
			Matcher matcher = patternCreateTable.matcher(inputLine);
			if (matcher.find()) {
				// New table
				currentTableName = matcher.group("tablename");
				listTables.add(currentTableName);
				TableBloc tableBloc = new TableBloc(currentTableName);
				tableBloc.addPreInstruction("DROP TABLE IF EXISTS " + currentTableName);
				tableBloc.addPreInstruction(";");
				mapTableBlocs.put(currentTableName, tableBloc);
			}
		}

		String outputLine = inputLine;
		outputLine = outputLine.replaceAll("§", ";");
		outputLine = outputLine.replaceAll("UNSIGNED", "");
		outputLine = outputLine.replaceAll("unsigned", "");
		outputLine = outputLine.replaceAll("SIGNED", "");
		outputLine = outputLine.replaceAll("signed", "");

		outputLine = outputLine.replace("ENGINE=InnoDB", "");
		outputLine = outputLine.replace("DEFAULT CHARSET=utf8", "");
		outputLine = outputLine.replace("AUTO_INCREMENT", "PRIMARY KEY AUTOINCREMENT");
		//outputLine = outputLine.replace("current_timestamp()", "current_timestamp");
		outputLine = outputLine.replace("current_timestamp()", "(datetime(CURRENT_TIMESTAMP, 'localtime'))");
		outputLine = outputLine.replace("current_date()", "(date(CURRENT_TIMESTAMP, 'localtime'))");
		outputLine = outputLine.replace("INT(11)", "INTEGER");
		outputLine = outputLine.replace("INT(10)", "INTEGER");
		outputLine = outputLine.replace("UNIQUE KEY", "UNIQUE");
		outputLine = outputLine.replace("DEFAULT b'0'", "DEFAULT 0");
		if (outputLine.contains("_location")) {
			// System.out.println("for debbug");
		}
		if (outputLine.toUpperCase().contains("COMMENT")) {
			Pattern patternComment = Pattern
					.compile("(?<instruction>[a-zA-Z0-9_,.\t\s]+)COMMENT(\s)+\'(?<comment>[a-zA-Z0-9_\s]+)\'(\s),");
			patternComment = Pattern.compile(
					"(?<instruction>[a-zA-Z0-9_,.\s]+)`id_node_config`		INTEGER  (\s)*COMMENT \'(?<comment>[a-zA-Z0-9_\s]+)\',");
			patternComment = Pattern
					.compile("`(?<instruction>[a-zA-Z0-9_,.\t\s]+)(\s)*COMMENT(\s)*\'(?<comment>[a-zA-Z0-9_\s]+)\',");
			patternComment = Pattern.compile(
					"^(?<instruction>[a-zA-Z0-9_,.\\-)\\(\t\s`/\']+)(\s)*COMMENT(\s)*\'(?<comment>[a-zA-Z0-9_\s\\()\\-/]+)\'(\s)*,");
			// patternComment = Pattern.compile("`total_produced` DECIMAL(15,3) NOT NULL
			// DEFAULT 0.00 COMMENT 'total produced (KWH)',
			// outputLine = outputLine.replace("(", "");
			// outputLine = outputLine.replace(")", "");
			Matcher matcher = patternComment.matcher(outputLine);
			if (matcher.find()) {
				String instruction = matcher.group("instruction");
				String comment = matcher.group("comment");
				outputLine = instruction + ", -- " + comment + "";
			} else {
				// System.out.println("comment not matching:"+outputLine);
			}
		}
		String outputLine2 = outputLine.replace("`", "");
		if (outputLine2.toUpperCase().contains("ENUM")) {
			Pattern patternEnum = Pattern.compile(
					"(\s)*(?<field>[a-zA-Z0-9_`]+)(\s)*ENUM(\s)*\\((?<values>[a-zA-Z_,\s\']+)\\)(\s)+DEFAULT(\s)+(?<defaultvalue>[a-zA-Z0-9_\\']+)?");
			patternEnum = Pattern.compile(
					"(?<field>[a-zA-Z0-9_`]+)(\s)*ENUM \\(\'START\',\'STOP\',\'EXPIRY\',\'UPDATE\'\\) DEFAULT NULL,");
			patternEnum = Pattern
					.compile("(?<field>[a-zA-Z0-9_`]+)(\s)*ENUM \\((?<values>[a-zA-Z_,\s\']+)\\) DEFAULT NULL,");
			patternEnum = Pattern.compile(
					"(\\s)*(?<field>[a-zA-Z0-9_`]+)(\s)*ENUM(\\s)*\\((?<values>[a-zA-Z_,\s\']+)\\)(\\s)+DEFAULT(\\s)+NULL,");
			patternEnum = Pattern.compile(
					"(\\s)*(?<field>[a-zA-Z0-9_`]+)([\s\t])*ENUM(\\s)*\\((?<values>[a-zA-Z_,\s\']+)\\)(\\s)+DEFAULT(\\s)+(?<defaultvalue>[a-zA-Z0-9_\\']+),");
			outputLine2 = "`main_category` ENUM ('START','STOP','EXPIRY','UPDATE') DEFAULT NULL,";
			outputLine2 = "  `main_category`		ENUM('START','STOP','EXPIRY','UPDATE') DEFAULT NULL,";
			// outputLine2 = outputLine2.replace("'", "");
			Matcher matcher = patternEnum.matcher(outputLine);
			if (matcher.find()) {
				String field = matcher.group("field");
				String values = matcher.group("values");
				outputLine = "  " + field + " VARCHAR(32) " + " CHECK(" + field + " IN (" + values + ")) DEFAULT "
						+ matcher.group("defaultvalue") + ",";
			}
		} else if (outputLine2.toUpperCase().contains("CONSTRAINT")) {
			Pattern patternForeignKey = Pattern.compile(
					"CONSTRAINT(\s)*(?<field>[a-zA-Z0-9_]+)(\s)*FOREIGN KEY(\s)*\\((?<fields>[a-zA-Z_\s\\,]+)\\) REFERENCES (?<tablename>[a-zA-Z_]+)(\s)+\\((?<targetfield>[a-zA-Z_]+)\\)");
			// Pattern patternForeignKey = Pattern.compile("CONSTRAINT
			// fk1_id_transition_matrix FOREIGN KEY");
			Matcher matcher = patternForeignKey.matcher(outputLine2);
			if (matcher.find()) {
				String tableName = matcher.group("tablename");
				String fields = matcher.group("fields");
				String targetField = matcher.group("targetfield");
				// System.out.println("-- ### group count = " + groupCouht + ", tableName = "+
				// tableName);
				String instruction = "	FOREIGN KEY(" + fields + ") REFERENCES " + tableName + "(" + targetField + "),";
				addInstruction(instruction);
				return instruction;
			}
		} else if (outputLine2.trim().toUpperCase().startsWith("PRIMARY KEY")) {
			Pattern patternPrimaryKey = Pattern.compile("PRIMARY KEY(\s)+\\((?<fields>[a-zA-Z_\s\\,]+)\\)");
			// patternPrimaryKey = Pattern.compile(" PRIMARY KEY (id),");

			Matcher matcher = patternPrimaryKey.matcher(outputLine2);
			if (matcher.find()) {
				String instruction = "-- " + outputLine;
				addInstruction(instruction);
				return instruction;
			}
		} else if (outputLine2.toUpperCase().contains("UNIQUE")) {
			Pattern patternUnicity = Pattern
					.compile("UNIQUE(\s)+(?<constraintname>[a-zA-Z0-9_]+)(\s)*\\((?<fields>[a-zA-Z_\s\\,]+)\\)");
			Matcher matcher = patternUnicity.matcher(outputLine2);
			if (matcher.find()) {
				String instruction = "	UNIQUE (" + matcher.group("fields") + "),";
				addInstruction(instruction);
				return instruction;
			}
		}
		if (outputLine2.toUpperCase().trim().startsWith("KEY ")) {
			Pattern patternForeignKey = Pattern
					.compile("KEY(\s)*(?<keyname>[a-zA-Z0-9_]+)(\s)*\\((?<fields>[a-zA-Z_\s\\,]+)\\)");
			// Pattern patternForeignKey = Pattern.compile("CONSTRAINT
			// fk1_id_transition_matrix FOREIGN KEY");
			Matcher matcher = patternForeignKey.matcher(outputLine2);
			if (matcher.find()) {
				String fields = matcher.group("fields");
				// System.out.println("-- ### group count = " + groupCouht + ", tableName = "+
				// tableName);
				String instruction = "CREATE INDEX " + matcher.group("keyname") + " ON " + currentTableName + " ("
						+ fields + ")" + ";";
				addPostInstruction(instruction);
				return "[post]" + instruction;
			}
		}
		addInstruction(outputLine);
		return outputLine;
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
		return line.startsWith("######%") || containsTag(line, tagsIgnoreLine);
	}

	public static void main(String[] args) {
		// System.out.println("main " + args);
		//Writer writer = null;
		BufferedReader reader;
		//int nbBlanks = 0;
		try {
			// System.out.println("Current dir using System:" + currentDir);
			String mariaDbScriptPath = "../../energy/01_create_table.sql";
			mariaDbScriptPath = "../energy/01_create_table.sql";
			reader = new BufferedReader(new FileReader(mariaDbScriptPath));
			//writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("report.txt"), "utf-8"));
			String inputLine = null;
			//boolean inIgnoredBloc = false;
			while ((inputLine = reader.readLine()) != null) {
				if (toIgnore(inputLine)) {
					// do nothing
				} else {
					//String outputLine = transInstruction(inputLine);
					// System.out.println(outputLine);

				}
			}

			for (String nextTable : listTables) {
				TableBloc tableBoc = mapTableBlocs.get(nextTable);
				for (String inscruction : tableBoc.getAllInstructions()) {
					System.out.println(inscruction);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
