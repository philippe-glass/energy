package eu.sapere.middleware.log;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class MiddlewareLogFormatter extends Formatter {
    // Create a DateFormat to format the logger timestamp.
	public static final String format =  "[%1$tF %1$tT] [%2$-7s] %3$s %n";
	public static final String format2 = "[%1$tF %1$tT] [%4$-7s] %5$s %n";
	public static final String format3 = "%4$s: %5$s %n";


    @Override
	public synchronized String format(LogRecord lr) {
		return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
				lr.getMessage());
	}

    public String getHead(Handler h) {
        return super.getHead(h);
    }

    public String getTail(Handler h) {
        return super.getTail(h);
    }
}