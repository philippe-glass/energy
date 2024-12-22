package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.UtilDates;

public class RegulationWarning implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private WarningType type;
	private List<String> destinationAgents;
	private Date date;
	private Long timeShiftMS;
	private Date waitingDeadline;
	private Date receptionDeadline;
	private ChangeRequest changeRequest;

	public WarningType getType() {
		return type;
	}

	public void setType(WarningType type) {
		this.type = type;
	}

	public List<String> getDestinationAgents() {
		return destinationAgents;
	}

	public void setDestinationAgents(List<String> _destinationAgents) {
		this.destinationAgents = _destinationAgents;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getTimeShiftMS() {
		return timeShiftMS;
	}

	public void setTimeShiftMS(Long timeShiftMS) {
		this.timeShiftMS = timeShiftMS;
	}

	public Date getWaitingDeadline() {
		return waitingDeadline;
	}

	public void setDeadlines(Date deadline) {
		this.waitingDeadline = deadline;
		this.receptionDeadline = deadline;
	}

	public void setWaitingDeadline(Date waitingDeadline) {
		this.waitingDeadline = waitingDeadline;
	}

	public Date getReceptionDeadline() {
		return receptionDeadline;
	}

	public void setReceptionDeadline(Date validityDeadline) {
		this.receptionDeadline = validityDeadline;
	}

	public ChangeRequest getChangeRequest() {
		return changeRequest;
	}

	public void setChangeRequest(ChangeRequest changeRequest) {
		this.changeRequest = changeRequest;
	}

	public boolean hasWaitingExpired() {
		Date current = getCurrentDate();
		return current.after(this.waitingDeadline);
	}

	public boolean hasReceptionExpired() {
		Date current = getCurrentDate();
		return current.after(this.receptionDeadline);
	}

	public RegulationWarning(WarningType type, Date date, long _timeShiftMS) {
		super();
		this.type = type;
		this.destinationAgents = new ArrayList<String>();
		this.date = date;
		this.timeShiftMS = _timeShiftMS;
		this.setDeadlines(UtilDates.shiftDateSec(getCurrentDate(), type.getValiditySeconds()));
	}

	public void addAgent(String agentName) {
		if (!destinationAgents.contains(agentName)) {
			this.destinationAgents.add(agentName);
		}
	}

	public boolean hasAgent(String agentName) {
		return this.destinationAgents.contains(agentName);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(type.getLabel()).append(" at ").append(UtilDates.format_time.format(date)).append(" agents ")
				.append(destinationAgents);
		return result.toString();
	}

	@Override
	public RegulationWarning clone() {
		RegulationWarning clone = new RegulationWarning(type, date, timeShiftMS);
		for (String destAgent : destinationAgents) {
			clone.addAgent(destAgent);
		}
		clone.setReceptionDeadline(receptionDeadline);
		clone.setWaitingDeadline(waitingDeadline);
		if (this.changeRequest != null) {
			clone.setChangeRequest(changeRequest.clone());
		}
		return clone;
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
	}
}
