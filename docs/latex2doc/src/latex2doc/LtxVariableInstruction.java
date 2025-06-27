package latex2doc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LtxVariableInstruction {
	private String content;
	private String variable;
	private int increment = 0;
	private boolean isReset = false;
	private static Pattern variablePattern = Pattern.compile("#(?<variable>[a-zA-Z_]+)(?<incr>\\+\\+)?(?<reset>=0)?#"); //


	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public int getIncrement() {
		return increment;
	}

	public void setIncrement(int increment) {
		this.increment = increment;
	}

	public boolean isReset() {
		return isReset;
	}

	public void setReset(boolean isReset) {
		this.isReset = isReset;
	}

	public LtxVariableInstruction(String content, String name, String sIncrement, String sReset) {
		super();
		this.content = content;
		this.variable = name;
		this.increment = "++".equals(sIncrement) ? 1 : 0;
		this.isReset = "=0".equals(sReset);

	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(variable);
		if (increment > 0) {
			result.append(":").append(increment);
		}
		return result.toString();
	}

	public static List<LtxVariableInstruction> findVariables(String input) {
		List<LtxVariableInstruction> result = new ArrayList<LtxVariableInstruction>();
		String newContent = input;
		while (newContent.contains("#")) {
			// newContent = "[#num_chapter++#] Introduction:";
			// Pattern pattern =
			// Pattern.compile("[#](?<variable>[a-zA-Z_]+)(?<incr>[123]?)[#]"); //
			// Pattern pattern =
			// Pattern.compile("[#](?<variable>[a-zA-Z_]+)(?<incr>\\+\\+)?[#]"); //
			// Matcher matcher = pattern.matcher(newContent);
			// Pattern pattern =
			// Pattern.compile("[#](?<variable>[a-zA-Z_]+)(?<incr>\\+\\+)?[#]"); //
			String nextVarInstruction = "";
			int idx1 = newContent.indexOf("#");
			int idx2 = newContent.indexOf("#", 1 + idx1);
			if (idx1 >= 0 && idx2 > idx1) {
				try {
					nextVarInstruction = newContent.substring(idx1, idx2 + 1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Matcher matcher = variablePattern.matcher(nextVarInstruction);
			if (matcher.find()) {
				// int groupCouht = matcher.groupCount();
				String varname = matcher.group("variable");
				String sIncrement = matcher.group("incr");
				String sReset = matcher.group("reset");
				if (sIncrement == null) {
					sIncrement = "";
				}
				if (sReset == null) {
					sReset = "";
				}
				String content = nextVarInstruction; // varname + sIncrement + sReset;
				LtxVariableInstruction foundVariable = new LtxVariableInstruction(content, varname, sIncrement, sReset);
				result.add(foundVariable);
				String varname2 = foundVariable.getContent();// "#" + varname + sIncrement + "#";
				//boolean aTest = newContent.contains(varname2);
				newContent = newContent.replace(varname2, ("" + foundVariable.getIncrement()));
			} else {
				newContent = ""; // to avoid infinite loop
			}
		}
		return result;
	}
}
