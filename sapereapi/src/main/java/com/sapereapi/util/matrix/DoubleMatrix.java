package com.sapereapi.util.matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.model.HandlingException;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class DoubleMatrix implements Cloneable, java.io.Serializable {

	private static final long serialVersionUID = 1;

	/*
	 * ------------------------ Class variables ------------------------
	 */

	/**
	 * Array for internal storage of elements.
	 * 
	 * @serial internal array storage.
	 */
	protected double[][] content;

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

	public DoubleMatrix(int _aRowNb, int _aColNb) {
		this.rowNb = _aRowNb;
		this.colNb = _aColNb;
		content = new double[_aRowNb][_aColNb];
	}

	/**
	 * Construct an m-by-n constant matrix.
	 * 
	 * @param _rowNb Number of rows.
	 * @param _colNb Number of colums.
	 * @param s Fill the matrix with this scalar value.
	 */

	public DoubleMatrix(int _rowNb, int _colNb, double s) {
		this.rowNb = _rowNb;
		this.colNb = _colNb;
		content = new double[_rowNb][_colNb];
		for (int i = 0; i < _rowNb; i++) {
			for (int j = 0; j < _colNb; j++) {
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

	public DoubleMatrix(double[][] _content) {
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
	 * @param m Number of rows.
	 * @param n Number of colums.
	 */

	public DoubleMatrix(double[][] _content, int m, int n) {
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

	public DoubleMatrix(double vals[], int m) {
		this.rowNb = m;
		colNb = (m != 0 ? vals.length / m : 0);
		if (m * colNb != vals.length) {
			throw new IllegalArgumentException("Array length must be a multiple of m.");
		}
		content = new double[m][colNb];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = vals[i + j * m];
			}
		}
	}

	public List<Integer> getDimensions() {
		List<Integer> result = new ArrayList<Integer>();
		result.add(rowNb);
		result.add(colNb);
		return result;
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

	public static DoubleMatrix constructWithCopy(double[][] A) {
		int m = A.length;
		int n = A[0].length;
		DoubleMatrix X = new DoubleMatrix(m, n);
		double[][] C = X.getArray();
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

	public DoubleMatrix copy() {
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = content[i][j];
			}
		}
		return X;
	}

	/**
	 * Clone the Matrix object.
	 */

	public Object clone() {
		return this.copy();
	}

	/**
	 * Access the internal two-dimensional array.
	 * 
	 * @return Pointer to the two-dimensional array of matrix elements.
	 */

	public double[][] getArray() {
		return content;
	}

	/**
	 * Copy the internal two-dimensional array.
	 * 
	 * @return Two-dimensional array copy of matrix elements.
	 */

	public double[][] getArrayCopy() {
		double[][] C = new double[rowNb][colNb];
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

	public double[] getColumnPackedCopy() {
		double[] vals = new double[rowNb * colNb];
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

	public double[] getRowPackedCopy() {
		double[] vals = new double[rowNb * colNb];
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

	public double get(int i, int j) {
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

	public DoubleMatrix getMatrix(int i0, int i1, int j0, int j1) {
		DoubleMatrix X = new DoubleMatrix(i1 - i0 + 1, j1 - j0 + 1);
		double[][] B = X.getArray();
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


	public DoubleMatrix getColumnMatrix(int colIndex) {
		return getMatrix(0, this.rowNb-1, colIndex, colIndex);
	}

	public DoubleMatrix getRowMatrix(int rowIndex) {
		return getMatrix(rowIndex, rowIndex, 0, this.colNb -1);
	}

	/**
	 * Get a submatrix.
	 * 
	 * @param r Array of row indices.
	 * @param c Array of column indices.
	 * @return A(r(:),c(:))
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public DoubleMatrix getMatrix(int[] r, int[] c) {
		DoubleMatrix X = new DoubleMatrix(r.length, c.length);
		double[][] B = X.getArray();
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

	public DoubleMatrix getMatrix(int i0, int i1, int[] c) {
		DoubleMatrix X = new DoubleMatrix(i1 - i0 + 1, c.length);
		double[][] B = X.getArray();
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

	public DoubleMatrix getMatrix(int[] r, int j0, int j1) {
		DoubleMatrix X = new DoubleMatrix(r.length, j1 - j0 + 1);
		double[][] B = X.getArray();
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

	public void set(int i, int j, double s) {
		content[i][j] = s;
	}

	/**
	 * Set a submatrix.
	 * 
	 * @param iMin Initial row index
	 * @param iMax Final row index
	 * @param jMin Initial column index
	 * @param jMax Final column index
	 * @param X  A(i0:i1,j0:j1)
	 * @exception ArrayIndexOutOfBoundsException Submatrix indices
	 */

	public void setMatrix(int iMin, int iMax, int jMin, int jMax, DoubleMatrix X) {
		try {
			for (int i = iMin; i <= iMax; i++) {
				for (int j = jMin; j <= jMax; j++) {
					content[i][j] = X.get(i - iMin, j - jMin);
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

	public void setMatrix(int[] r, int[] c, DoubleMatrix X) {
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

	public void setMatrix(int[] r, int j0, int j1, DoubleMatrix X) {
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

	public void setMatrix(int i0, int i1, int[] c, DoubleMatrix X) {
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

	public DoubleMatrix transpose() {
		DoubleMatrix X = new DoubleMatrix(colNb, rowNb);
		double[][] C = X.getArray();
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
				s += Math.abs(content[i][j]);
			}
			f = Math.max(f, s);
		}
		return f;
	}

	/**
	 * Two norm
	 * 
	 * @return maximum singular value.
	 */

	public double norm2() {
		return (new SingularValueDecomposition(this).norm2());
	}

	/**
	 * Infinity norm
	 * 
	 * @return maximum row sum.
	 */

	public double normInf() {
		double f = 0;
		for (int i = 0; i < rowNb; i++) {
			double s = 0;
			for (int j = 0; j < colNb; j++) {
				s += Math.abs(content[i][j]);
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
		double f = 0;
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				f = Maths.hypot(f, content[i][j]);
			}
		}
		return f;
	}

	/**
	 * Unary minus
	 * 
	 * @return -A
	 */

	public DoubleMatrix uminus() {
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = -content[i][j];
			}
		}
		return X;
	}

	/**
	 * C = A + B
	 * 
	 * @param B another matrix
	 * @return A + B
	 */

	public DoubleMatrix plus(DoubleMatrix B) {
		checkMatrixDimensions(B);
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		double[][] contentResult = result.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				contentResult[i][j] = content[i][j] + B.content[i][j];
			}
		}
		return result;
	}

	public DoubleMatrix addColumnVector(DoubleMatrix B) {
		//checkMatrixDimensions(B);
		if (B.rowNb != rowNb || B.colNb != 1) {
			throw new IllegalArgumentException("AddColumnVector : matrix dimensions must agree with " + rowNb + " and 1.");
		}
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		//double[][] contentResult = result.getArray();
		for (int i = 0; i < rowNb; i++) {
			double vectorValue = B.get(i, 0);
			for (int j = 0; j < colNb; j++) {
				result.set(i, j, content[i][j] + vectorValue);
			}
		}
		return result;
	}

	/**
	 * A = A + B
	 * 
	 * @param B another matrix
	 * @return A + B
	 */

	public DoubleMatrix plusEquals(DoubleMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = content[i][j] + B.content[i][j];
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

	public DoubleMatrix minus(DoubleMatrix B) {
		checkMatrixDimensions(B);
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = content[i][j] - B.content[i][j];
			}
		}
		return X;
	}

	/**
	 * A = A - B
	 * 
	 * @param B another matrix
	 * @return A - B
	 */

	public DoubleMatrix minusEquals(DoubleMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = content[i][j] - B.content[i][j];
			}
		}
		return this;
	}


	/**
	 * Element-by-element right division, C = A./B
	 * 
	 * @param B another matrix
	 * @return A./B
	 */

	public DoubleMatrix arrayRightDivide(DoubleMatrix B) {
		checkMatrixDimensions(B);
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = content[i][j] / B.content[i][j];
			}
		}
		return X;
	}

	public DoubleMatrix addScalar(double toAdd) {
		DoubleMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				double toSet = content[rowIdx][colIdx] + toAdd;
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	public DoubleMatrix divideByScalar(double divisor) {
		DoubleMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				double toSet = (content[rowIdx][colIdx]) / divisor;
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	public DoubleMatrix auxApplyRound() {
		DoubleMatrix result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				double toSet = Math.round(content[rowIdx][colIdx]);
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	/**
	 * Element-by-element right division in place, A = A./B
	 * 
	 * @param B another matrix
	 * @return A./B
	 */

	public DoubleMatrix arrayRightDivideEquals(DoubleMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = content[i][j] / B.content[i][j];
			}
		}
		return this;
	}

	/**
	 * Element-by-element left division, C = A.\B
	 * 
	 * @param B another matrix
	 * @return A.\B
	 */

	public DoubleMatrix arrayLeftDivide(DoubleMatrix B) {
		checkMatrixDimensions(B);
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = B.content[i][j] / content[i][j];
			}
		}
		return X;
	}

	/**
	 * Element-by-element left division in place, A = A.\B
	 * 
	 * @param B another matrix
	 * @return A.\B
	 */

	public DoubleMatrix arrayLeftDivideEquals(DoubleMatrix B) {
		checkMatrixDimensions(B);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = B.content[i][j] / content[i][j];
			}
		}
		return this;
	}

	/**
	 * Multiply a matrix by a scalar, C = s*A
	 * 
	 * @param s scalar
	 * @return s*A
	 */

	public DoubleMatrix multiplyByScalar(double s) {
		DoubleMatrix X = new DoubleMatrix(rowNb, colNb);
		double[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				C[i][j] = s * content[i][j];
			}
		}
		return X;
	}

	/**
	 * Multiply a matrix by a scalar in place, A = s*A
	 * 
	 * @param s scalar
	 * @return replace A by s*A
	 */

	public DoubleMatrix timesEquals(double s) {
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = s * content[i][j];
			}
		}
		return this;
	}

	/**
	 * Apply matrix multiplication
	 * @param matrixB
	 * @return
	 */
	public DoubleMatrix multiplyByMatrix(DoubleMatrix matrixB) {
		return DoubleMatrix.matrixMultiply(this, matrixB);
	}

	/**
	 * mulitply only cell by cell.
	 * @param matrixB
	 * @return
	 */
	public DoubleMatrix simpleMultiplyByMatrix(DoubleMatrix matrixB) {
		return DoubleMatrix.simpleMultiply(this, matrixB);
	}


	/**
	 * LU Decomposition
	 * 
	 * @return LUDecomposition
	 * @see LUDecomposition
	 */

	public LUDecomposition lu() {
		return new LUDecomposition(this);
	}

	/**
	 * QR Decomposition
	 * 
	 * @return QRDecomposition
	 * @see QRDecomposition
	 */

	public QRDecomposition qr() {
		return new QRDecomposition(this);
	}

	/**
	 * Cholesky Decomposition
	 * 
	 * @return CholeskyDecomposition
	 * @see CholeskyDecomposition
	 */

	public CholeskyDecomposition chol() {
		return new CholeskyDecomposition(this);
	}

	/**
	 * Singular Value Decomposition
	 * 
	 * @return SingularValueDecomposition
	 * @see SingularValueDecomposition
	 */

	public SingularValueDecomposition svd() {
		return new SingularValueDecomposition(this);
	}

	/**
	 * Eigenvalue Decomposition
	 * 
	 * @return EigenvalueDecomposition
	 * @see EigenvalueDecomposition
	 */

	public EigenvalueDecomposition eig() {
		return new EigenvalueDecomposition(this);
	}

	/**
	 * Solve A*X = B
	 * 
	 * @param B right hand side
	 * @return solution if A is square, least squares solution otherwise
	 */

	public DoubleMatrix solve(DoubleMatrix B) {
		return (rowNb == colNb ? (new LUDecomposition(this)).solve(B) : (new QRDecomposition(this)).solve(B));
	}

	/**
	 * Solve X*A = B, which is also A'*X' = B'
	 * 
	 * @param B right hand side
	 * @return solution if A is square, least squares solution otherwise.
	 */

	public DoubleMatrix solveTranspose(DoubleMatrix B) {
		return transpose().solve(B.transpose());
	}

	/**
	 * Matrix inverse or pseudoinverse
	 * 
	 * @return inverse(A) if A is square, pseudoinverse otherwise.
	 */

	private final static String FUNCTION_TANH = "tanh";
	private final static String FUNCTION_EXP = "exp";

	public DoubleMatrix inverse() {
		return solve(identity(rowNb, rowNb));
	}

	public DoubleMatrix applyCellFunction(String functionCode) {
		DoubleMatrix result = new DoubleMatrix(this.rowNb, this.colNb);
		for(int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			for(int colIdx = 0; colIdx < this.colNb; colIdx++) {
				double cellValue1 = this.get(rowIdx, colIdx);
				double cellValue2 = 0;
				if(FUNCTION_TANH.equals(functionCode)) {
					cellValue2 = Math.tanh(cellValue1);
				} else if(FUNCTION_EXP.equals(functionCode)) {
					cellValue2 = Math.exp(cellValue1);
				}
				result.set(rowIdx, colIdx, Math.tanh(cellValue2));
			}
		}
		return result;
	}

	public DoubleMatrix tanh() {
		return applyCellFunction(FUNCTION_TANH);
	}

	public DoubleMatrix exp() {
		return applyCellFunction(FUNCTION_EXP);
	}

	public static DoubleMatrix hardSigmoid_0(DoubleMatrix X) {
		return aux_hardSigmoid(X, 5., 0.5);
	}

	public static DoubleMatrix hardSigmoid_1(DoubleMatrix X) {
		return aux_hardSigmoid(X, 6., 0.5);
	}

	public static DoubleMatrix aux_hardSigmoid(DoubleMatrix X, double divisor, double shift) {
		X = X.divideByScalar(divisor).addScalar(shift);
		DoubleMatrix clippedX = new DoubleMatrix(X.getRowDimension(), X.getColumnDimension());
		for (int rowIdx = 0; rowIdx < X.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < X.getColumnDimension(); colIdx++) {
				Double cell = X.get(rowIdx, colIdx);
				Double cell2 = cell;
				if (cell > 1) {
					cell2 = 1.0;
				} else if (cell < 0) {
					cell2 = 0.0;
				}
				clippedX.set(rowIdx, colIdx, cell2);
			}
		}
		return clippedX;
	}

	public static DoubleMatrix sigmoid(DoubleMatrix X) {
		DoubleMatrix clippedX = new DoubleMatrix(X.getRowDimension(), X.getColumnDimension());
		for (int rowIdx = 0; rowIdx < X.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < X.getColumnDimension(); colIdx++) {
				// Double cell1 = X.get(rowIdx, colIdx);
				Double cell1 = X.get(rowIdx, colIdx);
				double div = 1 + Math.exp(-1 * cell1);
				double cell2 = 1 / div;
				clippedX.set(rowIdx, colIdx, cell2);
			}
		}
		return clippedX;
	}

	/**
	 * Matrix determinant
	 * 
	 * @return determinant
	 */

	public double det() {
		return new LUDecomposition(this).det();
	}

	/**
	 * Matrix rank
	 * 
	 * @return effective numerical rank, obtained from SVD.
	 */

	public int rank() {
		return new SingularValueDecomposition(this).rank();
	}

	/**
	 * Matrix condition (2 norm)
	 * 
	 * @return ratio of largest to smallest singular value.
	 */

	public double cond() {
		return new SingularValueDecomposition(this).cond();
	}

	/**
	 * Matrix trace.
	 * 
	 * @return sum of the diagonal elements.
	 */

	public double trace() {
		double t = 0;
		for (int i = 0; i < Math.min(rowNb, colNb); i++) {
			t += content[i][i];
		}
		return t;
	}

	/**
	 * Generate matrix with random elements
	 * 
	 * @param m Number of rows.
	 * @param n Number of colums.
	 * @return An m-by-n matrix with uniformly distributed random elements.
	 */

	public static DoubleMatrix random(int m, int n) {
		DoubleMatrix A = new DoubleMatrix(m, n);
		double[][] X = A.getArray();
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				X[i][j] = Math.random();
			}
		}
		return A;
	}

	/**
	 * Generate identity matrix
	 * 
	 * @param m Number of rows.
	 * @param n Number of colums.
	 * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
	 */

	public static DoubleMatrix identity(int m, int n) {
		DoubleMatrix A = new DoubleMatrix(m, n);
		double[][] X = A.getArray();
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				X[i][j] = (i == j ? 1.0 : 0.0);
			}
		}
		return A;
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


	public String dump() {
		DecimalFormat format = new DecimalFormat();
		StringBuffer result = new StringBuffer();
		String sep1 = "";
		for (int i = 0; i < rowNb; i++) {
			result.append(sep1);
			String sep2 = "";
			for (int j = 0; j < colNb; j++) {
				String nextDouble = format.format(content[i][j]); // format the number
				result.append(sep2).append(nextDouble);
				sep2=",";
			}
			sep1 = "|";
		}
		return result.toString();
	}


	public String zip() {
		StringBuffer result = new StringBuffer();
		double[] flatAttay = getRowPackedCopy();
		String sep1 = "";
		result.append(rowNb).append("X").append(colNb).append(":");
		for(double nextValue : flatAttay) {
			result.append(sep1);
			String sep2 = "";
			result.append(sep2).append(nextValue);
			sep1=";";
		}
		return result.toString();
	}

	public static List<Integer> extractDimension(String content, String separator) {
		String[] dimensionArray =  content.split(separator);
		List<Integer> result = new ArrayList<Integer>();
		for(String nextDim : dimensionArray) {
			result.add(Integer.valueOf(nextDim));
		}
		return result;
	}

	public static DoubleMatrix unzip(String zipContent) throws HandlingException {
		String zipContent2 = zipContent;
		if(zipContent2.endsWith(";")) {
			zipContent2 = zipContent2 + " ";
		}
		String[] array1 = zipContent2.split(":");
		if(array1.length == 2) {
			String dimension = array1[0];
			String values = array1[1];
			List<Integer> listDimensions = SapereUtil.extractDimensions(dimension, "X");
			if(listDimensions.size() == 2) {
				int rowNb = listDimensions.get(0);
				int colNb = listDimensions.get(1);
				String[] sValuesArray = values.split(";");
				if(sValuesArray.length == rowNb * colNb) {
					double[][] content = new double[rowNb][colNb];
					for (int rowIndex = 0; rowIndex < rowNb; rowIndex++) {
						for (int colIndex = 0; colIndex < colNb; colIndex++) {
							int valueIndex = colNb*rowIndex + colIndex;
							String sValue = sValuesArray[valueIndex];
							double value= Double.valueOf(sValue);
							content[rowIndex][colIndex] = value;
						}
					}
					DoubleMatrix result = new DoubleMatrix(content, rowNb, colNb);
					return result;
				}
			}
		}
		return null;
	}


	/**
	 * Read a matrix from a stream. The format is the same the print method, so
	 * printed matrices can be read back in (provided they were printed using US
	 * Locale). Elements are separated by whitespace, all the elements for each row
	 * appear on a single line, the last row is followed by a blank line.
	 * 
	 * @param input the input stream.
	 */

	public static DoubleMatrix read(BufferedReader input) throws java.io.IOException {
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
		double[][] A = new double[m][];
		v.copyInto(A); // copy the rows out of the vector
		return new DoubleMatrix(A);
	}

	/*
	 * ------------------------ Private Methods ------------------------
	 */

	/** Check if size(A) == size(B) **/

	private void checkMatrixDimensions(DoubleMatrix B) {
		if (B.rowNb != rowNb || B.colNb != colNb) {
			throw new IllegalArgumentException("Matrix dimensions must agree.");
		}
	}

	public double getRowSum(int rowIdx) {
		double result = 0;
		if (rowIdx < this.rowNb) {
			for (int colIdx = 0; colIdx < this.colNb; colIdx++) {
				result += content[rowIdx][colIdx];
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
		for (double d : this.getRowPackedCopy()) {
			result += d;
		}
		return result;
	}

	public String format3d() {
		// Matrix aMatrixNorm = normalize(aMatrix);
		StringBuffer result = new StringBuffer();
		result.append("(").append(rowNb).append("X").append(colNb).append(")");
		result.append("[");
		String sep1 = "";
		for (int rowIdx = 0; rowIdx < rowNb; rowIdx++) {
			result.append(sep1);
			result.append("[");
			String cellSeparator = "";
			for (int colIdx = 0; colIdx < colNb; colIdx++) {
				double value = content[rowIdx][colIdx];
				if (true || value > 0) {
					result.append(cellSeparator);
					String svalue = UtilDates.df3.format(value);
					result.append(svalue);
					cellSeparator = " ";
				}
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
		return format3d();
	}

	public DoubleMatrix aggregate1(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, listObjects, agentAuthentication, logger);
	}

	public static DoubleMatrix aggregate2(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		DoubleMatrix result = null;
		List<DoubleMatrix> listMatrix = new ArrayList<>();
		for (Object nextObj : listObjects) {
			if (nextObj instanceof DoubleMatrix) {
				DoubleMatrix matrix = (DoubleMatrix) nextObj;
				listMatrix.add(result);
				if (result == null) {
					result = new DoubleMatrix(matrix.getRowDimension(), matrix.getColumnDimension());
				}
			}
		}
		if (listMatrix.size() == 0) {
			return result;
		}
		if ("avg".equals(operator)) {
			for (DoubleMatrix nextMatrix : listMatrix) {
				result = result.plus(nextMatrix);
			}
			int nbMatrix = listMatrix.size();
			result.divideByScalar(nbMatrix);
		}
		if ("sum".equals(operator)) {
			for (DoubleMatrix nextMatrix : listMatrix) {
				result = result.plus(nextMatrix);
			}
		}
		return null;
	}

	public static DoubleMatrix auxComputeSumMatrix(List<DoubleMatrix> listMatrix) {
		if(listMatrix.size() == 0) {
			return null;
		}
		DoubleMatrix firstMatrix = listMatrix.get(0);
		DoubleMatrix sumMatrix = new DoubleMatrix(firstMatrix.getRowDimension(), firstMatrix.getColumnDimension());
		for(DoubleMatrix nextMatrix : listMatrix) {
			sumMatrix = sumMatrix.plus(nextMatrix);
		}
		return sumMatrix;
	}

	public static DoubleMatrix auxComputeAvgMatrix(List<DoubleMatrix> listMatrix) {
		if(listMatrix.size() == 0) {
			return null;
		}
		DoubleMatrix sumMatrix = auxComputeSumMatrix(listMatrix);
		int divisor = listMatrix.size();
		//BasicMatrix result = auxApplyDivisor2(sumMatrix, divisor);
		DoubleMatrix result = (sumMatrix.divideByScalar(divisor)).auxApplyRound();
		return result;
	}

	public DoubleMatrix normalize() {
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		for (int rowIdx = 0; rowIdx <rowNb; rowIdx++) {
			// Compute row sum
			double rowSum = getRowSum(rowIdx);
			for (int colIdx = 0; colIdx < colNb; colIdx++) {
				// To normalize : divide each item by row sum
				result.set(rowIdx, colIdx, rowSum == 0 ? 0 : content[rowIdx][colIdx] / rowSum);
			}
		}
		return result;
	}

	//  Added functions
	public static DoubleMatrix zeros(int rowNb, int colNb) {
		return new DoubleMatrix(rowNb, colNb, 0.0);
	}

    public static DoubleMatrix loadMatrixFromFile(String filePath) throws HandlingException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream(filePath);
		if(stream == null) {
			stream = loader.getResourceAsStream("resources/"+filePath);
			System.out.println("loadProperties : step2 : stream = " + stream);
		}
		if(stream == null) {
			try {
				stream = new FileInputStream(new File(filePath));
			} catch (FileNotFoundException e) {
				throw new HandlingException(e.getMessage());
			}
		}
		if(stream == null) {
			System.out.println("### resource " +  filePath + " not loaded");
		}

        //FileInputStream fstream = new FileInputStream(filePath);
        double[][] target;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			List<List<Double>> loadedArrays = new ArrayList<List<Double>>();
			String strLine;
			while ((strLine = br.readLine()) != null) {
			    List<Double> row = new ArrayList<Double>();
			    String[] a = strLine.split(" ");
			    for (String s : a) {
			        row.add(Double.parseDouble(s));
			    }
			    loadedArrays.add(row);
			}
			br.close();
			int columns = loadedArrays.get(0).size();
			int rows = loadedArrays.size();
			target = new double[rows][columns];
			for (int i = 0; i < loadedArrays.size(); i++) {
			    for (int j = 0; j < target[i].length; j++) {
			        target[i][j] = (Double) loadedArrays.get(i).get(j);
			    }
			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			throw new HandlingException(e.getMessage());
		}
        return new DoubleMatrix(target);
    }


	/**
	 * Element-by-element multiplication, C = A*B
	 *
	 * @param matrixB another matrix
	 * @return A.*B
	 */

	public static DoubleMatrix matrixMultiply(DoubleMatrix matrixA, DoubleMatrix matrixB) {
		DoubleMatrix result = new DoubleMatrix(matrixA.getRowDimension(), matrixB.getColumnDimension());
		if(matrixA.getColumnDimension() != matrixB.getRowDimension()) {
			throw new IllegalArgumentException("column dimension of A and row dimension of B must be equal.");
		}
		for (int rowIdx = 0; rowIdx < matrixA.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < matrixB.getColumnDimension(); colIdx++) {
				double resultCell = 0;
				for (int k = 0; k < matrixA.getColumnDimension(); k++) {
					resultCell+= matrixA.get(rowIdx, k) * matrixB.get(k, colIdx);
				}
				result.set(rowIdx, colIdx, resultCell);
			}
		}
		return result;
	}


	public static DoubleMatrix simpleMultiply(DoubleMatrix matrixA, DoubleMatrix matrixB) {
		DoubleMatrix result = new DoubleMatrix(matrixA.getRowDimension(), matrixA.getColumnDimension());
		if(matrixA.getColumnDimension() != matrixB.getColumnDimension()) {
			throw new IllegalArgumentException("column dimension of A and B must be equal.");
		}
		if(matrixA.getRowDimension() != matrixB.getRowDimension()) {
			throw new IllegalArgumentException("row dimension of A and B must be equal.");
		}
		for (int rowIdx = 0; rowIdx < matrixA.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < matrixA.getColumnDimension(); colIdx++) {
				double resultCell = matrixA.get(rowIdx, colIdx) * matrixB.get(rowIdx, colIdx);
				result.set(rowIdx, colIdx, resultCell);
			}
		}
		return result;
	}

	public static DoubleMatrix applyDropout(DoubleMatrix matrix, double rate) {
		DoubleMatrix result = DoubleMatrix.zeros(matrix.getRowDimension(), matrix.getColumnDimension());
		for (int rowIdx = 0; rowIdx < matrix.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < matrix.getColumnDimension(); colIdx++) {
				double nextRandom =  Math.random();
				if(nextRandom >= rate ) {
					result.set(rowIdx, colIdx, matrix.get(rowIdx, colIdx));
				}
			}
		}
		double factor =  1 / (1 - rate);
		result = result.multiplyByScalar(factor);
		return result;
	}

	public DoubleMatrix getSubMatrix(MatrixWindow window) {
		DoubleMatrix result = this.getMatrix(window.getRowIndexBegin(), window.getRowIndexEnd()
				, window.getColumnIndexBegin(), window.getColumnIndexEnd());
		return result;
	}

	public static List<MatrixWindow> splitMatrixWindow(DoubleMatrix bigMatrix, int matrixNb, boolean applyOnColumns) {
		List<MatrixWindow> result = new ArrayList<MatrixWindow>();
		int nbRow = bigMatrix.getRowDimension();
		int nbColumns = bigMatrix.getColumnDimension();
		int dimension = applyOnColumns ? nbColumns : nbRow;
		if(dimension % matrixNb >0) {
			throw new IllegalArgumentException("splitMatrices : the " + (applyOnColumns ? "column" : "row") + " dimension of bigMatrix should be a multiple of " + matrixNb);
		}
		int rowBegin = 0;
		int columnBegin = 0;
		int splitRowNb = applyOnColumns ?  nbRow : nbRow/matrixNb;
		int splitColumnNb = applyOnColumns ? nbColumns/matrixNb : nbColumns;
		for(int idx =0; idx < matrixNb; idx++) {
			MatrixWindow nextWindow = new MatrixWindow(rowBegin, rowBegin+splitRowNb-1, columnBegin, columnBegin+splitColumnNb-1);
			result.add(nextWindow);
			if(applyOnColumns) {
				columnBegin+=splitColumnNb;
			} else {
				rowBegin += splitRowNb;
			}
		}
		return result;
	}

	public static List<DoubleMatrix> splitMatrices(DoubleMatrix bigMatrix, int matrixNb, boolean applyOnColumns) {
		List<DoubleMatrix> result = new ArrayList<DoubleMatrix>();
		int nbRow = bigMatrix.getRowDimension();
		int nbColumns = bigMatrix.getColumnDimension();
		int dimension = applyOnColumns ? nbColumns : nbRow;
		if(dimension % matrixNb >0) {
			throw new IllegalArgumentException("splitMatrices : the " + (applyOnColumns ? "column" : "row") + " dimension of bigMatrix should be a multiple of " + matrixNb);
		}
		int targetNbRow = dimension/matrixNb;
		int rowBegin = 0;
		int columnBegin = 0;
		int splitRowNb = applyOnColumns ?  nbRow : nbRow/matrixNb;
		int splitColumnNb = applyOnColumns ? nbColumns/matrixNb : nbColumns;
		for(int idx =0; idx < matrixNb; idx++) {
			DoubleMatrix nextMatrix = bigMatrix.getMatrix(rowBegin, rowBegin+splitRowNb-1, columnBegin, columnBegin+splitColumnNb-1);
			result.add(nextMatrix);
			if(applyOnColumns) {
				columnBegin+=splitColumnNb;
			} else {
				rowBegin += splitRowNb;
			}
		}
		return result;
	}

	public static DoubleMatrix concatenateMatrices(List<DoubleMatrix> listMatrices, boolean concatenateByColumn) {
		// apply on row dimension for the moment
		int rowDim = 0;
		int colDim = 0;
		for (DoubleMatrix nextMatrix : listMatrices) {
			if(concatenateByColumn) {
				colDim += nextMatrix.getColumnDimension();
				rowDim = nextMatrix.getRowDimension();
			} else {
				colDim = nextMatrix.getColumnDimension();
				rowDim += nextMatrix.getRowDimension();
			}
		}
		DoubleMatrix result = new DoubleMatrix(rowDim, colDim);
		if(concatenateByColumn) {
			int colIdx = 0;
			for (DoubleMatrix nextMatrix : listMatrices) {
				for (int colIdxNextMatrix = 0; colIdxNextMatrix < nextMatrix.getColumnDimension(); colIdxNextMatrix++) {
					for (int rowIdx = 0; rowIdx < nextMatrix.getRowDimension(); rowIdx++) {
						Double nextValue = nextMatrix.get(rowIdx, colIdxNextMatrix);
						result.set(rowIdx, colIdx, nextValue);
					}
					// next row
					colIdx++;
				}
			}
		} else {
			int rowIdx = 0;
			for (DoubleMatrix nextMatrix : listMatrices) {
				for (int rowIdxNextMatrix = 0; rowIdxNextMatrix < nextMatrix.getRowDimension(); rowIdxNextMatrix++) {
					for (int colIdx = 0; colIdx < nextMatrix.getColumnDimension(); colIdx++) {
						Double nextValue = nextMatrix.get(rowIdxNextMatrix, colIdx);
						result.set(rowIdx, colIdx, nextValue);
					}
					// next row
					rowIdx++;
				}
			}
		}
		return result;
	}

	public static DoubleMatrix auxAggregate(Map<String, DoubleMatrix> mapMatrices, Map<String, Double> weightsTable,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		if (mapMatrices.size() == 0) {
			return null;
		}
		if (mapMatrices.size() != weightsTable.size()) {
			return null;
		}
		DoubleMatrix firstMatrix = mapMatrices.values().iterator().next();
		int rowDimension = firstMatrix.getRowDimension();
		int columnDimension = firstMatrix.getColumnDimension();
		DoubleMatrix sumMatrix = new DoubleMatrix(firstMatrix.getRowDimension(), firstMatrix.getColumnDimension());
		double weightSum = 0;
		for (String agentName : mapMatrices.keySet()) {
			DoubleMatrix nextMatrix = mapMatrices.get(agentName);
			if (columnDimension != nextMatrix.getColumnDimension()) {
				logger.error("DoubleMatrix.auxAggregate : incompatible column dimension");
				return null;
			}
			if (rowDimension != nextMatrix.getRowDimension()) {
				logger.error("DoubleMatrix.auxAggregate : incompatible row dimension");
				return null;
			}
			double weight = weightsTable.get(agentName);
			weightSum += weight;
			DoubleMatrix toAdd = nextMatrix.multiplyByScalar(weight);
			sumMatrix = sumMatrix.plus(toAdd);
		}
		return sumMatrix.divideByScalar(weightSum);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DoubleMatrix) {
			DoubleMatrix other = (DoubleMatrix) obj;
			if(other.getRowDimension() == this.rowNb && other.getColumnDimension() == this.colNb) {
				boolean result = true;
				for (int i = 0; i < rowNb; i++) {
					for (int j = 0; j < colNb; j++) {
						double cellThis = get(i, j);
						double cellOther = other.get(i, j);
						boolean equals = Math.abs(cellThis - cellOther) <= 0.00001;
						result = result && equals;
					}
				}
				return result;
			}
		}
		return false;
	}

}
