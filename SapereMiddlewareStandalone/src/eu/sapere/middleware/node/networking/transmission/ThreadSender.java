package eu.sapere.middleware.node.networking.transmission;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;

public class ThreadSender extends Thread {
	// queue of lsa waiting for being sent
	private Queue<Lsa> queue;
	// ip and port of the destination node
	public String ipDest;
	private int port;
	private PoolThread pool;
	private Boolean on;
	public String key;

	/**
	 * @param pool
	 * @param ipDest
	 * @param port
	 */
	public ThreadSender(PoolThread pool, String ipDest, int port) {
		this.ipDest = ipDest;
		this.port = port;
		this.pool = pool;
		queue = new LinkedList<Lsa>();
		on = true;
		this.key = this.ipDest + ":" + this.port;
	}

	public void run() {
		// get the next element
		while (on) {
			try {
				Lsa nextLsa;
				synchronized (this) {
					nextLsa = queue.poll();
					if (nextLsa == null) {
						this.wait();
						continue;
					}
				}
				// send this lsa
				on = sendLsa(nextLsa);
			} catch (InterruptedException ex) {
				on = false;
			}
		}
		// send a message to the pool
		pool.onThreadExit(this);
		this.interrupt();

	}

	public synchronized void pushLsa(Lsa lsa) {
		queue.add(lsa);
		this.notify();
	}

	private Boolean sendLsa(Lsa lsa) {
		try {
			Socket socket = new Socket(ipDest, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(lsa);
			socket.close();
			return true;
		} catch (Exception ex) {
			lsa.addSyntheticProperty(SyntheticPropertyName.DIFFUSE, "2"); // error
			return false;
		} 
	}

	public synchronized void forceThreadStop() {
		on = false;
		this.notify();
	}
}
