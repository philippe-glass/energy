package com.sapereapi.model;

import java.util.ArrayList;
import java.util.List;

public class AgentState {
	private List<String> inputs;
	private List<String> outputs;

	public AgentState() {
		super();
		inputs = new ArrayList<String>();
		outputs = new ArrayList<String>();
	}

	public AgentState(List<String> inputs, List<String> outputs) {
		super();
		this.inputs = inputs;
		this.outputs = outputs;
	}


	public void addInput(String input) {
		if (input != null && !"".equals(input) && !inputs.contains(input)) {
			inputs.add(input.trim());
		}
	}

	public void addOutput(String output) {
		if (output != null && !"".equals(output) && !outputs.contains(output)) {
			outputs.add(output.trim());
		}
	}

	public List<String> getInputs() {
		return inputs;
	}

	public void setInputs(List<String> inputs) {
		this.inputs = inputs;
	}

	public List<String> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<String> outputs) {
		this.outputs = outputs;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		String sep = "";
		for (String input : inputs) {
			result.append(sep);
			result.append(input);
			sep = ",";
		}
		result.append("|");
		sep = "";
		for (String output : outputs) {
			result.append(sep);
			result.append(output);
			sep = ",";
		}
		return result.toString();
		// return inputs.toString() + "|" + outputs.toString();
	}

}
