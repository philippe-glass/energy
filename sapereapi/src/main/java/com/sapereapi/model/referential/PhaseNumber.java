package com.sapereapi.model.referential;

public enum PhaseNumber {l1(1)	, l2(2)	, l3(3);
	private Integer number;

	PhaseNumber(Integer _number) {
		this.number = _number;
	}

	public Integer getNumber() {
		return number;
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
