package eu.sapere.middleware.node.lsaspace;

import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import eu.sapere.middleware.log.MiddlewareLogger;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.Subscription;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.AggregationEvent;
import eu.sapere.middleware.node.notifier.event.SpreadingEvent;
import eu.sapere.middleware.node.notifier.event.RewardEvent;

public class OperationManager {

	private Queue<Operation> operationsQueue = null; // FIFO
	private Space space = null;
	private Notifier notifier = null;

	/**
	 * Creates an instance of the Operation Manager
	 * 
	 * @param space     the local LSA Space
	 * @param notifier  the Notifier
	 * @param opTime    the operation time
	 * @param sleepTime
	 */
	public OperationManager(Space space, Notifier notifier) {
		// This FIFO queue is synchronized AND NON BLOCKING
		this.operationsQueue = new ConcurrentLinkedQueue<Operation>();
		this.space = space;
		this.notifier = notifier;
	}

	/**
	 * Launches the ordered execution of operations, one by one, until the operation
	 * time is not expired.
	 */
	public void exec() {
		long startTime = new Date().getTime();
		long currentTime = startTime;
		boolean ended = false;
		do {
			if (!operationsQueue.isEmpty()) {
				execNextOp();
			} else {
				ended = true;
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					MiddlewareLogger.getInstance().error(e);
				}
			}
			currentTime = new Date().getTime();
		} while ((currentTime - startTime < NodeManager.SLEEPTIME) && !ended);
		MiddlewareLogger.getInstance().info("OperationManager.exec spentTime (MS) = " + (currentTime - startTime));
	}

	/**
	 * Pulls out the next operation to be executed by the queue, and launches its
	 * execution.
	 * 
	 * @return true if an operation has been executed, false otherwise
	 */
	private void execNextOp() {
		Operation nextOp = null;
		Iterator<Operation> iterator = operationsQueue.iterator();
		if (iterator.hasNext()) {
			nextOp = iterator.next();
			operationsQueue.poll();
			execOp(nextOp);
		}
	}

	/**
	 * Executes an operation on the local LSA space
	 * 
	 * @param nextOp the operation to be executed
	 */
	private void execOp(Operation nextOp) {
		boolean debug = false;
		if(!debug) {
			System.out.println("execOp:" + nextOp.getOpType().toString());
		}
		if (nextOp.getOpType() == OperationType.INJECT) {
			//if (!nextOp.getLsa().getAgentName().contains("*"))
			if (nextOp.getLsa().isLocal())
				space.inject(nextOp.getLsa());
			else {
				space.inject(nextOp.getLsa());
			}
		}
		if (nextOp.getOpType() == OperationType.REMOVE) {
			space.remove(nextOp.getLsa().getAgentName());
		}
		if (nextOp.getOpType() == OperationType.UPDATE) {
			space.update(nextOp.getLsa(), nextOp.getRequestingAgent(), true, true);
		}
		if (nextOp.getOpType() == OperationType.REWARD) {
			if(!debug) {
				System.out.println("Reward operation");
			}
			space.reward(nextOp.getLsa(), nextOp.query, nextOp.reward, nextOp.maxQst1);
		}
	}

	/**
	 * Adds an operation to the queue of operations to be performed
	 * 
	 * @param operation the operation to be queued
	 * @return the Id of the LSA injected if the operation is an inject, null
	 *         otherwise
	 */
	public void queueOperation(Operation operation) {
		boolean toDebug = true;
		//System.out.println("queueOperations:" + operation.getOpType().toString());
		if(operation.getLsa() != null) {
			// Clean subscriptions that are already linked to the same agent
			String agentName = operation.getLsa().getAgentName();
			if(notifier.hasSubscriptions(agentName)) {
				if(toDebug) {
					MiddlewareLogger.getInstance().info("queueOperations : clean " + operation.getOpType()  + " subscriptions linked to " + agentName);
				}
				notifier.unsubscribe(agentName);
			}
		}
		if (operation.getOpType() == OperationType.INJECT || operation.getOpType() == OperationType.UPDATE) {
			Subscription subsDecay = new Subscription(DecayedEvent.class, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsDecay);

			Subscription subsBond = new Subscription(BondEvent.class, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsBond);

			Subscription subsSpreading = new Subscription(SpreadingEvent.class, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsSpreading);

			if(operation.getLsa().hasSyntheticProperty(SyntheticPropertyName.AGGREGATION)) {
				Subscription subsUpdate = new Subscription(AggregationEvent.class, operation.getRequestingAgent(),
						operation.getLsa().getAgentName());
				notifier.subscribe(subsUpdate);
			}

			// Check if quality of service is activated for the agent
			if(operation.isQoSactivated()) {
				Subscription subsReward = new Subscription(RewardEvent.class, operation.getRequestingAgent(),
						operation.getLsa().getAgentName());
				notifier.subscribe(subsReward);
			}
		}
		else if (operation.getOpType() == OperationType.REMOVE) {
			Subscription s = new Subscription(AggregationEvent.class, operation.getRequestingAgent(), null);
			notifier.unsubscribe(s);
		}
		// Adds the operation to the queue
		operationsQueue.add(operation);
	}

	public void logNotifier() {
		notifier.log();
	}
}
