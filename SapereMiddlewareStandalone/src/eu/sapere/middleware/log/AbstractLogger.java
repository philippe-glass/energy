package eu.sapere.middleware.log;

public abstract class AbstractLogger {
	public static final String FILE_SEP = System.getProperty("file.separator");
	public static final String CR = System.getProperty("line.separator");

	protected abstract void initFileHandlers();

	public abstract void info(String message);

	public abstract void warning(String message);

	public abstract void warning(Throwable t);

	public abstract void error(String message);

	public abstract void error(Throwable t);

	public static String stackTraceToString(Throwable e) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
