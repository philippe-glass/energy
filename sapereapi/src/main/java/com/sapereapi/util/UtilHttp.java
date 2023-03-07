package com.sapereapi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.sapereapi.log.AbstractLogger;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.OptionItem;

public class UtilHttp {

	public static Map<String, Object> generateRequestParams(Object anObject, SimpleDateFormat format_datetime, AbstractLogger logger) {
		Map<String, Object> params = new HashMap<String, Object>();
		Class<?> targetObjectClass = anObject.getClass();
		for(Method method : targetObjectClass.getMethods())  {
			if(method.getParameterCount()==0) {
				String fieldName = null;
				if(method.getName().startsWith("get")) {
					fieldName = SapereUtil.firstCharToLower(method.getName().substring(3));
				} else if(method.getName().startsWith("is")) {
					//System.out.print("For debug : is method");
					fieldName = SapereUtil.firstCharToLower(method.getName().substring(2));
				}
				if(fieldName!=null) {
					try {
						Object value = method.invoke(anObject);
						if(value!=null) {
							if(value instanceof Date) {
								params.put(fieldName, format_datetime.format(value));
							} else if(value instanceof OptionItem) {
								//logger.info("generateRequestParams OptionItem = " + value);
								// Put the value itself as parameter
								params.put(fieldName, value);
							} else {
								// Put the string representation of the value
								params.put(fieldName, ""+value);
							}
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

	public static String formatRequest(String baseUrl, Map<String, Object> params, boolean useUtf8)  throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        //result.append(false && useUtf8 ? URLEncoder.encode(baseUrl, "UTF-8") : baseUrl);
        result.append(baseUrl);
        result.append("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
          result.append(useUtf8 ? URLEncoder.encode(entry.getKey(), "UTF-8") : entry.getKey());
          result.append("=");
          result.append(useUtf8 ? URLEncoder.encode(""+entry.getValue(), "UTF-8") : ""+entry.getValue());
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
			String sUrl = formatRequest(url, params, encodeUtf8);
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
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuffer response = new StringBuffer();
				while ((readLine = in.readLine()) != null) {
					response.append(readLine);
				}
				in.close();
				// print result
				if (debugLevel > 2) {
					logger.info("JSON String Result " + response.toString());
				}
				return response.toString();
				// GetAndPost.POSTRequest(response.toString());
			} else {
				logger.warning("GET NOT WORKED");
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	public static String sendPostRequest(String sUrl, Map<String, Object> params, AbstractLogger logger, int debugLevel) throws Exception {
		boolean encodeUtf8 = true;
		try {
			URL url = new URL(sUrl);
			StringBuilder bPostParams = new StringBuilder();
			bPostParams.append("{");
			String sep = "";
			for (String field : params.keySet()) {
				boolean isString = true;
				Object pvalue = params.get(field);
				String pvalueStr = ""+pvalue;
				if(pvalue instanceof OptionItem) {
					// Convert the object content in JSON format
					OptionItem oItem = (OptionItem) pvalue;
					pvalueStr = convertToJson(oItem);
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
			if (debugLevel > 2) {
				logger.info(bPostParams.toString());
			}
			byte[] postDataBytes = bPostParams.toString().getBytes("UTF-8");
			HttpURLConnection postConnection = (HttpURLConnection) url.openConnection();
			postConnection.setRequestMethod("POST");
			// conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			postConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			// postConnection.setRequestProperty( "Content-Type",
			// "application/x-www-form-urlencoded");
			if (encodeUtf8) {
				postConnection.setRequestProperty("charset", "utf-8");
				postConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			}
			postConnection.setDoOutput(true);
			if (encodeUtf8) {
				// postConnection.getOutputStream().write(postDataBytes);
				postConnection.getOutputStream().write(bPostParams.toString().getBytes("utf-8"));
			} else {
				postConnection.getOutputStream().write(bPostParams.toString().getBytes());
			}
			int responseCode = postConnection.getResponseCode();
			if (debugLevel > 0) {
				logger.info("POST Response Code :  " + responseCode);
				logger.info("POST Response Message : " + postConnection.getResponseMessage());
			}

			if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) { // success
				BufferedReader in = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));
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
			} else {
				if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
					System.out.print("For debug request = " + params);
				}
				logger.error("POST NOT WORKED " + params + ", bPostParams = " + bPostParams);
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
