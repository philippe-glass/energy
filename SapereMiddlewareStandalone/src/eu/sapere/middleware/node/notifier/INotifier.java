package eu.sapere.middleware.node.notifier;

import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * Provides an interface for the Notifier of the local Node.
 * 
 */
public interface INotifier {

	/**
	 * Publishes an event
	 * 
	 * @param event
	 *            The event to publish
	 */
	public void publish(AbstractSapereEvent event);

	/**
	 * Adds a subscription to the Notifier
	 * 
	 * @param s
	 *            The subscription to add
	 * @throws InvalidEventTypeException
	 */
	public void subscribe(Subscription s);

	/**
	 * Removes a subscription from the Notifier
	 * 
	 * @param s
	 *            The subscription to remove
	 * @throws InvalidEventTypeException
	 */
	public void unsubscribe(Subscription s);

	/**
	 * Removes all subscription by a subscriber from the Notifier
	 * 
	 * @param subscriberName
	 *            The subscriber
	 */
	public void unsubscribe(String subscriberName);

}
