package com.sapereapi.model.learning.lstm;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.log.AbstractLogger;

public class LayerDefinition implements Serializable {
	private static final long serialVersionUID = 1L;
	private int layerIndex;
	private int layerIndex2;
	private String layerName;
	private String layerType;
	private List<Integer> layerDimension = new ArrayList<Integer>();
	private List<ParamType> paramTypes = new ArrayList<ParamType>();

	private static Integer maxLayerIndex = -1;
	private static Map<String, Integer> maxLayerIndexByType = new HashMap<String, Integer>();

	public LayerDefinition() {
		super();
	}

	public static void resetIndexes() {
		maxLayerIndex = -1;
		maxLayerIndexByType.clear();
	}

	public int getLayerIndex() {
		return layerIndex;
	}

	public void setLayerIndex(int layerIndex) {
		this.layerIndex = layerIndex;
	}

	public int getLayerIndex2() {
		return layerIndex2;
	}

	public void setLayerIndex2(int layerIndex2) {
		this.layerIndex2 = layerIndex2;
	}

	public String getLayerType() {
		return layerType;
	}

	public void setLayerType(String layerType) {
		this.layerType = layerType;
	}

	public List<Integer> getLayerDimension() {
		return layerDimension;
	}

	public void setLayerDimension(List<Integer> layerDimension) {
		this.layerDimension = layerDimension;
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public List<ParamType> getParamTypes() {
		return paramTypes;
	}

	public void setParamTypes(List<ParamType> paramTypes) {
		this.paramTypes = paramTypes;
	}

	public String getKey2() {
		return this.layerIndex + "_" + this.layerType + "_" + (1+this.layerIndex2);
	}

	public LayerDefinition(Class<?> layerClass) {
		this(layerClass, null, null, null);
	}

	public LayerDefinition(Class<?> layerClass, Integer dimension1) {
		this(layerClass, dimension1, null, null);
	}

	public LayerDefinition(Class<?> layerClass, Integer dimension1, Integer dimension2) {
		this(layerClass, dimension1, dimension2, null);
	}

	public LayerDefinition(Class<?> layerClass, Integer dimension1, Integer dimension2, Integer dimension3) {
		super();
		LayerDefinition.maxLayerIndex += 1;
		this.layerIndex = LayerDefinition.maxLayerIndex; // layerIndex
		// this.layerIndex2 = _layerIndex2;
		this.layerType = layerClass.getSimpleName().replace("Layer", "");
		if (LayerDefinition.maxLayerIndexByType.containsKey(layerType)) {
			Integer lastLayerIndex = maxLayerIndexByType.get(layerType);
			LayerDefinition.maxLayerIndexByType.put(layerType, 1 + lastLayerIndex);
		} else {
			LayerDefinition.maxLayerIndexByType.put(layerType, 0);
		}
		this.layerIndex2 = LayerDefinition.maxLayerIndexByType.get(layerType);

		this.layerDimension.clear();
		if (dimension1 != null) {
			layerDimension.add(dimension1);
		}
		if (dimension2 != null) {
			layerDimension.add(dimension2);
		}
		if (dimension3 != null) {
			layerDimension.add(dimension3);
		}
		this.layerName = this.layerType + "_" + this.layerIndex2;
	}

	public LayerDefinition clone() {
		LayerDefinition result = new LayerDefinition();
		result.setLayerIndex(layerIndex);
		result.setLayerIndex2(layerIndex2);
		result.setLayerName(layerName);
		result.setLayerType(layerType);
		result.setLayerDimension(SapereUtil.cloneListInteger(layerDimension));
		List<ParamType> cloneParamTypes = new ArrayList<ParamType>();
		if(paramTypes != null) {
			for(ParamType nextParamType : paramTypes) {
				cloneParamTypes.add(nextParamType);
			}
		}
		result.setParamTypes(cloneParamTypes);
		return result;
	}

	public String generateMaskFileName() {
		String layerName = layerType.toLowerCase();
		if (layerIndex2 > 0) {
			layerName = layerName + "_" + layerIndex2;
		}
		String fileMask = "^Layer_" + layerIndex + "_" + layerName + "_" + "(?<paramtype>(w|u|b))" + "_"
				+ "(?<dimensions>[X0-9]+)" + ".txt$";
		return fileMask;
	}

	public Map<ParamType, String> getMapFilepath(String modelDirectory) {
		Map<ParamType, String> result = new HashMap<ParamType, String>();
		File fModelDirectory = new File(modelDirectory);
		if (fModelDirectory.exists()) {
			String fileMask = generateMaskFileName();
			Pattern fileMaskPattern = Pattern.compile(fileMask);
			File[] files = fModelDirectory.listFiles();
			for (File nextFile : files) {
				String filename = nextFile.getName();
				Matcher matcher = fileMaskPattern.matcher(filename);
				if (matcher.find()) {
					String sParamType = matcher.group("paramtype");
					ParamType paramType = ParamType.valueOf(sParamType);
					result.put(paramType, modelDirectory + filename);
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("layerIndex = ").append(layerIndex)
			.append(", layerIndex2 = ").append(layerIndex2)
			.append(", layerName = ").append(layerName)
			.append(", layerType = ").append(layerType)
			.append(", layerDimension = ").append(layerDimension);
		return result.toString();
	}

	public String generateMatrixKey(ParamType paramType) {
		String key = this.layerName + "#" + paramType;
		return key;
	}

	public Map<ParamType, DoubleMatrix> extractMatrices(Map<String, DoubleMatrix> mapMatrices) {
		Map<ParamType, DoubleMatrix> result = new HashMap<ParamType, DoubleMatrix>();
		for(ParamType paramType : ParamType.values()) {
			String key = generateMatrixKey(paramType);
			if(mapMatrices.containsKey(key)) {
				result.put(paramType, mapMatrices.get(key));
			}
		}
		return result;
	}

	public String zip() {
		StringBuffer result = new StringBuffer();
		result.append(layerIndex)
			.append(",").append(layerIndex2)
			.append(",").append(layerType)
			.append(",").append(layerName)
			.append(",");
		String dimSep = "";
		for(int dimension : layerDimension) {
			result.append(dimSep).append(dimension);
			dimSep = "X";
		}
		result.append(",");
		String sParamTypes = " ";
		if(paramTypes != null && paramTypes.size() > 0) {
			sParamTypes="";
			String sep="";
			for(ParamType paramType : paramTypes) {
				result.append(sep).append(paramType);
				sep="-";
			}
		}
		result.append(",").append(sParamTypes);
		return result.toString();
	}

	public static LayerDefinition unzip(String content, AbstractLogger logger) {
		LayerDefinition result = new LayerDefinition();
		String[] contentArray = content.split(",");
		result.setLayerIndex(Integer.valueOf(contentArray[0]));
		result.setLayerIndex2(Integer.valueOf(contentArray[1]));
		result.setLayerType(contentArray[2]);
		result.setLayerName(contentArray[3]);
		String sDimension = contentArray[4];
		try {
			List<Integer> listDimensions = SapereUtil.extractDimensions(sDimension, "X");
			result.setLayerDimension(listDimensions);
		} catch (HandlingException e) {
			logger.error(e);
		}
		String sParamTypes = contentArray[5];
		if(!sParamTypes.trim().equals("")) {
			String[] paramTypes  = sParamTypes.split("-");
			List<ParamType> listParamTypes  = new ArrayList<ParamType>();
			for(String nextSParamType : paramTypes) {
				ParamType nextParamType =  ParamType.valueOf(nextSParamType);
				listParamTypes.add(nextParamType);
			}
			result.setParamTypes(listParamTypes);
		}
		return result;
	}
}
