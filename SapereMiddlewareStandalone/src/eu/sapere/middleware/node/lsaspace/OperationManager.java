package eu.sapere.middleware.node.lsaspace;

import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import eu.sapere.middleware.node.NodeManager;
import eu.sapere.middleware.node.notifier.Notifier;
import eu.sapere.middleware.node.notifier.Subscription;
import eu.sapere.middleware.node.notifier.event.BondEvent;
import eu.sapere.middleware.node.notifier.event.DecayedEvent;
import eu.sapere.middleware.node.notifier.event.LsaUpdatedEvent;
import eu.sapere.middleware.node.notifier.event.PropagationEvent;
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
		boolean ended = false;
		do {
			if (!operationsQueue.isEmpty()) {
				execNextOp();
			} else {
				ended = true;
			}
		} while ((new Date().getTime() - startTime < NodeManager.SLEEPTIME) && !ended);
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
		System.out.println("execOp:" + nextOp.getOpType().toString());
		if (nextOp.getOpType() == OperationType.INJECT) {
			if (!nextOp.getLsa().getAgentName().contains("*"))
				space.inject(nextOp.getLsa());
			else {
				if (!space.getAllLsa().containsKey(
						nextOp.getLsa().getAgentName().substring(0, nextOp.getLsa().getAgentName().indexOf('*'))))
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
			System.out.println("Reward operation");
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
		System.out.println("queueOperations:" + operation.getOpType().toString());
		if (operation.getOpType() == OperationType.INJECT || operation.getOpType() == OperationType.UPDATE) {
			DecayedEvent eventDecay = new DecayedEvent(operation.getLsa());
			Subscription subsDecay = new Subscription(eventDecay, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsDecay);

			BondEvent eventBond = new BondEvent(operation.getLsa(), null);
			Subscription subsBond = new Subscription(eventBond, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsBond);

			PropagationEvent eventPropagation = new PropagationEvent(null);
			Subscription subsPropagation = new Subscription(eventPropagation, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsPropagation);

			LsaUpdatedEvent eventUpdate = new LsaUpdatedEvent(null);
			Subscription subsUpdate = new Subscription(eventUpdate, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsUpdate);

			RewardEvent eventReward = new RewardEvent(operation.getLsa(), operation.query, operation.getReward(),
					operation.maxQst1);
			Subscription subsReward = new Subscription(eventReward, operation.getRequestingAgent(),
					operation.getLsa().getAgentName());
			notifier.subscribe(subsReward);

		} 
		else if (operation.getOpType() == OperationType.REMOVE) {
			LsaUpdatedEvent event = new LsaUpdatedEvent(operation.getLsa());
			Subscription s = new Subscription(event, operation.getRequestingAgent(), null);
			notifier.unsubscribe(s);
		}
		// Adds the operation to the queue
		operationsQueue.add(operation);
	}
}
