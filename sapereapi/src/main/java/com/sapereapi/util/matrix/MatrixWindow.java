package com.sapereapi.util.matrix;

import java.io.Serializable;

public class MatrixWindow implements Serializable {
	private static final long serialVersionUID = 1L;
	private int rowIndexBegin;
	private int rowIndexEnd;
	private int columnIndexBegin;
	private int columnIndexEnd;

	public int getRowIndexBegin() {
		return rowIndexBegin;
	}

	public void setRowIndexBegin(int rowIndexBegin) {
		this.rowIndexBegin = rowIndexBegin;
	}

	public int getRowIndexEnd() {
		return rowIndexEnd;
	}

	public void setRowIndexEnd(int rowIndexEnd) {
		this.rowIndexEnd = rowIndexEnd;
	}

	public int getColumnIndexBegin() {
		return columnIndexBegin;
	}

	public void setColumnIndexBegin(int columnIndexBegin) {
		this.columnIndexBegin = columnIndexBegin;
	}

	public int getColumnIndexEnd() {
		return columnIndexEnd;
	}

	public void setColumnIndexEnd(int columnIndexEnd) {
		this.columnIndexEnd = columnIndexEnd;
	}

	public MatrixWindow(int rowIndexBegin, int rowIndexEnd, int columnIndexBegin, int columnIndexEnd) {
		super();
		this.rowIndexBegin = rowIndexBegin;
		this.rowIndexEnd = rowIndexEnd;
		this.columnIndexBegin = columnIndexBegin;
		this.columnIndexEnd = columnIndexEnd;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[").append(rowIndexBegin).append("-").append(rowIndexEnd).append(" X ").append(columnIndexBegin)
				.append("-").append(columnIndexEnd).append("]");
		return result.toString();
	}

}
