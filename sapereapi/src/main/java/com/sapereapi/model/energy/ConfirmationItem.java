package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;

public class ConfirmationItem implements Serializable {
	private static final long serialVersionUID = 6L;
	private String issuer;
	private ProsumerRole issuerRole;
	private String receiver;
	private Boolean isOK;
	private Boolean isComplementary;
	private String comment;
	private Date date;
	private Date expiryDeadline;
	private int nbOfRenewals;
	private long timeShiftMS = 0;

	final static int EXPIRY_SECONDS = 10;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public ProsumerRole getIssuerRole() {
		return issuerRole;
	}

	public void setIssuerRole(ProsumerRole issuerRole) {
		this.issuerRole = issuerRole;
	}

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

	public void setIsComplementary(Boolean isComplementary) {
		this.isComplementary = isComplementary;
	}

	public int getNbOfRenewals() {
		return nbOfRenewals;
	}

	public void setNbOfRenewals(int nbOfRenewals) {
		this.nbOfRenewals = nbOfRenewals;
	}

	public ConfirmationItem(String issuer, ProsumerRole issuerRole, String receiver, Boolean _isComplementary,
			Boolean isOK, String acomment, int nbOfRenewals, Long _timeShiftMS) {
		super();
		this.issuer = issuer;
		this.issuerRole = issuerRole;
		this.receiver = receiver;
		this.isComplementary = _isComplementary;
		this.isOK = isOK;
		this.comment = acomment;
		this.nbOfRenewals = nbOfRenewals;
		this.timeShiftMS = _timeShiftMS;
		this.date = getCurrentDate();
		this.expiryDeadline = UtilDates.shiftDateSec(date, EXPIRY_SECONDS);
	}

	public Date getCurrentDate() {
		return UtilDates.getNewDateNoMilliSec(timeShiftMS);
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
		//result.append(receiver).append(":");
		result.append(isOK);
		String sTime = UtilDates.format_time.format(date);
		result.append("@").append(sTime);
		if (comment != null && comment.length() > 0) {
			result.append(" '").append(comment).append("'");
		}
		if (nbOfRenewals > 0) {
			result.append(" nbOfRenewals=").append(nbOfRenewals);
		}
		if (this.hasExpired()) {
			result.append(" (EXPIRED)");
		}
		return result.toString();
	}

	@Override
	public ConfirmationItem clone() {
		ConfirmationItem result = new ConfirmationItem(this.issuer, this.issuerRole, this.receiver,
				this.isComplementary, this.isOK, this.comment, this.nbOfRenewals, this.timeShiftMS);
		result.setDate(result.getDate());
		result.setExpiryDeadline(result.getExpiryDeadline());
		return result;
	}

	/**
	 * Used to renew a confirmation by updating its date.  The confirmation must not have expired.
	 * This operation is only permitted if the nbOfRenewals counter is strictly positive.
	 * @return
	 */
	public boolean renewConfirmation() {
		boolean updated = false;
		if (nbOfRenewals > 0) {
			Date current = getCurrentDate();
			if (current.after(date) && !current.after(expiryDeadline)) {
				int shiftSec = (int) Math.ceil(UtilDates.computeDurationSeconds(date, current));
				// is renewal permitted ?
				if (shiftSec <= nbOfRenewals) {
					// renew the confirmation
					nbOfRenewals -= shiftSec;
					date = current;
					updated = true;
				}
			}
		}
		return updated;
	}
}
