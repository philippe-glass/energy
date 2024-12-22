package eu.sapere.middleware.node.notifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import eu.sapere.middleware.log.MiddlewareLogger;
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
			if (elems.isSubscribed(event)) {
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

	public boolean _hasSubscriptions(String agent) {
		int nbSubscriptions = getNbSubscriptions(agent);
		return nbSubscriptions > 0;
	}

	public synchronized boolean hasSubscriptions(String agent) {
		for (Subscription subscription : subscriptions) {
			if (agent.equals(subscription.getSubscriberName())) {
				return true;
			}
		}
		return false;
	}

	public synchronized int getNbSubscriptions(String agent) {
		int nbSubscriptions = 0;
		for (Subscription subscription : subscriptions) {
			String subscriber = subscription.getSubscriberName();
			if (agent.equals(subscriber)) {
				nbSubscriptions++;
			}
		}
		return nbSubscriptions;
	}

	public synchronized List<String> getSubscriptionEventTypes(String agent) {
		List<String>  result = new ArrayList<String>();
		for (Subscription subscription : subscriptions) {
			String subscriber = subscription.getSubscriberName();
			if (agent.equals(subscriber)) {
				result.add(subscription.getEventType());
			}
		}
		return result;
	}

	public int getNbSubscriptions() {
		return subscriptions.size();
	}

	public synchronized Map<String, Integer> getNbSubscriptionsByAgent() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (Subscription ss : subscriptions) {
			String subscriber = ss.getSubscriberName();
			int nbSubscriptions = result.containsKey(subscriber) ? result.get(subscriber) : 0;
			nbSubscriptions++;
			result.put(subscriber, nbSubscriptions);
		}
		return result;
	}

	public synchronized void log() {
		Map<String, Integer> result = getNbSubscriptionsByAgent();
		for(String agent : result.keySet()) {
			List<String> agentSubscriptions = getSubscriptionEventTypes(agent);
			MiddlewareLogger.getInstance().info("log nextSubsciption of " + agent + " : " + String.join(",", agentSubscriptions));
		}
	}

	public synchronized void unsubscribe(String subscriberName, Class<?> eventClass) {
		String eventType = eventClass.getSimpleName();
		HashSet<Subscription> v = new HashSet<Subscription>();
		for (Subscription ss : subscriptions) {
			if (ss.getSubscriberName().equals(subscriberName) && ss.getEventType().equals(eventType)) {
				v.add(ss);
			}
		}
		for (Subscription ss : v) {
			unsubscribe(ss);
		}
	}

	public synchronized void unsubscribe(String subscriberName) {
		HashSet<Subscription> v = new HashSet<Subscription>();
		for (Subscription ss : subscriptions) {
			if (ss.getSubscriberName().equals(subscriberName)) {
				v.add(ss);
			}
		}
		for (Subscription ss : v) {
			unsubscribe(ss);
		}
	}

}
