package com.sapereapi.util.matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class IterationObsNb implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Map<Integer, Double> values;

	public IterationObsNb() {
		super();
		values = new HashMap<Integer, Double>();
	}

	public Map<Integer, Double> getValues() {
		return values;
	}

	public void setValues(Map<Integer, Double> values) {
		this.values = values;
	}

	public void setValue(int iterationNumber, double obsNb) {
		this.values.put(iterationNumber, obsNb);
	}

	public List<Integer> computeIterations() {
		List<Integer> result = new ArrayList<Integer>();
		for (Integer nextIt : values.keySet()) {
			result.add(nextIt);
		}
		Collections.sort(result);
		return result;
	}

	public Integer getMaxIterationNb() {
		Integer result = 0;
		for (Integer nextIteration : values.keySet()) {
			if (nextIteration > result) {
				result = nextIteration;
			}
		}
		return result;
	}

	public boolean hasValue(int iterationNumber) {
		return values.containsKey(iterationNumber);
	}

	public Double getValue(int iterationNumber) {
		if (values.containsKey(iterationNumber)) {
			return values.get(iterationNumber);
		}
		return null;
	}

	public double getTotalValue() {
		double total = 0.0;
		for (Integer nextIteration : values.keySet()) {
			total += values.get(nextIteration);
		}
		return total;
	}

	public Set<Integer> getIerations() {
		return values.keySet();
	}

	public int getSize() {
		return values.size();
	}

	public IterationObsNb clone() {
		IterationObsNb result = new IterationObsNb();
		for (Integer nextIteration : values.keySet()) {
			result.setValue(nextIteration, values.get(nextIteration));
		}
		return result;
	}

	public IterationObsNb plus(IterationObsNb b) {
		IterationObsNb result = this.clone();
		for (Integer nextIteration : b.getIerations()) {
			double toAdd = b.getValue(nextIteration);
			if (result.hasValue(nextIteration)) {
				result.setValue(nextIteration, result.getValue(nextIteration) + toAdd);
			} else {
				result.setValue(nextIteration, toAdd);
			}
		}
		return result;
	}

	public IterationObsNb minus(IterationObsNb b) {
		IterationObsNb result = this.clone();
		for (Integer nextIteration : b.getIerations()) {
			double toWithdraw = b.getValue(nextIteration);
			if (result.hasValue(nextIteration)) {
				result.setValue(nextIteration, result.getValue(nextIteration) - toWithdraw);
			} else {
				result.setValue(nextIteration, -1 * toWithdraw);
			}
		}
		return result;
	}

	public IterationObsNb uminus() {
		return (new IterationObsNb().minus(this));
	}

	public IterationObsNb times(double s) {
		IterationObsNb result = this.clone();
		for (Integer nextIteration : values.keySet()) {
			result.setValue(nextIteration, s * values.get(nextIteration));
		}
		return result;
	}

	public IterationObsNb auxApplyDivisor(double divisor) {
		IterationObsNb result = this.clone();
		for (Integer nextIteration : values.keySet()) {
			double value = SapereUtil.round(values.get(nextIteration) / divisor, 3);
			result.setValue(nextIteration, value);
		}
		return result;
	}

	public IterationObsNb auxApplyRound() {
		IterationObsNb result = this.clone();
		for (Integer nextIteration : values.keySet()) {
			result.setValue(nextIteration, Math.round(values.get(nextIteration)));
		}
		return result;
	}

	public String format3d(List<Integer> matrixIterations) {
		StringBuffer result = new StringBuffer();
		result.append("{");
		//if(false && values.size() > 0) {
		//	result.append(values.size()).append(":").append(matrixIterations.size()).append("#");
		//}
		String sep1 = "";
		List<Integer> iterations2 = matrixIterations;
		if (iterations2 == null) {
			iterations2 = computeIterations();
		}
		for (Integer nextIteration : iterations2) {
			result.append(sep1);
			if (values.containsKey(nextIteration)) {
				double value = values.get(nextIteration);
				result.append(UtilDates.df3.format(value));
				//result.append("$");
			} else {
				//result.append("£");
			}
			sep1 = ",";
		}
		result.append("}");
		return result.toString();
	}
}
