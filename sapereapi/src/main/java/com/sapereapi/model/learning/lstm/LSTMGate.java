package com.sapereapi.model.learning.lstm;

public enum LSTMGate {
	INPUT(0, "i"), FORGATE(1, "f"), CELL(2, "c"), OUTPUT(3, "o");

	private String code;
	private Integer index;

	LSTMGate(Integer _number, String _code) {
		this.index = _number;
		this.code = _code;
	}

	public Integer getIndex() {
		return index;
	}

	public String getCode() {
		return code;
	}

	public static LSTMGate getByCode(String label) {
		String label2 = (label == null) ? "" : label;
		for (LSTMGate warningType : LSTMGate.values()) {
			if (warningType.getCode().equals(label2)) {
				return warningType;
			}
		}
		return null;
	}

	public static LSTMGate getByIndex(Integer index) {
		Integer number2 = (index == null) ? -1 : index;
		for (LSTMGate warningType : LSTMGate.values()) {
			if (warningType.getIndex().equals(number2)) {
				return warningType;
			}
		}
		return null;
	}
}
