package com.sapereapi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sapereapi.model.LaunchConfig;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.TimestampedValue;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeConfig;

public class UtilHttp {
	public final static String METHOD_GET = "GET";
	public final static String METHOD_DELETE = "DELETE";
	public final static String METHOD_PUT = "PUT";
	public final static String METHOD_POST = "POST";
	public final static String METHOD_TRACE = "TRACE";
	public final static String METHOD_OPTIONS = "OPTIONS";

	public static Object formatParamGET(Object value, SimpleDateFormat format_datetime) {
		Object postedValue = "" + value; // By default : tut the string representation of the value
		if(value instanceof Date) {
			postedValue = format_datetime.format(value);
		} else if(value instanceof OptionItem || value instanceof NodeConfig || value instanceof HashMap || value instanceof AgentType ) {
			postedValue = value;
		} else if (value instanceof List<?> ) {
			//JSONArray jsonList = new JSONArray(value);
			List listValue = (List) value;
			postedValue =  String.join(",", listValue);
			//postedValue = value;
		} else if (value instanceof Object[] ) {
			//JSONArray jsonList = new JSONArray(value);
			Object[] arrayValue = (Object[] ) value;
			List listValue = Arrays.asList(arrayValue);
			postedValue =  String.join(",",  listValue);
			//postedValue = value;
		}
		return postedValue;
	}

	public static Object formatParamPOST(Object value, SimpleDateFormat format_datetime) {
		Object postedValue = "" + value; // By default : tut the string representation of the value
		if(value instanceof Date) {
			postedValue = format_datetime.format(value);
		} else if(value instanceof OptionItem || value instanceof NodeConfig || value instanceof HashMap || value instanceof AgentType ) {
			postedValue = value;
		} else if (value instanceof List || value instanceof Object[] || value instanceof Boolean || value instanceof Double
				|| value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Short) {
			//JSONArray jsonList = new JSONArray(value);
			postedValue = value;
		}
		return postedValue;
	}

