package eu.sapere.middleware.node.notifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.sapere.middleware.node.notifier.event.AbstractSapereEvent;

/**
 * Provides the implementation for the Notifier
 */
public class Notifier implements INotifier {

	protected HashSet<Subscription> subscriptions = null;

	/**
	 * Creates an instance for the Notifier
	 */
	public Notifier() {
		subscriptions = new HashSet<Subscription>();
	}

	public synchronized void publish(AbstractSapereEvent event) {
		for (Subscription elems : subscriptions) {
			if (elems.getEventType().isAssignableFrom(event.getClass())) {
				try {
					if (event.getRequiringAgent().equals(elems.getSubscriberName())) {
						elems.getSubscriber().onNotification(event);
					}
				} catch (Exception e) {
					System.out.println(event.getRequiringAgent()+" publishing "+e.toString());
				}
			}
		}
	}

	public synchronized void subscribe(Subscription s) {
		subscriptions.add(s);
	}

	public synchronized void unsubscribe(Subscription s) {
		subscriptions.remove(s);
	}

	public int getNbSubscriptions() {
		return subscriptions.size();
	}

	public Map<String, Integer> getNbSubscriptionsByAgent() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (Subscription ss : subscriptions) {
			String subscriber = ss.getSubscriberName();
			int nbSubscriptions = result.containsKey(subscriber) ? result.get(subscriber) : 0;
			nbSubscriptions++;
			result.put(subscriber, nbSubscriptions);
		}
		return result;
	}

	public synchronized void unsubscribe(String subscriberName) {
		HashSet<Subscription> v = new HashSet<Subscription>();
		for (Subscription ss : subscriptions)
			if (ss.getSubscriberName().equals(subscriberName))
				v.add(ss);
		for (Subscription ss : v)
			unsubscribe(ss);
	}

}
