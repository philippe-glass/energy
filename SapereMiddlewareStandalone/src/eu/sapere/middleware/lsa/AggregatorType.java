package eu.sapere.middleware.lsa;

public enum AggregatorType {
	STANDARD("standard", 0), CUSTOMIZED("custormized", 1);

	private AggregatorType(final String text, final int _index) {
		this.label = text;
		this.index = _index;
	}

	private final String label;
	private final int index;

	public String getLabel() {
		return label;
	}

	public int getIndex() {
		return index;
	}

}
