package com.sapereapi.lightserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sapereapi.model.DataRetriever;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eu.sapere.middleware.log.AbstractLogger;

public class LightHTTPServer extends Thread implements HttpHandler {
	private static AbstractLogger logger = null;
	private HttpServer server;
	private DataRetriever dataRetriever;
	private static int serverCounter = 0;
	private Object waiter = new Object();
	private boolean isRunning = false;
	protected String uri = null;
	protected URI requestedUri = null;
	protected String httpMethod = null;
	protected Map<String, Object> httpInput = new HashMap<>();
	protected Map<String, AbstractHandler> handlersTable = new HashMap<>();
	protected int debugLevel = 1;
	protected static StringBuffer response = new StringBuffer();
	protected static boolean isOutputStremClosed = false;
	protected static StringBuffer fooResponse = new StringBuffer();
	protected static byte[] responseBytes = null;
	private int fooTestCounter = 0;
	private ServerConfig serverConfig = null;
	public final static int RETURN_CODE_OK = 200;
	public final static int RETURN_CODE_SERVER_ERROR = 500;

	private boolean isRunning() {
		return isRunning;
	}

	private void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public LightHTTPServer(ServerConfig _serverConfig, AbstractLogger aLogger) {
		try {
			serverConfig = _serverConfig;
			logger = aLogger;
			//hostname = serverConfig.getNodeLocation().getHost();
			int port = serverConfig.getNodeLocation().getRestPort();
			InetSocketAddress address = new InetSocketAddress(port);
			server = HttpServer.create(address, 10);
			// server.createContex
			logger.info("starting http server(" + address + ")");
			server.createContext("/", this);
			// Init handlers
			handlersTable.put("energy", new EnergyHandler("/energy", serverConfig));
			handlersTable.put("sapere", new SapereHandler("/sapere", serverConfig));
			handlersTable.put("config", new ConfigHandler("/config", serverConfig));
			handlersTable.put("query", new QueryHandler("/query", serverConfig));
			handlersTable.put("service", new ServiceHandler("/service", serverConfig));
			// server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			if(serverConfig.isModeAuto()) {
				dataRetriever = new DataRetriever(serverConfig, logger);
				dataRetriever.start();
			}
			this.performAction();
		} catch (Exception e) {
			logger.warning(e.toString());
		}
	}

	private void performAction() {
		setRunning(true);
		Runtime.getRuntime().addShutdownHook(this);

		while (isRunning()) {
			try {
				synchronized (waiter) {
					waiter.wait(5000);
				}
			} catch (InterruptedException ie) {
			}
		}
		server.stop(3);
		// TODO unpublishAllEndpoints();
	}

	public void handleFileRequest(HttpExchange he, String fileName) throws IOException {
		// Headers headers = he.getResponseHeaders();
		he.getResponseHeaders().add("Content-Type", "image/png");
		// Retrieve requested file
		File file = new File(fileName);
		byte[] bytes = new byte[(int) file.length()];
		logger.info("handleFileRequest requeted file = " + file.getAbsolutePath());
		logger.info("handleFileRequest length:" + file.length());
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
		bufferedInputStream.read(bytes, 0, bytes.length);
		he.sendResponseHeaders(RETURN_CODE_OK, file.length());
		OutputStream outputStream = he.getResponseBody();
		outputStream.write(bytes, 0, bytes.length);
		outputStream.close();
	}// end of handleFileReques

	@Override
	public void handle(HttpExchange he) throws IOException {
		isOutputStremClosed = false;
		if(serverCounter % 10 ==0) {
			System.gc();
		}
		// parse request
		String query = null;
		try (InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
				BufferedReader br = new BufferedReader(isr)) {
			query = br.readLine();
		} catch (IOException e) {
			logger.error(e);
			throw e;
		}
		httpMethod = he.getRequestMethod();
		requestedUri = he.getRequestURI();
		if (!UtilHttp.METHOD_GET.equals(httpMethod)) {
			logger.info("handle : for debug");
		}
		String uriPath = requestedUri.getPath();
		if(uriPath.startsWith("/")) {
			uriPath = uriPath.substring(1);
		}
		String[] uriTab = uriPath.split("/");
		String handlerUri = uriTab[0];
		String requestedServcice = uriPath.replace(handlerUri, "");
		if (requestedServcice.startsWith("//")) {
			requestedServcice = requestedServcice.substring(1);
		}
		logger.info("receive request  " + requestedUri + " " + query);
		httpInput = new HashMap<String, Object>();
		if (query == null && UtilHttp.METHOD_GET.equals(httpMethod)) {
			query = requestedUri.getQuery();
			httpInput = UtilHttp.parseParams(query, httpInput);
		}
		boolean isMthodOptions = UtilHttp.METHOD_OPTIONS.equals(httpMethod);
		if (UtilHttp.METHOD_POST.equals(httpMethod)) {
			logger.info("POST handle : for debug");
			httpInput = UtilHttp.parseJsonParams(query, httpInput);
		}
		Object result = null;
		AbstractHandler requestedHandler = null;
		if(serverConfig.isUseFooHandler()) {
			response = FooHandler.generateResponse(fooTestCounter);
			fooTestCounter++;
			sendTextResponse(he, response);
		} else if (handlerUri.endsWith(".ico")){
			handleFileRequest(he, handlerUri);
		} else {
			if(handlersTable.containsKey(handlerUri)) {
				requestedHandler = handlersTable.get(handlerUri);
			}
			if(requestedHandler == null) {
				// handler by default
				logger.error("Handler not found for the URI " + handlerUri);
			}

			// TODO : set a default service in each handler
			if ("/".equals(requestedServcice)) {
				requestedServcice = "/retrieveNodeContent";
			}
			if (isMthodOptions) {
				// DO NOTHING
				logger.info("option method received : do nothing");
			} else if(requestedHandler != null)  {
				boolean useStressTest2 = true && serverConfig.isUseStressTest() &&
						(requestedServcice.contains("retrieveNodeContent") || requestedServcice.contains("nodeTotalHistory"));
				result = requestedHandler.callService(requestedServcice, httpMethod, httpInput, useStressTest2);
				if(useStressTest2) {
					for (int i=0; i<1000;i++) {
						result = requestedHandler.callService(requestedServcice, httpMethod, httpInput, useStressTest2);
						if(i % 100 == 0) {
							//System.gc();
						}
					}
				}
			}
			//sendFooJsonResponse = false;
			if(serverConfig.isSendFooJsonResponse() && fooResponse.toString().length()> 0) {
				response = fooResponse;
				sendTextResponse(he, response);
			} else {
				sendHttpResponse(he, result);
			}
		}
		serverCounter++;
	}

	public void run() {
		logger.info("HTTP Test Server ShutdownHook has been aktivated.");

		setRunning(false);
		synchronized (waiter) {
			waiter.notifyAll();
		}
	}

	protected StringBuffer generateJsonResponse(Object result, boolean useHomeMadeJsonConverter) {
		StringBuffer responseBuff = new StringBuffer();
		if (useHomeMadeJsonConverter) {
			try {
				responseBuff = UtilJsonParser.toJsonStr(result, logger, 0);
				if (requestedUri.getPath().contains("PredictionContext")) {
					logger.info("generateJsonResponse for debug");
					fooResponse =  UtilJsonParser.toJsonStr(result, logger, 0);
				}
				/*
				 * if(useStressTest) { for (int i = 0; i<100; i++) { response =
				 * UtilJsonParser.toJsonStr(result, logger,0); } }
				 */
				/*
				 * if(response.contains("\r\n")) { response = response.replace("\r\n",""); }
				 * response = response.replace("0.0,", "0,"); response =
				 * response.replace("0.0,", "0,");
				 */
			} catch (Throwable e) {
				logger.error(e);
			}
		} else {
			Object jsonResult = null;
			if (result instanceof List<?>) {
				JSONArray jsonArray = new JSONArray();
				List<Object> resultList = (List) result;
				for (Object nextItem : resultList) {
					jsonArray.put(new JSONObject(nextItem));
				}
				jsonResult = jsonArray;
			} else {
				jsonResult = new JSONObject(result);
				Object[] resultCorrection = UtilJsonParser.correctJsonDates(jsonResult, logger);
				Boolean isChanged = (Boolean) resultCorrection[0];
				if (isChanged) {
					jsonResult = (JSONObject) resultCorrection[1];
				}
			}
			logger.info("jsonResult = " + jsonResult);
			responseBuff.append(jsonResult.toString());
		}
		return responseBuff;
	}

	protected boolean sendHttpResponse(HttpExchange he, Object result) {
		response = new StringBuffer();
		boolean bresult = false;
		boolean useHomeMadeJsonConverter = true;
		if (result == null && httpMethod.equals("OPTIONS")) {
			try {
				sendJsonResponse(he, response, RETURN_CODE_OK);
			} catch (IOException e) {
				logger.error(e);
			}
		} else if(result == null) {
			try {
				sendNotFoundResponse(he);
			} catch (IOException e) {
				logger.error(e);
			}
		} else if(result instanceof HandlingException) {
			try {
				Exception ex = (Exception) result;
				String message = ""+ ex;
				response =  generateJsonResponse(message, useHomeMadeJsonConverter);
				sendJsonResponse(he, response, RETURN_CODE_SERVER_ERROR);
			} catch (IOException e) {
				logger.error(e);
			}
		} else {
			response = generateJsonResponse(result, useHomeMadeJsonConverter);
			try {
				sendJsonResponse(he, response, RETURN_CODE_OK);
				bresult = true;
			} catch (Throwable t) {
				logger.error("Exception returned by sendJsonResponse : response = " + response);
				logger.error(t);
			} finally {
				int len = response.length();
				response.delete(0, len);
				responseBytes = null;
				//System.gc();
			}
		}
		// send response
		try {
			if(!isOutputStremClosed && (he.getResponseBody() != null)) {
				he.getResponseBody().close();
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return bresult;
	}

	public void generateHeader(HttpExchange he, String contentType) {
		he.getResponseHeaders().set("Content-Type", contentType);
		he.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		he.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials-Header", "*");
		he.getResponseHeaders().add("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
		if (httpMethod.equals("OPTIONS")) {
			logger.info("he.getResponseHeaders() = " + he.getResponseHeaders());
			// he.getResponseHeaders()..ge(HttpServletResponse.SC_ACCEPTED);
			// he.getRequestHeaders().setStatus(HttpServletResponse.SC_ACCEPTED);
		}
	}

	public void sendNotFoundResponse(HttpExchange he) throws IOException {
		//responseBytes = response.toString().getBytes("UTF-8");
		generateHeader(he, "application/json");
		// For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS
		// handshake
		if (httpMethod.equals("OPTIONS")) {
			logger.info("he.getResponseHeaders() = " + he.getResponseHeaders());
			// he.getResponseHeaders()..ge(HttpServletResponse.SC_ACCEPTED);
			// he.getRequestHeaders().setStatus(HttpServletResponse.SC_ACCEPTED);
		}
		// he.sendResponseHeaders(200, response.length());
		he.sendResponseHeaders(404, -1);
		OutputStream os = he.getResponseBody();
		//os.write(responseBytes);
		os.flush();
		os.close();
		isOutputStremClosed = true;
	}

	public void sendJsonResponse(HttpExchange he, StringBuffer response, int returnCode) throws IOException {
		responseBytes = response.toString().getBytes("UTF-8");
		generateHeader(he, "application/json");
		// For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS
		// handshake
		he.sendResponseHeaders(returnCode, responseBytes.length);
		OutputStream os = he.getResponseBody();
		os.write(responseBytes);
		os.flush();
		os.close();
		isOutputStremClosed = true;
	}

	public void sendTextResponse(HttpExchange he, StringBuffer response) throws IOException {
		byte[] responseBytes = response.toString().getBytes("UTF-8");
		generateHeader(he, "text/html");
		he.sendResponseHeaders(RETURN_CODE_OK, responseBytes.length);
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		os.flush();
		os.close();
		isOutputStremClosed = true;
	}
}
