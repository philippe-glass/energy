package com.sapereapi.model.referential;

public enum PhaseNumber {
	  L1(1, "l1")
	, L2(2, "l2")
	, L3(3, "l3")
	;

	private String label;
	private Integer number;

	PhaseNumber(Integer _number, String _label) {
		this.number = _number;
		this.label = _label;
	}

	public Integer getNumber() {
		return number;
	}

	public String getLabel() {
		return label;
	}

	public static PhaseNumber getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (PhaseNumber warningType : PhaseNumber.values()) {
			if (warningType.getLabel().equals(label2)) {
				return warningType;
			}
		}
		return null;
	}

	public static PhaseNumber getByNumber(Integer number) {
		Integer number2 = (number == null) ? -1 : number;
		for (PhaseNumber warningType : PhaseNumber.values()) {
			if (warningType.getNumber().equals(number2)) {
				return warningType;
			}
		}
		return null;
	}
}
