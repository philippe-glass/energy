package eu.sapere.middleware.node.networking.transmission;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.Lsa;

/**
 * The tcp/ip server for the Sapere network interface. Multi-thread
 * implementation.
 * 
 */
public class Server extends Thread implements LsaReceived {

	private ServerSocket serverSocket;
	private Executor executor;
	private boolean listening = true;
	private LsaReceived listener;

	/**
	 * Instantiates the Server.
	 * 
	 * @param port
	 *            The tcp port.
	 * @param listener
	 *            The listener to be notified of the reception of a Lsa.
	 */
	public Server(LsaReceived listener) {
		this.listener = listener;
	}

	/**
	 * @param port
	 */
	public void start(int port) {
		try {
			serverSocket = new ServerSocket(port);
			executor = Executors.newCachedThreadPool();
			start();
		} catch (IOException e) {
			MiddlewareLogger.getInstance().error(e);
		}
	}
	@Override
	public void run() {
		while (listening) {
			try {
				Socket socket = serverSocket.accept();
				ServerThread serverThread = new ServerThread(socket, this);
				executor.execute(serverThread);
			} catch (IOException e) {
				MiddlewareLogger.getInstance().error(e);
			}
		}
	}

	@Override
	public void onLsaReceived(Lsa lsaReceived) {
		listener.onLsaReceived(lsaReceived);
	}

	/**
	 * Terminates this server.
	 */
	public void quit() {
		listening = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			MiddlewareLogger.getInstance().error(e);
		}

	}

}
