package com.sapereapi.util.matrix;

import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class OldIterationMatrix2 implements Cloneable, java.io.Serializable {

	private static final long serialVersionUID = 1;

	/*
	 * ------------------------ Class variables ------------------------
	 */

	/**
	 * Array for internal storage of elements.
	 * 
	 * @serial internal array storage.
	 */
	protected Map<Integer, Double>[][] content;

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

	public OldIterationMatrix2(int _aRowNb, int _aColNb) {
		this.rowNb = _aRowNb;
		this.colNb = _aColNb;
		content = new HashMap[_aRowNb][_aColNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				content[i][j] = new HashMap<Integer, Double>();
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

	public OldIterationMatrix2(int m, int n, Map<Integer, Double> s) {
		this.rowNb = m;
		this.colNb = n;
		content = new Map[m][n];
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

	public OldIterationMatrix2(Map<Integer, Double>[][] _content) {
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

	public OldIterationMatrix2(Map<Integer, Double>[][] _content, int m, int n) {
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

	public OldIterationMatrix2(Map<Integer, Double> vals[], int m) {
		this.rowNb = m;
		colNb = (m != 0 ? vals.length / m : 0);
		if (m * colNb != vals.length) {
			throw new IllegalArgumentException("Array length must be a multiple of m.");
		}
		content = new Map[m][colNb];
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
	 * Make a deep copy of a matrix
	 */

	public OldIterationMatrix2 copy() {
		OldIterationMatrix2 result = new OldIterationMatrix2(rowNb, colNb);
		// result.setMaxIterationNumber(maxIterationNumber);
		Map<Integer, Double>[][] resultArray = result.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				resultArray[i][j] = content[i][j];
			}
		}
		return result;
	}

	/**
	 * Clone the Matrix object.
	 */

	public OldIterationMatrix2 clone() {
		return this.copy();
	}

	/**
	 * Access the internal two-dimensional array.
	 * 
	 * @return Pointer to the two-dimensional array of matrix elements.
	 */

	public Map<Integer, Double>[][] getArray() {
		return content;
	}

	/**
	 * Make a one-dimensional row packed copy of the internal array.
	 * 
	 * @return Matrix elements packed in a one-dimensional array by rows.
	 */

	public Map<Integer, Double>[] getRowPackedCopy() {
		Map<Integer, Double>[] vals = new Map[rowNb * colNb];
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

	public Map<Integer, Double> get(int i, int j) {
		return content[i][j];
	}

	/**
	 * Set a single element.
	 * 
	 * @param i Row index.
	 * @param j Column index.
	 * @param s A(i,j).
	 * @exception ArrayIndexOutOfBoundsException
	 */

	public void set(int i, int j, Map<Integer, Double> s) {
		content[i][j] = s;
	}

	public void set(int i, int j, int iterationNumbe, double value) {
		if (i < rowNb && j < colNb) {
			Map<Integer, Double> mapObsNb = content[i][j];
			mapObsNb.put(iterationNumbe, value);
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

	public void setMatrix(int i0, int i1, int j0, int j1, OldIterationMatrix2 X) {
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

	public void setMatrix(int[] r, int[] c, OldIterationMatrix2 X) {
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

	public void setMatrix(int[] r, int j0, int j1, OldIterationMatrix2 X) {
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
	 * C = A + B
	 * 
	 * @param toAdd another matrix
	 * @return A + B
	 */

	public OldIterationMatrix2 plus(OldIterationMatrix2 toAdd) {
		double thisSum = this.getSum();
		double toAddSum = toAdd.getSum();
		checkMatrixDimensions(toAdd);
		OldIterationMatrix2 result = new OldIterationMatrix2(rowNb, colNb);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				Map<Integer, Double> item = content[i][j];
				Map<Integer, Double> itemToAdd = toAdd.get(i, j);
				item = aux_plus(item, itemToAdd);
				result.set(i, j, item);
			}
		}
		double resultSum = result.getSum();
		if (Math.abs(resultSum - (thisSum + toAddSum)) > 0.1) {
			System.out.print("IterationMatrix.plus : pb");
		}
		return result;
	}

	public OldIterationMatrix2 minus(OldIterationMatrix2 toWithdraw) {
		double thisSum = this.getSum();
		double toAddSum = toWithdraw.getSum();
		checkMatrixDimensions(toWithdraw);
		OldIterationMatrix2 result = new OldIterationMatrix2(rowNb, colNb);
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				Map<Integer, Double> item = content[i][j];
				Map<Integer, Double> itemToWithdraw = toWithdraw.get(i, j);
				item = aux_minus(item, itemToWithdraw);
				result.set(i, j, item);
			}
		}
		double resultSum = result.getSum();
		if (Math.abs(resultSum - (thisSum + toAddSum)) > 0.1) {
			System.out.print("IterationMatrix.plus : pb");
		}
		return result;
	}

	public OldIterationMatrix2 auxApplyDivisor(double divisor) {
		OldIterationMatrix2 result = this.copy();
		for (int rowIdx = 0; rowIdx < this.getRowDimension(); rowIdx++) {
			for (int colIdx = 0; colIdx < this.getColumnDimension(); colIdx++) {
				Map<Integer, Double> toSet = aux_ApplyDivisor(content[rowIdx][colIdx], divisor);
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

	public OldIterationMatrix2 times(double s) {
		OldIterationMatrix2 X = new OldIterationMatrix2(rowNb, colNb);
		Map<Integer, Double>[][] C = X.getArray();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				Map<Integer, Double> item = C[i][j];
				C[i][j] = aux_times(item, s);// * content[i][j];
			}
		}
		return X;
	}

	/**
	 * Read a matrix from a stream. The format is the same the print method, so
	 * printed matrices can be read back in (provided they were printed using US
	 * Locale). Elements are separated by whitespace, all the elements for each row
	 * appear on a single line, the last row is followed by a blank line.
	 * 
	 * @param input the input stream.
	 */

	public static OldIterationMatrix2 read(BufferedReader input) throws java.io.IOException {
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
		Map<Integer, Double>[][] A = new Map[m][];
		v.copyInto(A); // copy the rows out of the vector
		return new OldIterationMatrix2(A);
	}

	/*
	 * ------------------------ Private Methods ------------------------
	 */

	/** Check if size(A) == size(B) **/

	private void checkMatrixDimensions(OldIterationMatrix2 B) {
		if (B.rowNb != rowNb || B.colNb != colNb) {
			throw new IllegalArgumentException("Matrix dimensions must agree.");
		}
	}

	public double getRowSum(int rowIdx) {
		double result = 0;
		if (rowIdx < this.rowNb) {
			for (int colIdx = 0; colIdx < this.colNb; colIdx++) {
				result += aux_getTotalValue(content[rowIdx][colIdx]);
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
		for (Map<Integer, Double> d : this.getRowPackedCopy()) {
			result += aux_getTotalValue(d);
		}
		return result;
	}

	public DoubleMatrix generateTotalMatrix() {
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			for (int columnIdx = 0; columnIdx < this.colNb; columnIdx++) {
				Map<Integer, Double> iterObsNb = content[rowIdx][columnIdx];
				result.set(rowIdx, columnIdx, aux_getTotalValue(iterObsNb));
			}
		}
		return result;
	}

	public DoubleMatrix generateMatrixAtIteration(int iterationNumber) {
		DoubleMatrix result = new DoubleMatrix(rowNb, colNb);
		for (int rowIdx = 0; rowIdx < this.rowNb; rowIdx++) {
			for (int columnIdx = 0; columnIdx < this.colNb; columnIdx++) {
				Map<Integer, Double> item = content[rowIdx][columnIdx];
				if (item.containsKey(iterationNumber)) {
					result.set(rowIdx, columnIdx, item.get(iterationNumber));
				}
			}
		}
		return result;
	}

	/*
	 * public BasicMatrix generateMatrixAtLastIteration() { BasicMatrix result = new
	 * BasicMatrix(rowNb, colNb); for (int rowIdx = 0; rowIdx < this.rowNb;
	 * rowIdx++) { for (int columnIdx = 0; columnIdx < this.colNb; columnIdx++) {
	 * Map<Integer, Double> item = content[rowIdx][columnIdx]; if
	 * (item.containsKey(maxIterationNumber)) { result.set(columnIdx, rowIdx,
	 * item.get(maxIterationNumber)); } } } return result; }
	 */
	public String format3d(List<Integer> matrixIterations) {
		// Matrix aMatrixNorm = normalize(aMatrix);
		StringBuffer result = new StringBuffer();
		result.append("[");
		String sep1 = "";
		for (int rowIdx = 0; rowIdx < rowNb; rowIdx++) {
			result.append(sep1);
			result.append("[");
			String cellSeparator = "";
			for (int colIdx = 0; colIdx < colNb; colIdx++) {
				Map<Integer, Double> value = content[rowIdx][colIdx];
				result.append(cellSeparator);
				// result.append(sIn).append("->").append(sMut).append(":");
				String svalue = aux_format3d(matrixIterations, value); // value.format3d(matrixIterations);
				/*
				 * while(svalue.length() <5) { svalue = svalue + " "; }
				 */
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

	public static OldIterationMatrix2 auxComputeSumIterationMatrix(List<OldIterationMatrix2> listMatrix) {
		if (listMatrix.size() == 0) {
			return null;
		}
		OldIterationMatrix2 firstMatrix = listMatrix.get(0);
		OldIterationMatrix2 sumMatrix = new OldIterationMatrix2(firstMatrix.getRowDimension(),
				firstMatrix.getColumnDimension());
		for (OldIterationMatrix2 nextMatrix : listMatrix) {
			sumMatrix = sumMatrix.plus(nextMatrix);
		}
		return sumMatrix;
	}

	public static OldIterationMatrix2 auxComputeAvgIterationMatrix(List<OldIterationMatrix2> listMatrix) {
		if (listMatrix.size() == 0) {
			return null;
		}
		OldIterationMatrix2 sumMatrix = auxComputeSumIterationMatrix(listMatrix);
		int divisor = listMatrix.size();
		OldIterationMatrix2 result = sumMatrix.auxApplyDivisor(divisor);
		return result;
	}
/*
	@Override
	public IAggregateable aggregate1(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, listObjects, agentAuthentication, logger);
	}
*/
	public static OldIterationMatrix2 aggregate2(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		OldIterationMatrix2 result = null;
		List<OldIterationMatrix2> listMatrix = new ArrayList<>();
		for (Object nextObj : listObjects) {
			if (nextObj instanceof OldIterationMatrix2) {
				OldIterationMatrix2 matrix = (OldIterationMatrix2) nextObj;
				listMatrix.add(matrix);
			}
		}
		if (listMatrix.size() == 0) {
			return result;
		}
		if ("avg".equals(operator)) {
			result = auxComputeAvgIterationMatrix(listMatrix);
		}
		if ("sum".equals(operator)) {
			result = auxComputeSumIterationMatrix(listMatrix);
		}
		/*
		 * if (result == null) { result = new IterationMatrix(matrix.getRowDimension(),
		 * matrix.getColumnDimension()); }
		 */
		return result;
	}

	private static String aux_format3d(List<Integer> matrixIterations, Map<Integer, Double> mapIterations) {
		StringBuffer result = new StringBuffer();
		result.append("{");
		// if(false && values.size() > 0) {
		// result.append(values.size()).append(":").append(matrixIterations.size()).append("#");
		// }
		String sep1 = "";
		List<Integer> iterations2 = matrixIterations;
		if (iterations2 == null) {
			iterations2 = aux_computeIterations(mapIterations);
		}
		for (Integer nextIteration : iterations2) {
			result.append(sep1);
			if (mapIterations.containsKey(nextIteration)) {
				double value = mapIterations.get(nextIteration);
				result.append(UtilDates.df3.format(value));
				// result.append("$");
			} else {
				// result.append("£");
			}
			sep1 = ",";
		}
		result.append("}");
		return result.toString();
	}

	private static List<Integer> aux_computeIterations(Map<Integer, Double> mapIterations) {
		List<Integer> result = new ArrayList<Integer>();
		for (Integer nextIt : mapIterations.keySet()) {
			result.add(nextIt);
		}
		Collections.sort(result);
		return result;
	}

	private static double aux_getTotalValue(Map<Integer, Double> mapIterations) {
		double total = 0.0;
		for (Integer nextIteration : mapIterations.keySet()) {
			total += mapIterations.get(nextIteration);
		}
		return total;
	}

	private static Map<Integer, Double> aux_times(Map<Integer, Double> mapIterations, double s) {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Integer nextIteration : mapIterations.keySet()) {
			result.put(nextIteration, s * mapIterations.get(nextIteration));
		}
		return result;
	}

	private static Map<Integer, Double> aux_ApplyDivisor(Map<Integer, Double> mapIterations, double s) {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Integer nextIteration : mapIterations.keySet()) {
			result.put(nextIteration, mapIterations.get(nextIteration) / s);
		}
		return result;
	}

	public static Map<Integer, Double> aux_plus(Map<Integer, Double> mapIterations, Map<Integer, Double> b) {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Integer nextIteration : mapIterations.keySet()) {
			result.put(nextIteration, mapIterations.get(nextIteration));
		}
		for (Integer nextIteration : b.keySet()) {
			if (!result.containsKey(nextIteration)) {
				result.put(nextIteration, 0.0);
			}
			double newValue = result.get(nextIteration) + b.get(nextIteration);
			result.put(nextIteration, newValue);
		}
		return result;
	}

	public static Map<Integer, Double> aux_minus(Map<Integer, Double> mapIterations, Map<Integer, Double> b) {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Integer nextIteration : b.keySet()) {
			double toWithdraw = b.get(nextIteration);
			if (mapIterations.containsKey(nextIteration)) {
				result.put(nextIteration, mapIterations.get(nextIteration) - toWithdraw);
			} else {
				result.put(nextIteration, -1 * toWithdraw);
			}
		}
		return result;
	}

	public IterationMatrix generateIterationMatrix() {
		IterationMatrix result = new IterationMatrix(rowNb, colNb);
		//Map<Integer, Double>[][] C = new Map[rowNb][colNb];
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				Map<Integer, Double> itValues = content[i][j];
				for (Integer nextIteration : itValues.keySet()) {
					Double nextValue = itValues.get(nextIteration);
					result.set(i, j, nextIteration, nextValue);
				}
			}
		}
		return result;
	}

	public List<Integer> generateIterations() {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < rowNb; i++) {
			for (int j = 0; j < colNb; j++) {
				Map<Integer, Double> itValues = content[i][j];
				for (Integer nextIteration : itValues.keySet()) {
					if (!result.contains(nextIteration)) {
						result.add(nextIteration);
					}
				}
			}
		}
		Collections.sort(result);
		return result;
	}


	public String zip() {
		StringBuffer result = new StringBuffer();
		Map<Integer, Double>[] flatAttay = getRowPackedCopy();
		String sep1 = "";
		for(Map<Integer, Double> nextMap : flatAttay) {
			result.append(sep1);
			String sep2 = "";
			for(Integer nextIteration : nextMap.keySet()) {
				Double value = nextMap.get(nextIteration);
				result.append(sep2).append(nextIteration).append(":").append(value);
				sep2=",";
			}
			sep1=";";
		}
		return result.toString();
	}

	public static OldIterationMatrix2 unzip(String zipContent) {
		String zipContent2 = zipContent;
		if(zipContent2.endsWith(";")) {
			zipContent2 = zipContent2 + " ";
		}
		String[] sflat = zipContent2.split(";");
		int size = (int) Math.round(Math.sqrt(sflat.length));
		OldIterationMatrix2 result = new OldIterationMatrix2(size, size);
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
}
