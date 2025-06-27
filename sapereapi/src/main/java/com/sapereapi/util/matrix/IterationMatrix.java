package com.sapereapi.util.matrix;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class IterationMatrix implements Cloneable, java.io.Serializable {

	private static final long serialVersionUID = 1;

	/*
	 * ------------------------ Class variables ------------------------
	 */

	/**
	 * Array for internal storage of elements.
	 * 
	 * @serial internal array storage.
	 */
	protected IterationObsNb[][] content;

	/**
	 * Row and column dimensions.
	 * 
	 * @serial row dimension.
	 * @serial column dimension.
	 */
	protected int rowNb, colNb;


	/*
	 * ------------------------ Constructors ------------------------
	 */

	/**
	 * Construct an m-by-n matrix of zeros.
	 * 
	 * @param _aRowNb Number of rows.
	 * @param _aColNb Number of colums.
	 */

	public IterationMatrix(int _aRowNb, int _aColNb) {
		this.rowNb = _aRowNb;
		this.colNb = _aColNb;
		content = new IterationObsNb[_aRowNb][_aColNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = new IterationObsNb();
			}
		}
	}

	/**
	 * Construct an m-by-n constant matrix.
	 * 
	 * @param m Number of rows.
	 * @param n Number of colums.
	 * @param s Fill the matrix with this scalar value.
	 */

	public IterationMatrix(int m, int n, IterationObsNb s) {
		this.rowNb = m;
		this.colNb = n;
		content = new IterationObsNb[m][n];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				content[i][j] = s;
			}
		}
	}

	/**
	 * Construct a matrix from a 2-D array.
	 * 
	 * @param _content Two-dimensional array of doubles.
	 * @exception IllegalArgumentException All rows must have the same length
	 * @see #constructWithCopy
	 */

	public IterationMatrix(IterationObsNb[][] _content) {
		rowNb = _content.length;
		colNb = _content[0].length;
		for (int i = 0; i < rowNb; i++) {
			if (_content[i].length != colNb) {
				throw new IllegalArgumentException("All rows must have the same length.");
			}
		}
		this.content = _content;
	}

	/**
	 * Construct a matrix quickly without checking arguments.
	 * 
	 * @param _content Two-dimensional array of doubles.
	 * @param m        Number of rows.
	 * @param n        Number of colums.
	 */

	public IterationMatrix(IterationObsNb[][] _content, int m, int n) {
		this.content = _content;
		this.rowNb = m;
		this.colNb = n;
	}

	/**
	 * Construct a matrix from a one-dimensional packed array
	 * 
	 * @param vals One-dimensional array of doubles, packed by columns (ala
	 *             Fortran).
	 * @param m    Number of rows.
	 * @exception IllegalArgumentException Array length must be a multiple of m.
	 */

	public IterationMatrix(IterationObsNb vals[], int m) {
		this.rowNb = m;
		colNb = (m != 0 ? vals.length / m : 0);
		if (m * colNb != vals.length) {
			throw new IllegalArgumentException("Array length must be a multiple of m.");
		}
		content = new IterationObsNb[m][colNb];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = vals[i + j * m];
			}
		}
	}

	/*
	 * ------------------------ Public Methods ------------------------
	 */

	/**
	 * Construct a matrix from a copy of a 2-D array.
	 * 
	 * @param A Two-dimensional array of doubles.
	 * @exception IllegalArgumentException All rows must have the same length
	 */

	public static IterationMatrix constructWithCopy(IterationObsNb[][] A) {
		int m = A.length;
		int n = A[0].length;
		IterationMatrix X = new IterationMatrix(m, n);
		IterationObsNb[][] C = X.getArray();
		for (int i = 0; i < m; i++) {
			if (A[i].length != n) {
				throw new IllegalArgumentException("All rows must have the same length.");
			}
			for (int j = 0; j < n; j++) {
				C[i][j] = A[i][j];
			}
		}
		return X;
	}

	/**
	 * Make a deep copy of a matrix
	 */

	public IterationMatrix copy() {
		IterationMatrix result = new IterationMatrix(rowNb, colNb);
		IterationObsNb[][] resultArray = result.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				resultArray[i][j] = item.clone();
			}
		}
		return result;
	}

	/**
	 * Clone the Matrix object.
	 */

	public IterationMatrix clone() {
		return this.copy();
	}

	/**
	 * Access the internal two-dimensional array.
	 * 
	 * @return Pointer to the two-dimensional array of matrix elements.
	 */

	public IterationObsNb[][] getArray() {
		return content;
	}

	/**
	 * Copy the internal two-dimensional array.
	 * 
	 * @return Two-dimensional array copy of matrix elements.
	 */

	public IterationObsNb[][] getArrayCopy() {
		IterationObsNb[][] C = new IterationObsNb[rowNb][colNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = content[i][j];
			}
		}
		return C;
	}

	/**
	 * Make a one-dimensional column packed copy of the internal array.
	 * 
	 * @return Matrix elements packed in a one-dimensional array by columns.
	 */

	public IterationObsNb[] getColumnPackedCopy() {
		IterationObsNb[] vals = new IterationObsNb[rowNb * colNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				vals[i + j * rowNb] = content[i][j];
			}
		}
		return vals;
	}

	/**
	 * Make a one-dimensional row packed copy of the internal array.
	 * 
	 * @return Matrix elements packed in a one-dimensional array by rows.
	 */

	public IterationObsNb[] getRowPackedCopy() {
		IterationObsNb[] vals = new IterationObsNb[rowNb * colNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				vals[i * colNb + j] = content[i][j];
			}
		}
		return vals;
	}

	/**
	 * Get row dimension.
	 * 
	 * @return m, the number of rows.
	 */

	public int getRowDimension() {
		return rowNb;
	}

	/**
	 * Get column dimension.
	 * 
	 * @return n, the number of columns.
	 */

	public int getColumnDimension() {
		return colNb;
	}

	/**
	 * Get a single element.
	 * 
	 * @param i Row index.
	 * @param j Column index.
	 * @return A(i,j)
	 * @exception ArrayIndexOutOfBoundsException
	 */

	public IterationObsNb get(int i, int j) {
		return content[i][j];
	}

	/**
	 * Get a submatrix.
	 * 
	 * @param i0 Initial row index
	 * @param i1 Final row index
	 * @param j0 Initial column index
	 * @param j1 Final column index
	 * @return A(i0:i1,j0:j1)
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public IterationMatrix getMatrix(int i0, int i1, int j0, int j1) {
		IterationMatrix X = new IterationMatrix(i1 - i0 + 1, j1 - j0 + 1);
		IterationObsNb[][] B = X.getArray();
		try {
			for (int i = i0; i <= i1; i++) {
				for (int j = j0; j <= j1; j++) {
					B[i - i0][j - j0] = content[i][j];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}

	/**
	 * Get a submatrix.
	 * 
	 * @param r Array of row indices.
	 * @param c Array of column indices.
	 * @return A(r(:),c(:))
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public IterationMatrix getMatrix(int[] r, int[] c) {
		IterationMatrix X = new IterationMatrix(r.length, c.length);
		IterationObsNb[][] B = X.getArray();
		try {
			for (int i = 0; i < r.length; i++) {
				for (int j = 0; j < c.length; j++) {
					B[i][j] = content[r[i]][c[j]];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}

	/**
	 * Get a submatrix.
	 * 
	 * @param i0 Initial row index
	 * @param i1 Final row index
	 * @param c  Array of column indices.
	 * @return A(i0:i1,c(:))
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public IterationMatrix getMatrix(int i0, int i1, int[] c) {
		IterationMatrix X = new IterationMatrix(i1 - i0 + 1, c.length);
		IterationObsNb[][] B = X.getArray();
		try {
			for (int i = i0; i <= i1; i++) {
				for (int j = 0; j < c.length; j++) {
					B[i - i0][j] = content[i][c[j]];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}

	/**
	 * Get a submatrix.
	 * 
	 * @param r  Array of row indices.
	 * @param j0 Initial column index
	 * @param j1 Final column index
	 * @return A(r(:),j0:j1)
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public IterationMatrix getMatrix(int[] r, int j0, int j1) {
		IterationMatrix X = new IterationMatrix(r.length, j1 - j0 + 1);
		IterationObsNb[][] B = X.getArray();
		try {
			for (int i = 0; i < r.length; i++) {
				for (int j = j0; j <= j1; j++) {
					B[i][j - j0] = content[r[i]][j];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}

	/**
	 * Set a single element.
	 * 
	 * @param i Row index.
	 * @param j Column index.
	 * @param s A(i,j).
	 * @exception ArrayIndexOutOfBoundsException
	 */

	public void set(int i, int j, IterationObsNb s) {
		content[i][j] = s;
	}

	public void set(int i, int j, int iterationNumbe, double value) {
		if (i < rowNb && j < colNb) {
			IterationObsNb iterationObsNb = content[i][j];
			iterationObsNb.setValue(iterationNumbe, value);
		}
	}


	/**
	 * Set a submatrix.
	 * 
	 * @param i0 Initial row index
	 * @param i1 Final row index
	 * @param j0 Initial column index
	 * @param j1 Final column index
	 * @param X  A(i0:i1,j0:j1)
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public void setMatrix(int i0, int i1, int j0, int j1, IterationMatrix X) {
		try {
			for (int i = i0; i <= i1; i++) {
				for (int j = j0; j <= j1; j++) {
					content[i][j] = X.get(i - i0, j - j0);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
	}

	/**
	 * Set a submatrix.
	 * 
	 * @param r Array of row indices.
	 * @param c Array of column indices.
	 * @param X A(r(:),c(:))
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public void setMatrix(int[] r, int[] c, IterationMatrix X) {
		try {
			for (int i = 0; i < r.length; i++) {
				for (int j = 0; j < c.length; j++) {
					content[r[i]][c[j]] = X.get(i, j);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
	}

	/**
	 * Set a submatrix.
	 * 
	 * @param r  Array of row indices.
	 * @param j0 Initial column index
	 * @param j1 Final column index
	 * @param X  A(r(:),j0:j1)
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public void setMatrix(int[] r, int j0, int j1, IterationMatrix X) {
		try {
			for (int i = 0; i < r.length; i++) {
				for (int j = j0; j <= j1; j++) {
					content[r[i]][j] = X.get(i, j - j0);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
	}

	/**
	 * Set a submatrix.
	 * 
	 * @param i0 Initial row index
	 * @param i1 Final row index
	 * @param c  Array of column indices.
	 * @param X  A(i0:i1,c(:))
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public void setMatrix(int i0, int i1, int[] c, IterationMatrix X) {
		try {
			for (int i = i0; i <= i1; i++) {
				for (int j = 0; j < c.length; j++) {
					content[i][c[j]] = X.get(i - i0, j);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
	}

	/**
	 * Matrix transpose.
	 * 
	 * @return A'
	 */

	public IterationMatrix transpose() {
		IterationMatrix X = new IterationMatrix(colNb, rowNb);
		IterationObsNb[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[j][i] = content[i][j];
			}
		}
		return X;
	}

	/**
	 * One norm
	 * 
	 * @return maximum column sum.
	 */

	public double norm1() {
		double f = 0;
		for (int j = 0; j < colNb; j++) {
			double s = 0;
			for (int i = 0; i < rowNb; i++) {
				s += Math.abs((content[i][j]).getTotalValue());
			}
			f = Math.max(f, s);
		}
		return f;
	}

	/**
	 * Infinity norm
	 * 
	 * @return maximum row sum.
	 */

	public double normInf() {
		// IterationObsNb f = new IterationObsNb();
		double f = 0.0;
		for (int i = 0; i < rowNb; i++) {
			double s = 0;
			for (int j = 0; j < colNb; j++) {
				s += Math.abs((content[i][j]).getTotalValue());
			}
			f = Math.max(f, s);
		}
		return f;
	}

	/**
	 * Frobenius norm
	 * 
	 * @return sqrt of sum of squares of all elements.
	 */

	public double normF() {
		double f = 0.0;
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				f = Maths.hypot(f, (content[i][j]).getTotalValue());
			}
		}
		return f;
	}

	/**
	 * Unary minus
	 * 
	 * @return -A
	 */

	public IterationMatrix uminus() {
		IterationMatrix X = new IterationMatrix(rowNb, colNb);
		IterationObsNb[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = (content[i][j]).uminus();
			}
		}
		return X;
	}

	/**
	 * C = A + B
	 * 
	 * @param toAdd another matrix
	 * @return A + B
	 */

	public IterationMatrix plus(IterationMatrix toAdd) {
		double thisSum = this.getSum();
		double toAddSum = toAdd.getSum();
		checkMatrixDimensions(toAdd);
		IterationMatrix result = new IterationMatrix(rowNb, colNb);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				IterationObsNb itemToAdd = toAdd.get(i, j);
				item = item.plus(itemToAdd);
				result.set(i, j, item);
			}
		}
		double resultSum = result.getSum();
		if (Math.abs(resultSum - (thisSum + toAddSum)) > 0.1) {
			System.out.print("IterationMatrix.plus : pb");
		}
		return result;
	}

	/**
	 * A = A + B
	 * 
	 * @param B another matrix
	 * @return A + B
	 */

	public IterationMatrix plusEquals(IterationMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				item.plus(B.content[i][j]);
				content[i][j] = item;
			}
		}
		return this;
	}

	/**
	 * C = A - B
	 * 
	 * @param B another matrix
	 * @return A - B
	 */

	public IterationMatrix _minus(IterationMatrix B) {
		checkMatrixDimensions(B);
		IterationMatrix X = new IterationMatrix(rowNb, colNb);
		IterationObsNb[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				// C[i][j] = content[i][j] - B.content[i][j];
				IterationObsNb item = content[i][j];
				item.minus(B.content[i][j]);
				C[i][j] = item;
			}
		}
		return X;
	}

	public IterationMatrix minus(IterationMatrix toWithdraw) {
		double thisSum = this.getSum();
		double toAddSum = toWithdraw.getSum();
		checkMatrixDimensions(toWithdraw);
		IterationMatrix result = new IterationMatrix(rowNb, colNb);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				IterationObsNb itemToWithdraw = toWithdraw.get(i, j);
				item = item.minus(itemToWithdraw);
				result.set(i, j, item);
			}
		}
		double resultSum = result.getSum();
		if (Math.abs(resultSum - (thisSum + toAddSum)) > 0.1) {
			System.out.print("IterationMatrix.plus : pb");
		}
		return result;
	}

	/**
	 * A = A - B
	 * 
	 * @param B another matrix
	 * @return A - B
	 */

	public IterationMatrix minusEquals(IterationMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				item.minus(B.content[i][j]);
				content[i][j] = item; // content[i][j] - B.content[i][j];
			}
		}
		return this;
	}




	public IterationMatrix auxApplyDivisor(double divisor) {
		IterationMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				IterationObsNb toSet = (content[rowIdx][colIdx]).auxApplyDivisor(divisor);
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	public IterationMatrix auxApplyRound() {
		IterationMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				IterationObsNb toSet = (content[rowIdx][colIdx]).auxApplyRound();
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}


	/**
	 * Multiply a matrix by a scalar, C = s*A
	 * 
	 * @param s scalar
	 * @return s*A
	 */

	public IterationMatrix times(double factor) {
		IterationMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				IterationObsNb toSet = (content[rowIdx][colIdx]).times(factor);
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	/**
	 * Multiply a matrix by a scalar in place, A = s*A
	 * 
	 * @param s scalar
	 * @return replace A by s*A
	 */

	public IterationMatrix timesEquals(double s) {
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb item = content[i][j];
				content[i][j] = item.times(s);
			}
		}
		return this;
	}



	/**
	 * Matrix trace.
	 * 
	 * @return sum of the diagonal elements.
	 */

	public double trace() {
		double t = 0;
		for (int i = 0; i < Math.min(rowNb, colNb); i++) {
			t += (content[i][i]).getTotalValue();
		}
		return t;
	}

	/**
	 * Print the matrix to stdout. Line the elements up in columns with a
	 * Fortran-like 'Fw.d' style format.
	 * 
	 * @param w Column width.
	 * @param d Number of digits after the decimal.
	 */

	public void print(int w, int d) {
		print(new PrintWriter(System.out, true), w, d);
	}

	/**
	 * Print the matrix to the output stream. Line the elements up in columns with a
	 * Fortran-like 'Fw.d' style format.
	 * 
	 * @param output Output stream.
	 * @param w      Column width.
	 * @param d      Number of digits after the decimal.
	 */

	public void print(PrintWriter output, int w, int d) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(d);
		format.setMinimumFractionDigits(d);
		format.setGroupingUsed(false);
		print(output, format, w + 2);
	}

	/**
	 * Print the matrix to stdout. Line the elements up in columns. Use the format
	 * object, and right justify within columns of width characters. Note that is
	 * the matrix is to be read back in, you probably will want to use a
	 * NumberFormat that is set to US Locale.
	 * 
	 * @param format A Formatting object for individual elements.
	 * @param width  Field width for each column.
	 * @see java.text.DecimalFormat#setDecimalFormatSymbols
	 */

	public void print(NumberFormat format, int width) {
		print(new PrintWriter(System.out, true), format, width);
	}

	// DecimalFormat is a little disappointing coming from Fortran or C's printf.
	// Since it doesn't pad on the left, the elements will come out different
	// widths. Consequently, we'll pass the desired column width in as an
	// argument and do the extra padding ourselves.

	/**
	 * Print the matrix to the output stream. Line the elements up in columns. Use
	 * the format object, and right justify within columns of width characters. Note
	 * that is the matrix is to be read back in, you probably will want to use a
	 * NumberFormat that is set to US Locale.
	 * 
	 * @param output the output stream.
	 * @param format A formatting object to format the matrix elements
	 * @param width  Column width.
	 * @see java.text.DecimalFormat#setDecimalFormatSymbols
	 */

	public void print(PrintWriter output, NumberFormat format, int width) {
		output.println(); // start on new line.
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				String s = format.format(content[i][j]); // format the number
				int padding = Math.max(1, width - s.length()); // At _least_ 1 space
				for (int k = 0; k < padding; k++)
					output.print(' ');
				output.print(s);
			}
			output.println();
		}
		output.println(); // end with blank line.
	}

	/**
	 * Read a matrix from a stream. The format is the same the print method, so
	 * printed matrices can be read back in (provided they were printed using US
	 * Locale). Elements are separated by whitespace, all the elements for each row
	 * appear on a single line, the last row is followed by a blank line.
	 * 
	 * @param input the input stream.
	 */

	public static IterationMatrix read(BufferedReader input) throws java.io.IOException {
		StreamTokenizer tokenizer = new StreamTokenizer(input);

		// Although StreamTokenizer will parse numbers, it doesn't recognize
		// scientific notation (E or D); however, Double.valueOf does.
		// The strategy here is to disable StreamTokenizer's number parsing.
		// We'll only get whitespace delimited words, EOL's and EOF's.
		// These words should all be numbers, for Double.valueOf to parse.

		tokenizer.resetSyntax();
		tokenizer.wordChars(0, 255);
		tokenizer.whitespaceChars(0, ' ');
		tokenizer.eolIsSignificant(true);
		java.util.Vector<Double> vD = new java.util.Vector<Double>();

		// Ignore initial empty lines
		while (tokenizer.nextToken() == StreamTokenizer.TT_EOL)
			;
		if (tokenizer.ttype == StreamTokenizer.TT_EOF)
			throw new java.io.IOException("Unexpected EOF on matrix read.");
		do {
			vD.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st row.
		} while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

		int n = vD.size(); // Now we've got the number of columns!
		double row[] = new double[n];
		for (int j = 0; j < n; j++) // extract the elements of the 1st row.
			row[j] = vD.elementAt(j).doubleValue();
		java.util.Vector<double[]> v = new java.util.Vector<double[]>();
		v.addElement(row); // Start storing rows instead of columns.
		while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
			// While non-empty lines
			v.addElement(row = new double[n]);
			int j = 0;
			do {
				if (j >= n)
					throw new java.io.IOException("Row " + v.size() + " is too long.");
				row[j++] = Double.valueOf(tokenizer.sval).doubleValue();
			} while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
			if (j < n)
				throw new java.io.IOException("Row " + v.size() + " is too short.");
		}
		int m = v.size(); // Now we've got the number of rows.
		IterationObsNb[][] A = new IterationObsNb[m][];
		v.copyInto(A); // copy the rows out of the vector
		return new IterationMatrix(A);
	}

	/*
	 * ------------------------ Private Methods ------------------------
	 */

	/** Check if size(A) == size(B) **/

	private void checkMatrixDimensions(IterationMatrix B) {
		if (B.rowNb != rowNb || B.colNb != colNb) {
			throw new IllegalArgumentException("Matrix dimensions must agree.");
		}
	}

	public double getRowSum(int rowIdx) {
		double result = 0;
		if (rowIdx < this.rowNb) {
			for (int colIdx = 0; colIdx < this.colNb; colIdx++) {
				result += (content[rowIdx][colIdx]).getTotalValue();
			}
		}
		return result;
	}

	@DisableJson
	public double[] getRowSums() {
		double[] result = new double[this.rowNb];
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			result[rowIdx] = getRowSum(rowIdx);
		}
		return result;
	}

	@DisableJson
	public Map<Integer, Double> getMapRowSums() {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			result.put(rowIdx, getRowSum(rowIdx));
		}
		return result;
	}

	@DisableJson
	public double getSum() {
		double result = 0;
		for (IterationObsNb d : this.getRowPackedCopy()) {
			result += d.getTotalValue();
		}
		return result;
	}

	public DoubleMatrix generateTotalMatrix() {
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			for (int columnIdx = 0; columnIdx < this.colNb; columnIdx++) {
				IterationObsNb iterObsNb = content[rowIdx][columnIdx];
				result.set(rowIdx, columnIdx, iterObsNb.getTotalValue());
			}
		}
		return result;
	}

	public DoubleMatrix generateMatrixAtIteration(int iterationNumber) {
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			for (int columnIdx = 0; columnIdx < this.colNb; columnIdx++) {
				IterationObsNb item = content[rowIdx][columnIdx];
				if (item.hasValue(iterationNumber)) {
					result.set(rowIdx, columnIdx, item.getValue(iterationNumber));
				}
			}
		}
		return result;
	}

	public String format3d(List<Integer> matrixIterations) {
		StringBuffer result = new StringBuffer();
		result.append("[");
		String sep1 = "";
		for (int rowIdx = 0; rowIdx < rowNb; rowIdx++) {
			result.append(sep1);
			result.append("[");
			String cellSeparator = "";
			for (int colIdx = 0; colIdx < colNb; colIdx++) {
				IterationObsNb value = content[rowIdx][colIdx];
				result.append(cellSeparator);
				String svalue = value.format3d(matrixIterations);
				result.append(svalue);
				cellSeparator = " ";
			}
			result.append("]");
			sep1 = SapereUtil.CR;
			// result.append(CR);
		}
		result.append("]");
		return result.toString();
	}

	@Override
	public String toString() {
		return format3d(null);
	}

	public static IterationMatrix auxComputeWeightedAvgIterationMatrix(List<Double> listWeights, List<IterationMatrix> listMatrix) {
		if(listMatrix.size() == 0) {
			return null;
		}
		if(listMatrix.size() != listWeights.size()) {
			return null;
		}
		IterationMatrix firstMatrix = listMatrix.get(0);
		IterationMatrix sumMatrix = new IterationMatrix(firstMatrix.getRowDimension(), firstMatrix.getColumnDimension());
		int idx = 0;
		double weightSum = 0;
		for(IterationMatrix nextMatrix : listMatrix) {
			double weight = listWeights.get(idx);
			weightSum+=weight;
			IterationMatrix toAdd = nextMatrix.times(weight);
			sumMatrix = sumMatrix.plus(toAdd);
			idx++;
		}
		return sumMatrix.auxApplyDivisor(weightSum);
	}

	public static IterationMatrix auxComputeSumIterationMatrix(List<IterationMatrix> listMatrix) {
		if(listMatrix.size() == 0) {
			return null;
		}
		IterationMatrix firstMatrix = listMatrix.get(0);
		IterationMatrix sumMatrix = new IterationMatrix(firstMatrix.getRowDimension(), firstMatrix.getColumnDimension());
		for(IterationMatrix nextMatrix : listMatrix) {
			sumMatrix = sumMatrix.plus(nextMatrix);
		}
		return sumMatrix;
	}

	public static IterationMatrix auxComputeAvgIterationMatrix(List<IterationMatrix> listMatrix) {
		if(listMatrix.size() == 0) {
			return null;
		}
		IterationMatrix sumMatrix = auxComputeSumIterationMatrix(listMatrix);
		int divisor = listMatrix.size();
		IterationMatrix result = sumMatrix.auxApplyDivisor(divisor);
		return result;
	}

	public static IterationMatrix auxAggregate(Map<String, IterationMatrix> mapObjects
			, Map<String, Double> weightsTable
			, AgentAuthentication agentAuthentication, AbstractLogger logger) {
		IterationMatrix result = null;
		List<IterationMatrix> listMatrix = new ArrayList<IterationMatrix>();
		List<Double> listWeights = new ArrayList<Double>();
		for (String nextNode : mapObjects.keySet()) {
			IterationMatrix nextObj = mapObjects.get(nextNode);
			if (nextObj instanceof IterationMatrix) {
				IterationMatrix matrix = (IterationMatrix) nextObj;
				listMatrix.add(matrix);
				if(weightsTable.containsKey(nextNode)) {
					double weight = weightsTable.get(nextNode);
					listWeights.add(weight);
				}
			}
		}
		if (listMatrix.size() == 0) {
			return result;
		}
		result = auxComputeWeightedAvgIterationMatrix(listWeights, listMatrix);
		/*
		if ("avg".equals(operator)) {
			result = auxComputeAvgIterationMatrix(listMatrix);
		}
		if ("w_avg".equals(operator)) {
			result = auxComputeWeightedAvgIterationMatrix(listWeights, listMatrix);
		}
		if ("sum".equals(operator)) {
			result = auxComputeSumIterationMatrix(listMatrix);
		}*/
		/*
		 if (result == null) {
					result = new IterationMatrix(matrix.getRowDimension(), matrix.getColumnDimension());
				}
		 * */
		return result;
	}


	public List<Integer> generateIterations() {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				IterationObsNb itValues = content[i][j];
				for (Integer nextIteration : itValues.getIerations()) {
					if(!result.contains(nextIteration)) {
						result.add(nextIteration);
					}
				}
			}
		}
		Collections.sort(result);
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(obj instanceof IterationMatrix) {
			IterationMatrix other = (IterationMatrix) obj;
			String sOther = other.zip();
			String sCurrent= this.zip();
			return sCurrent.equals(sOther);
		}
		return false;
	}

	public boolean isComplete() {
		for (int rowIdx = 0; rowIdx <rowNb; rowIdx++) {
			double rowSum = this.getRowSum(rowIdx);
			if (rowSum == 0) {
				return false;
			}
		}
		return true;
	}

	public String zip() {
		StringBuffer result = new StringBuffer();
		IterationObsNb[] flatAttay = getRowPackedCopy();
		String sep1 = "";
		for(IterationObsNb nextMap : flatAttay) {
			result.append(sep1);
			String sep2 = "";
			for(Integer nextIteration : nextMap.getIerations()) {
				Double value = nextMap.getValue(nextIteration);
				result.append(sep2).append(nextIteration).append(":").append(value);
				sep2=",";
			}
			sep1=";";
		}
		return result.toString();
	}

	public static IterationMatrix unzip(String zipContent) {
		String zipContent2 = zipContent;
		if(zipContent2.endsWith(";")) {
			zipContent2 = zipContent2 + " ";
		}
		String[] sflat = zipContent2.split(";");
		int size = (int) Math.round(Math.sqrt(sflat.length));
		IterationMatrix result = new IterationMatrix(size, size);
		for (int rowIndex = 0; rowIndex < size; rowIndex++) {
			for (int colIndex = 0; colIndex < size; colIndex++) {
				int index = size*rowIndex + colIndex;
				String sMap = sflat[index];
				String[] keyvalues = sMap.split(",");
				for (String nextKeyvalue : keyvalues) {
					String[] keyvalue = nextKeyvalue.split(":");
					if(keyvalue.length == 2) {
						int nextIteration =  Integer.valueOf(keyvalue[0]);
						double nextValue = Double.valueOf(keyvalue[1]);
						result.set(rowIndex, colIndex, nextIteration, nextValue);
					}
				}
			}
		}
		return result;
	}

	public boolean incrementObsNumber(int rowIdx, int columnIdx, int ieterationId) {
		boolean result = false;
		if (rowIdx < rowNb && columnIdx < colNb) {
			IterationObsNb iterationObsNb = this.get(rowIdx, columnIdx);
			int increment = 1;
			//increment = 100; // TO DELETE !!!!!!!!!!!!!!
			if (iterationObsNb.hasValue(ieterationId)) {
				iterationObsNb.setValue(ieterationId, increment + iterationObsNb.getValue(ieterationId));
			} else {
				iterationObsNb.setValue(ieterationId, increment);
			}
			result = true;
		}
		return result;
	}

}
