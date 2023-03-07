package eu.sapere.middleware.node.networking.transmission;

import java.util.HashMap;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeManager;

public final class PoolThread {
	private HashMap<String, ThreadSender> senders;
	private static PoolThread singleton = null;

	/**
	 * @return
	 */
	public static PoolThread getInstance() {
		if (singleton == null)
			singleton = new PoolThread();
		return singleton;
	}

	private PoolThread() {
		senders = new HashMap<String, ThreadSender>();
	}

	/**
	 * @param ip
	 * @param port
	 */
	public synchronized void addNewThread(String ip, int port) {
		String key = ip+":"+port;
		if (senders.containsKey(key))
			return;

		ThreadSender sender = new ThreadSender(this, ip, port);
		// create a new thread sender
		senders.put(key, sender);
		sender.start();
	}

	/**
	 * @param lsa
	 * @param ipDest
	 */
	public synchronized void pushLsa(Lsa lsa, String ipDest) {
		String threadKey = ipDest;
		if(ipDest.contains(":")) {
			String[] ipPort = ipDest.split(":");
			int port = Integer.valueOf(ipPort[1]);
			addNewThread(ipPort[0], port);
		} else {
			addNewThread(ipDest, NodeManager.DEFAULT_PORT);
			threadKey = ipDest+":"+NodeManager.DEFAULT_PORT;
		}
		// push an lsa into the queue of the associated thread sender
		senders.get(threadKey).pushLsa(lsa);
	}

	/**
	 * @param s
	 */
	public synchronized void onThreadExit(ThreadSender s) {
		this.senders.remove(s.key);
	}

	/**
	 * @param ipDest
	 * @param portDest 
	 */
	public synchronized void removeSenderThread(String ipDest, int portDest) {
		ThreadSender sender = this.senders.get(ipDest+":"+portDest);
		if (sender == null)
			return;
		// stop the thread
		sender.forceThreadStop();
	}
}
