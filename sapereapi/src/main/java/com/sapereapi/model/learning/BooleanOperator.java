package com.sapereapi.model.learning;

import java.text.DecimalFormat;

public enum BooleanOperator {
	EQUALS("==", 0, new IBooleanOperator() {
		@Override
		public boolean apply(Double value1, Double value2) {
			return (value1 != null) && (value2 != null) && (value1.floatValue() == value2.floatValue());
		}
	}), GREATER_THAN(">", 1, new IBooleanOperator() {
		@Override
		public boolean apply(Double value1, Double value2) {
			return value1 > value2;
		}
	}), GREATER_THAN_OR_EQUALS(">=", 2, new IBooleanOperator() {
		@Override
		public boolean apply(Double value1, Double value2) {
			return value1 >= value2;
		}
	}), LESS_THAN("<", 3, new IBooleanOperator() {
		@Override
		public boolean apply(Double value1, Double value2) {
			return value1 < value2;
		}
	}), LESS_THAN_OR_EQUALS("<=", 4, new IBooleanOperator() {
		@Override
		public boolean apply(Double value1, Double value2) {
			return value1 <= value2;
		}
	}),;

	private String label;
	private Integer id;
	private IBooleanOperator operator;

	private BooleanOperator(String _label, Integer _id, IBooleanOperator _operator) {
		this.label = _label;
		this.id = _id;
		this.operator = _operator;
	}

	public boolean apply(Double value1, Double value2) {
		return operator.apply(value1, value2);
	}

	public String getLabel() {
		return label;
	}

	public Integer getId() {
		return id;
	}

	public IBooleanOperator getOperator() {
		return operator;
	}

	public String getIntervalMarker(Double value, DecimalFormat df) {
		String sValue = df.format(value);
		if (EQUALS.equals(this)) {
			return "{" + sValue + "}";
		} else if (GREATER_THAN.equals(this)) {
			return "]" + sValue;
		} else if (GREATER_THAN_OR_EQUALS.equals(this)) {
			return "[" + sValue;
		} else if (LESS_THAN.equals(this)) {
			return sValue + "[";
		} else if (LESS_THAN_OR_EQUALS.equals(this)) {
			return sValue + "]";
		} else {
			return "";
		}
	}
}
