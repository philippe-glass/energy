package eu.sapere.middleware.node.notifier;

import java.util.Objects;
import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * A subscription to events happening to LSAs.
 * 
 */
public class Subscription {

	private AbstractSapereEvent eventType;
	private AbstractSubscriber subscriber;
	private String subscriberName;

	/**
	 * Instantiates a Subscription
	 * 
	 * @param anEventType
	 *            The type of event
	 * @param aFilter
	 *            The filter for events
	 * @param aSubscriber
	 *            The Subscriber Object
	 * @param subscriberName
	 *            The name of the Subscriber
	 */
	public Subscription(AbstractSapereEvent anEventType, AbstractSubscriber aSubscriber, String subscriberName) {
		this.eventType = anEventType;
		this.subscriber = aSubscriber;
		this.subscriberName = subscriberName;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(eventType, subscriber, subscriberName);
	}

	@Override
	public String toString() {
		return "<eventType=" + eventType + ", subscriber=" + subscriber + ", subscriberName=" + subscriberName + ">";
	}

	/**
	 * Gets the Subscriber for this Subscription
	 * 
	 * @return The Subscriber
	 */
	public AbstractSubscriber getSubscriber() {
		return subscriber;
	}

	/**
	 * Gets the name of the Subscriber for this Subscription
	 * 
	 * @return The name of the Subscriber
	 */
	public String getSubscriberName() {
		return subscriberName;
	}

	/**
	 * Gets the EventType for this Subscription
	 * 
	 * @return The EventType
	 */
	public Class<? extends AbstractSapereEvent> getEventType() {
		return eventType.getClass();
	}

}
