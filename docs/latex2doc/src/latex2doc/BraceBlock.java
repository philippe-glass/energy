package latex2doc;

public class BraceBlock {
	private String tagName;
	private int idxOpen;
	private int idxClose;
	private String innerText;
	private String replacement;

	public BraceBlock() {
		super();
		idxOpen = -1;
		idxClose = -1;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public int getIdxOpen() {
		return idxOpen;
	}

	public void setIdxOpen(int idxOpen) {
		this.idxOpen = idxOpen;
	}

	public int getIdxClose() {
		return idxClose;
	}

	public void setIdxClose(int idxClose) {
		this.idxClose = idxClose;
	}

	public String getInnerText() {
		return innerText;
	}

	public void setInnerText(String innerText) {
		this.innerText = innerText;
	}

	boolean isOpen() {
		return idxClose < 0;
	}

	boolean isClosed() {
		return !isOpen();
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public boolean containsSubBraket() {
		if (this.innerText != null) {
			return innerText.contains("{") && innerText.contains("}");
		}
		return false;
	}

	public boolean isLeaf() {
		return !this.containsSubBraket();
	}

	@Override
	public String toString() {
		String quote = "\"";
		StringBuffer result = new StringBuffer();
		result.append("[Braket tag:").append(quote).append(tagName).append(quote).append(" ").append(idxOpen)
				.append(" -> ");
		if (isClosed()) {
			result.append(idxClose);
			result.append(", innerText:").append(innerText);
		}
		result.append("]");
		return result.toString();
		// return "[Braket tag:" + + tagName + "\"" + " " + idxOpen + "->" + idxClose +
		// ", innerText:" + innerText + "]";
	}

	public String getInnerTextTransformed() {
		String newContent = this.innerText;
		if (this.replacement != null && !"%".equals(this.replacement)) {
			try {
				String innerText2 = innerText;
				if (innerText.contains("$")) {
					innerText2 = innerText.replaceAll("\\$", "");
				}
				if (innerText2.contains("~")) {
					//innerText2 = innerText2.replace("~", "\\~");
					StringBuffer result = new StringBuffer();
					for (int charIdx = 0; charIdx < replacement.length(); charIdx++)  {
						char nextChar = replacement.charAt(charIdx);
						if(nextChar == '%') {
							result.append(innerText2);
						} else {
							result.append(nextChar);
						}
					}
					//String test = replacement.replace("%", innerText2);
					newContent = result.toString();// replacement.replace("%", innerText2);
				} else {
					newContent = replacement.replaceAll("%", innerText2);
				}
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return newContent;
	}

	public String transformLine(String input) {
		String output = input;
		if (isOpen()) {
			System.out.print("transformLine : for debug : braket is open " + this.toString());
		} else if (input.contains(innerText) && input.length() >= 1 + this.idxClose) {
			String innerTextTransformed = getInnerTextTransformed();
			int idxBeginTag = idxOpen - (this.tagName == null ? 0 : tagName.length());
			output = input.substring(0, idxBeginTag) + innerTextTransformed + input.substring(1 + this.idxClose);
		} else {
			System.err.println("transformLine " + toString() + " tag not found in the input " + input);
		}
		return output;
	}
}
