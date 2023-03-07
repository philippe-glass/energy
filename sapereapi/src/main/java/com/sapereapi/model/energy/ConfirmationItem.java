package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.util.UtilDates;

public class ConfirmationItem implements Serializable {
	private static final long serialVersionUID = 6L;
	private String receiver;
	private Boolean isOK;
	private Boolean isComplementary;
	private String comment;
	private Date date;
	private Date expiryDeadline;
	private long timeShiftMS = 0;

	final static int EXPIRY_SECONDS = 10;

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String _receiver) {
		this.receiver = _receiver;
	}

	public Boolean getIsOK() {
		return isOK;
	}

	public void setIsOK(Boolean isOK) {
		this.isOK = isOK;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setExpiryDeadline(Date expiryDeadline) {
		this.expiryDeadline = expiryDeadline;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isComplementary() {
		return isComplementary;
	}

	public ConfirmationItem(String receiver, Boolean _isComplementary, Boolean isOK, String acomment,
			Long _timeShiftMS) {
		super();
		this.receiver = receiver;
		this.isComplementary = _isComplementary;
		this.isOK = isOK;
		this.comment = acomment;
		this.timeShiftMS = _timeShiftMS;
		this.date = getCurrentDate();
		this.expiryDeadline = UtilDates.shiftDateSec(date, EXPIRY_SECONDS);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDate(timeShiftMS);
	}

	public Date getExpiryDeadline() {
		return expiryDeadline;
	}

	public boolean hasExpired() {
		Date current = getCurrentDate();
		return current.after(expiryDeadline);
	}

	public boolean hasExpired(int marginSec) {
		Date current = getCurrentDate();
		return current.after(UtilDates.shiftDateSec(expiryDeadline, marginSec));
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(isOK);
		if (comment != null && comment.length() > 0) {
			result.append(" ").append(comment);
		}
		if (this.hasExpired()) {
			result.append(" (").append(UtilDates.format_time.format(date)).append(" )");
		}
		return result.toString();
	}

	@Override
	public ConfirmationItem clone() {
		ConfirmationItem result = new ConfirmationItem(this.receiver, this.isComplementary, this.isOK, this.comment,
				this.timeShiftMS);
		result.setDate(result.getDate());
		result.setExpiryDeadline(result.getExpiryDeadline());
		return result;
	}
}
