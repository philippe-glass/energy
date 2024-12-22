package eu.sapere.middleware.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import eu.sapere.middleware.node.NodeManager;


public class MiddlewareLogger extends AbstractLogger {
	protected static Logger logger = null;
	protected static FileHandler fileHandler = null;
	protected static String logFilename = null;
	public final static SimpleDateFormat format_sessionid = new SimpleDateFormat("yyyyMMdd_HHmmss");

	static {
		// System.setProperty("java.util.logging.SimpleFormatter.format", format3);
		logger = Logger.getLogger(MiddlewareLogger.class.getName());
	}

	/** Instance unique pré-initialisée */
	private static MiddlewareLogger instance = new MiddlewareLogger();

	protected void initFileHandlers() {
		if(fileHandler==null) {
			String sDay = format_sessionid.format(new Date());
			String nodeName =  NodeManager.getNodeName();
			logFilename = "log" + FILE_SEP + "middleware" + FILE_SEP + "middleware."+ "." + sDay  + "." + nodeName + ".log";
			try {
				fileHandler = new FileHandler(logFilename);
				fileHandler.setFormatter(new MiddlewareLogFormatter());
				logger.addHandler(fileHandler);
				// logger.addHandler(new ConsoleLoggerBase());
				// the following statement is used to log any messages
				logger.info("--------------------------- First log nodeName = " + nodeName + "  -------------------------");
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private MiddlewareLogger() {
		super();
		initFileHandlers();
	}

	/**
	 * Access point for the singleton's single instance
	 * @return
	 */
	public static MiddlewareLogger getInstance() {
		return instance;
	}

	public void info(String message) {
		logger.info(message);
	}

	public void warning(String message) {
		logger.warning(message);
	}

	public void warning(Throwable t) {
		logger.warning("Exception : " + t + " "+ t.getMessage() + CR + t.getCause() + CR + stackTraceToString(t));
	}

	public void error(String message) {
		logger.severe(message);
	}

	public void error(Throwable t) {
		logger.severe("Exception : " + t + " "+ t.getMessage() + CR + t.getCause() + CR + stackTraceToString(t));
	}
}
