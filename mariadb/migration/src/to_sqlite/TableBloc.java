package to_sqlite;

import java.util.ArrayList;
import java.util.List;

public class TableBloc {
	String tableName;
	List<String> preInstructions = new ArrayList<>();
	List<String> instructions = new ArrayList<>();
	List<String> postInstructions = new ArrayList<>();

	public TableBloc(String tableName) {
		super();
		this.tableName = tableName;
		preInstructions = new ArrayList<>();
		instructions = new ArrayList<>();
		postInstructions = new ArrayList<>();
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<String> getPreInstructions() {
		return preInstructions;
	}

	public void setPreInstructions(List<String> preInstructions) {
		this.preInstructions = preInstructions;
	}

	public List<String> getInstructions() {
		return instructions;
	}

	public void setInstructions(List<String> instructions) {
		this.instructions = instructions;
	}

	public List<String> getPostInstructions() {
		return postInstructions;
	}

	public void setPostInstructions(List<String> postInstructions) {
		this.postInstructions = postInstructions;
	}

	public void addInstruction(String instruction) {
		this.instructions.add(instruction);
	}

	public void addPreInstruction(String instruction) {
		this.preInstructions.add(instruction);
	}

	public void addPostInstruction(String instruction) {
		this.postInstructions.add(instruction);
	}

	public List<String> getAllInstructions() {
		List<String> result = new ArrayList<>();
		result.addAll(this.preInstructions);
		result.addAll(this.instructions);
		// TODO remove "," on the last instruction
		//int indexLastInstruction = result.size()-1;
		//result.get(indexLastInstruction).;
		result.addAll(this.postInstructions);
		return result;
	}
}