	public static Map<String, Object> parseParams(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {
		if (query != null) {
			    String[] parts =  query.split("&");
			    for (String part : parts) {
			        String[] pair = part.split("=", 2);
			        if (pair.length != 2)
			            continue;
			        String name = URLDecoder.decode(pair[0], "UTF-8");
			        String value = URLDecoder.decode(pair[1], "UTF-8");
			        parameters.put(name, value);
			    }
		}
		return parameters;
	}

	public static Map<String, Object> parseJsonParams(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {
		if (query != null) {
			JSONObject jsonObject = new JSONObject(query);
			for(Object key : jsonObject.names()) {
				String skey = key.toString();
				Object jsonValue = jsonObject.get(skey);
				if(jsonValue instanceof String) {
					parameters.put(skey, jsonValue);
				} else {
					if(jsonValue instanceof JSONObject) {
						JSONObject jsonValue2 = (JSONObject) jsonValue;
					}
					parameters.put(skey, jsonValue);
				}
			}
		}
		return parameters;
	}

	public static Object fillObject(Object targetObject, String httpMethod, Map<String, Object> params, AbstractLogger logger) {
		Class<?> targetObjectClass = targetObject.getClass();
		//boolean useJson = METHOD_POST.equals(httpMethod);
		for(Method method : targetObjectClass.getMethods()) {
			if(1==method.getParameterCount() && method.getName().startsWith("set")) {
				Parameter param = method.getParameters()[0];
				String fieldName =  SapereUtil.firstCharToLower(method.getName().substring(3));
				if(params.containsKey(fieldName))  {
					Object value = params.get(fieldName);
					Class<?> paramType = param.getType();
					Object valueToSet = null;
					try {
						if(paramType.equals(String.class)) {
							valueToSet = ""+value;
						} else if(paramType.equals(Double.class) || paramType.equals(double.class)) {
							valueToSet = Double.valueOf(""+value);
						} else if(paramType.equals(Float.class) || paramType.equals(float.class)) {
							valueToSet = Float.valueOf(""+value);
						} else if(paramType.equals(Integer.class) || paramType.equals(int.class)) {
							valueToSet = Integer.valueOf(""+value);
						} else if(paramType.equals(Long.class) || paramType.equals(long.class)) {
							valueToSet = Long.valueOf(""+value);
						} else if(paramType.equals(Boolean.class) || paramType.equals(boolean.class))  {
							valueToSet = Boolean.valueOf(""+value);
						} else if(paramType.equals(Date.class))  {
							valueToSet = UtilJsonParser.parseJsonDate(""+value);
						} else if(List.class.equals(paramType)) {
							List<Object> listToSet = new ArrayList<Object>();
							//Class<?> test = paramType.arrayType();
							JSONArray jsonArray = (JSONArray) value;
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								listToSet.add(valueItem);
							}
							valueToSet = listToSet;
						} else if(String[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							String[] toSet = new String[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = ""+valueItem;
							}
							valueToSet = toSet;
						} else if(String[].class.equals(paramType) && !(value instanceof JSONArray)) {
							String sValue = ""+value;
							if(sValue.length() > 0) {
								String[] toSet = sValue.split(",");
								if(toSet.length > 0) {
									valueToSet = toSet;
								}
							}
						//} else if(paramType.equals(Map.class)) {
							/*
						} else if(paramType.isEnum()) {
							String svalue =  "" + value;
							Enum foo = Enum.valueOf(paramType.getClass(), svalue);
							Enum<?> enumClass = Enum.valueOf(paramType, svalue); */
						} else if(Integer[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Integer[] toSet = new Integer[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Integer.valueOf(""+valueItem);
							}
						} else if(Double[].class.equals(paramType) && value instanceof JSONArray) {
							logger.error("fillObject : Double[] not implemented ");
						} else if(Float[].class.equals(paramType) && value instanceof JSONArray) {
							logger.error("fillObject : Float[] not implemented ");
						} else if(Long[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Long[] toSet = new Long[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Long.valueOf(""+valueItem);;
							}
							valueToSet = toSet;
						} else if(paramType.equals(EnvironmentalImpact.class)) {
							String svalue =  "" + value;
							valueToSet = EnvironmentalImpact.getByName(svalue);
						} else if(paramType.equals(DeviceCategory.class)) {
							String svalue =  "" + value;
							valueToSet = DeviceCategory.getByName(svalue);
						} else if(paramType.equals(AgentType.class)) {
							String svalue = "" + value;
							valueToSet = AgentType.getByName(svalue);
						} else if(paramType.equals(OptionItem.class)) {
							JSONObject jsonObject = (JSONObject) value;
							valueToSet = UtilJsonParser.parseOptionItem(jsonObject, logger);
						} else if(paramType.equals(TimestampedValue.class)) {
							JSONObject jsonObject = (JSONObject) value;
							valueToSet = UtilJsonParser.parseTimeStampedValue(jsonObject, logger);
						} else {
							logger.error("fillObject : paramtype not handled " + paramType);
						}
					} catch (Throwable e) {
						logger.error(e);
					}
					if(valueToSet!=null)  {
						try {
							method.invoke(targetObject, valueToSet);
						} catch (Throwable e) {
							logger.error(e);
						}

					}
				}
			}
		}
		return targetObject;
	}


	public static Map<String, Object> generateRequestParams(Object anObject, SimpleDateFormat format_datetime, AbstractLogger logger, String httpMethod) {
		Map<String, Object> params = new HashMap<String, Object>();
		Class<?> targetObjectClass = anObject.getClass();
		for(Method method : targetObjectClass.getMethods())  {
			if(method.getParameterCount()==0) {
				String fieldName = null;
				if(UtilJsonParser.excludeMethodList.contains(method.getName())) {
					// Do nothing
					logger.info("generateRequestParams : do nothing");
				} else if(method.getName().startsWith("get")) {
					fieldName = SapereUtil.firstCharToLower(method.getName().substring(3));
				} else if(method.getName().startsWith("is")) {
					//System.out.print("For debug : is method");
					fieldName = SapereUtil.firstCharToLower(method.getName().substring(2));
				}
				if(fieldName!=null) {
					try {
						Object value = method.invoke(anObject);
						if(value!=null) {
							Object postedValue = null;
							if(METHOD_POST.equals(httpMethod)) {
								postedValue = formatParamPOST(value, format_datetime);
							} else {
								postedValue = formatParamGET(value, format_datetime);
							}
							params.put(fieldName, postedValue);
						}
					} catch (Throwable e) {
						logger.error(e);
						if(anObject instanceof AgentForm) {
							AgentForm agentForm = (AgentForm) anObject;
							boolean isRunning = agentForm.isRunning();
							logger.info("generateRequestParams isRunning = " + isRunning);
						}
					}
				}
			}
		}
		return params;
	}

	public static String formatGETRequest(String baseUrl, Map<String, Object> params, boolean useUtf8)  throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        //result.append(false && useUtf8 ? URLEncoder.encode(baseUrl, "UTF-8") : baseUrl);
        result.append(baseUrl);
        result.append("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
          result.append(useUtf8 ? URLEncoder.encode(entry.getKey(), "UTF-8") : entry.getKey());
          result.append("=");
          String pvalueStr = ""+entry.getValue();
          result.append(useUtf8 ? URLEncoder.encode(pvalueStr, "UTF-8") : pvalueStr);
          result.append("&");
        }
        String resultString = result.toString();
        return resultString.length() > 0
          ? resultString.substring(0, resultString.length() - 1)
          : resultString;
    }

	public static String sendGetRequest(String url, AbstractLogger logger, int debugLevel) {
		 Map<String, Object> params = new HashMap<String, Object>();
		 return sendGetRequest(url, params, logger, debugLevel);
	}

	public static String sendGetRequest(String url, Map<String, Object> params, AbstractLogger logger, int debugLevel) {
		try {
			String readLine = null;
			if(params.size()>0) {
				//logger.info("sendGetRequest : params = " + params);
			}
			boolean encodeUtf8 = true;
			String sUrl = formatGETRequest(url, params, encodeUtf8);
			URL uri = new URL(sUrl);
			HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
			if(encodeUtf8) {
				connection.setRequestProperty("Accept-Charset", "UTF-8");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", String.valueOf(sUrl.length()));

			}
			connection.setRequestMethod("GET");
			//connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			// conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample
			// here
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
					StringBuffer response = new StringBuffer();
					while ((readLine = in.readLine()) != null) {
						response.append(readLine);
					}
					// print result
					if (debugLevel > 2) {
						logger.info("JSON String Result " + response.toString());
					}
					return response.toString();
				} catch (Throwable e) {
					logger.error(e);
				}
				// GetAndPost.POSTRequest(response.toString());
			} else {
				logger.warning("GET NOT WORKED");
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	private static StringBuilder buildParams(Map<String, Object> params) {
		StringBuilder bPostParams = new StringBuilder();
		bPostParams.append("{");
		String sep = "";
		for (String field : params.keySet()) {
			Object pvalue = params.get(field);
			boolean isString = pvalue instanceof String; // true;
			String pvalueStr = ""+pvalue;
			if(pvalue instanceof LaunchConfig || pvalue instanceof NodeConfig || pvalue instanceof OptionItem
					|| pvalue instanceof AgentType) {
				pvalueStr = convertToJson(pvalue);
				isString=false;
			} else if(pvalue instanceof Map<?, ?>) {
				// Clone the map object
				Map<Object, Object> test = new HashMap<Object, Object>();
				Map<Object, Object> map = (Map<Object, Object>) pvalue;
				for(Object key : map.keySet()) {
					Object value = map.get(key);
					test.put(key, value);
				}
				JSONObject jsonPvalue = new JSONObject(test);
				pvalueStr = jsonPvalue.toString();
				isString=false;
			}
			/*
			 * if(pvalue!=null && pvalue.indexOf("TV ") >=0 ) { logger.info("For debug " +
			 * pvalue); }
			 */
			if (isString && pvalueStr != null && pvalueStr.indexOf("\"") >= 0) {
				String replace = "\\" + "\"";
				pvalueStr = pvalueStr.replace("\"", replace);
			}
			bPostParams.append(sep).append(SapereUtil.addDoubleQuote(field)).append(":")
				.append(isString ? SapereUtil.addDoubleQuote(pvalueStr) : pvalueStr);
			sep = ",";
		}
		bPostParams.append("}");
		return bPostParams;
	}

	public static String sendPostRequest(String sUrl, Object toPost, AbstractLogger logger, int debugLevel) throws Exception {
		StringBuffer jsonContent = UtilJsonParser.toJsonStr(toPost, logger, 0);
		return aux_sendPostRequest(sUrl, jsonContent.toString(), logger, debugLevel);
	}

	@Deprecated
	public static String sendPostRequest(String sUrl, Map<String, Object> params, AbstractLogger logger, int debugLevel) throws Exception {
		try {
			StringBuilder bPostParams = buildParams(params);
			String sParams = bPostParams.toString();
			if (debugLevel > 2) {
				logger.info(bPostParams.toString());
			}
			return aux_sendPostRequest(sUrl, sParams, logger, debugLevel);
		} catch (Exception e1) {
			e1.printStackTrace();
			logger.error(e1);
			throw e1;
		}
	}

	public static String aux_sendPostRequest(String sUrl, String sparams, AbstractLogger logger, int debugLevel) throws Exception {
		boolean encodeUtf8 = true;
		try {
			logger.info("aux_sendPostRequest " + sUrl + " params: " + sparams);
			URL url = new URL(sUrl);
			byte[] postDataBytes = encodeUtf8 ? sparams.getBytes("UTF-8") : sparams.getBytes();
			HttpURLConnection postConnection = (HttpURLConnection) url.openConnection();
			postConnection.setRequestMethod("POST");
			// conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			postConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			// postConnection.setRequestProperty( "Content-Type",
			// "application/x-www-form-urlencoded");
			if (encodeUtf8) {
				postConnection.setRequestProperty("charset", "utf-8");
			}
			postConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			postConnection.setDoOutput(true);
			postConnection.getOutputStream().write(postDataBytes);
			int responseCode = postConnection.getResponseCode();
			if (debugLevel > 0) {
				logger.info("POST Response Code :  " + responseCode);
				logger.info("POST Response Message : " + postConnection.getResponseMessage());
			}

			if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) { // success
				try(BufferedReader in = new BufferedReader(new InputStreamReader(postConnection.getInputStream()))) {
					String inputLine;
					StringBuffer response = new StringBuffer();
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					// print result
					if (debugLevel > 2) {
						logger.info(response.toString());
					}
					return response.toString();
				} catch(Throwable e) {
					logger.error(e);
					throw e;
				}
			} else {
				if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
					System.out.print("For debug request = " + sparams);
				}
				logger.error("POST NOT WORKED " + sparams );
				throw new Exception("Post request failed : code " + responseCode);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			logger.error(e1);
			throw e1;
		}
	}

	public static String convertToJson(Object obj) {
		JSONObject jsonObject = new JSONObject(obj);
		String myJson = jsonObject.toString();
		return myJson;
	}
}
