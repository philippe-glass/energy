package eu.sapere.middleware.node.notifier;

import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * Abstract base class for all Subscriber implementations.
 * 
 */
public abstract class AbstractSubscriber {

	/**
	 * Invoked to trigger the Notification.
	 * 
	 * @param event
	 *            the AbstractSapereEvent that triggered the notification
	 */
	public abstract void onNotification(AbstractSapereEvent event);

}
