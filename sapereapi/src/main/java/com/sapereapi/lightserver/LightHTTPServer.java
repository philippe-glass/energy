package com.sapereapi.lightserver;

import java.io.BufferedReader;
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
import com.sapereapi.model.ServerConfig;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeConfig;

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
	protected static StringBuffer fooResponse = new StringBuffer();
	protected static byte[] responseBytes = null;
	private int fooTestCounter = 0;
	private ServerConfig serverConfig = null;

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
			//hostname = serverConfig.getNodeConfig().getHost();
			int port = serverConfig.getNodeConfig().getRestPort();
			InetSocketAddress address = new InetSocketAddress(port);
			server = HttpServer.create(address, 10);
			// server.createContex
			logger.info("starting http server(" + address + ")");
			server.createContext("/", this);
			// Init handlers
			List<NodeConfig> defaultNeighbours = serverConfig.getDefaultNeighbours();
			NodeConfig nodeConfig = serverConfig.getNodeConfig();
			handlersTable.put("energy", new EnergyHandler("/energy", nodeConfig, defaultNeighbours));
			handlersTable.put("sapere", new SapereHandler("/sapere", nodeConfig, defaultNeighbours));
			handlersTable.put("config", new ConfigHandler("/config", nodeConfig, defaultNeighbours));
			handlersTable.put("query", new QueryHandler("/query", nodeConfig, defaultNeighbours));
			handlersTable.put("service", new ServiceHandler("/service", nodeConfig, defaultNeighbours));
			// server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			if(serverConfig.isModeAuto()) {
				dataRetriever = new DataRetriever(serverConfig, logger);
				dataRetriever.start();
			}
			this.performAction();
		} catch (Exception e) {
			e.printStackTrace();
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

	@Override
	public void handle(HttpExchange he) throws IOException {
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
		} else {
			if(handlersTable.containsKey(handlerUri)) {
				requestedHandler = handlersTable.get(handlerUri);
			}
			if(requestedHandler == null) {
				// handler by default
				requestedHandler = handlersTable.get("energy");
			}

			// TODO : set a default service in each handler
			if ("/".equals(requestedServcice)) {
				requestedServcice = "/retrieveNodeContent";
			}
			if (isMthodOptions) {
				// DO NOTHING
				logger.info("option method received : do nothing");
			} else  {
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

	protected boolean sendHttpResponse(HttpExchange he, Object result) {
		response = new StringBuffer();
		Object jsonResult = null;
		boolean useHomeMadeJsonConverter = true;
		if (result == null && httpMethod.equals("OPTIONS")) {
			try {
				sendJsonResponse(he, response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (result != null) {
			if (useHomeMadeJsonConverter) {
				try {
					response = UtilJsonParser.toJsonStr(result, logger, 0);
					sendJsonResponse(he, response);
					if (requestedUri.getPath().contains("retrieveAllNodesContent")) {
						logger.info("for debug");
						//fooResponse = new StringBuffer(response.toString());
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
				/* */
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
				response.append(jsonResult.toString());
			}
		}
		// send response
		boolean bresult = false;
		try {
			bresult = true;
		} catch (Throwable t) {
			logger.error("response = " + response);
			logger.error(t);
		} finally {
			int len = response.length();
			response.delete(0, len);
			responseBytes = null;
			//System.gc();
		}
		try {
			if(!bresult) {
				he.getResponseBody().close();
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return bresult;
	}

	public void sendJsonResponse(HttpExchange he, StringBuffer response) throws IOException {
		responseBytes = response.toString().getBytes("UTF-8");
		he.getResponseHeaders().set("Content-Type", "application/json");
		he.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		he.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials-Header", "*");
		he.getResponseHeaders().add("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

		// For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS
		// handshake
		if (httpMethod.equals("OPTIONS")) {
			logger.info("he.getResponseHeaders() = " + he.getResponseHeaders());
			// he.getResponseHeaders()..ge(HttpServletResponse.SC_ACCEPTED);
			// he.getRequestHeaders().setStatus(HttpServletResponse.SC_ACCEPTED);
		}
		// he.sendResponseHeaders(200, response.length());
		he.sendResponseHeaders(200, responseBytes.length);
		OutputStream os = he.getResponseBody();
		os.write(responseBytes);
		os.flush();
		os.close();
	}

	public void sendTextResponse(HttpExchange he, StringBuffer response) throws IOException {
		byte[] responseBytes = response.toString().getBytes("UTF-8");
		he.getResponseHeaders().set("Content-Type", "text/html");
		he.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		he.getResponseHeaders().set("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
		he.getResponseHeaders().add("Access-Control-Allow-Credentials-Header", "*");
		he.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
		he.sendResponseHeaders(200, responseBytes.length);
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
	}
}
