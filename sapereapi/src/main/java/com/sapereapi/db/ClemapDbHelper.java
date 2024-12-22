package com.sapereapi.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class ClemapDbHelper {
	private static DBConnection dbConnection = null;
	private static ClemapDbHelper instance = null;
	private static SapereLogger logger = SapereLogger.getInstance();
	public final static String CR = System.getProperty("line.separator"); // Cariage return

	public static void init() {
		// initialise db connection
		instance = new ClemapDbHelper();
	}

	public ClemapDbHelper() {
		// initialise db connection
		dbConnection = DBConnectionFactory.getInstance(Sapere.DB_CLEMAP);// new DBConnection(dbConfig, logger);
		// JUST FOR TEST
		try {
			dbConnection.executeSelect("SELECT * FROM simulated_node_history LIMIT 0,1");
			logger.info("test ClemapDbHelper OK");
		} catch (HandlingException e) {
			logger.error(e);
		}
	}

	public static String addSingleQuotes(String str) {
		return SapereUtil.addSingleQuotes(str);
	}

	public static List<NodeTotal> loadNodeHistory(PredictionContext predictionContext, Long maxHistoryDurationMS) throws HandlingException {
		String sqlRequest = "";
		long timeShiftMS = predictionContext.getNodeContext().getTimeShiftMS();
		List<NodeTotal> result = new ArrayList<NodeTotal>();
		PredictionScope scope = predictionContext.getScope();
		String dateFilter = "1";
		Date current = predictionContext.getCurrentDate();
		if(maxHistoryDurationMS != null) {
			Date minDate = new Date(predictionContext.getCurrentDate().getTime() - maxHistoryDurationMS);
			dateFilter = "date >= " + addSingleQuotes(UtilDates.format_sql.format(minDate));
		}
		if (PredictionScope.CLUSTER.equals(scope)) {
			sqlRequest = "SELECT date"
					+ CR + ",SUM(produced) AS produced"
					+ CR + ",SUM(requested) AS requested"
					+ CR + ",SUM(consumed) AS consumed"
					+ CR + ",SUM(provided) AS provided"
					+ CR + ",SUM(available) AS available"
					+ CR + ",SUM(missing) AS missing"
					+ CR + " FROM simulated_node_history nsh"
					+ CR + "WHERE " + dateFilter + " GROUP BY date";
		} else {
			String node = predictionContext.getNodeLocation().getName();
			sqlRequest = "SELECT * FROM simulated_node_history WHERE node = '" + node + "' AND " + dateFilter + " ORDER BY date";
		}
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlRequest);
		for (Map<String, Object> row : rows) {
			NodeTotal nodeTotal = new NodeTotal();
			Date nextDate = SapereUtil.getDateValue(row, "date", logger);
			if(nextDate.before(current)) {
				nodeTotal.setDate(nextDate);
				nodeTotal.setTimeShiftMS(timeShiftMS);
				nodeTotal.setRequested(SapereUtil.getDoubleValue(row, "requested", logger));
				nodeTotal.setProduced(SapereUtil.getDoubleValue(row, "produced", logger));
				nodeTotal.setConsumed(SapereUtil.getDoubleValue(row, "consumed", logger));
				nodeTotal.setAvailable(SapereUtil.getDoubleValue(row, "available", logger));
				nodeTotal.setMissing(SapereUtil.getDoubleValue(row, "missing", logger));
				nodeTotal.setProvided(SapereUtil.getDoubleValue(row, "provided", logger));
				result.add(nodeTotal);
			}
		}
		return result;
	}

	public static long addNodeTotal(PredictionContext predictionContext, NodeTotal nextTotal) throws HandlingException {
		//String reqSeparator2 = CR + dbConnection.getReqSeparator2();
		StringBuffer request = new StringBuffer();
		request.append("INSERT INTO simulated_node_history (date, node, requested, produced, consumed, provided, available, missing) VALUES ");
		String itemSep = "";
		String node = predictionContext.getNodeLocation().getName();
		StringBuffer nextItem = new StringBuffer();
		nextItem.append("(")
			.append(addSingleQuotes(UtilDates.format_sql.format(nextTotal.getDate()))).append(",")
			.append(addSingleQuotes(node)).append(",")
			.append(nextTotal.getRequested()).append(",")
			.append(nextTotal.getProduced()).append(",")
			.append(nextTotal.getConsumed()).append(",")
			.append(nextTotal.getProvided()).append(",")
			.append(nextTotal.getAvailable()).append(",")
			.append(nextTotal.getMissing()).append(")");
		request.append(CR).append(itemSep).append(nextItem);
		itemSep = ",";
		long result = dbConnection.execUpdate(request.toString());
		return result;
	}
}
