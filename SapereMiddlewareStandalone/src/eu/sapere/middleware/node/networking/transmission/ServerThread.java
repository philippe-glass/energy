package eu.sapere.middleware.node.networking.transmission;

import java.io.ObjectInputStream;
import java.net.Socket;
import eu.sapere.middleware.lsa.Lsa;

/**
 * The thread that manages a connection received by the server.
 * 
 */
public class ServerThread extends Thread {

	private Socket socket = null;
	private LsaReceived listener;

	/**
	 * Instantiates a ServerThread.
	 * 
	 * @param socket
	 *            The socket
	 * @param listener
	 *            The listener to be notified with received Lsas.
	 */
	public ServerThread(Socket socket, LsaReceived listener) {
		super("Server");
		this.listener = listener;
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			ObjectInputStream ois = new ObjectInputStream(
					socket.getInputStream());
			Lsa receivedLsa = (Lsa) ois.readObject();
			listener.onLsaReceived(receivedLsa);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
