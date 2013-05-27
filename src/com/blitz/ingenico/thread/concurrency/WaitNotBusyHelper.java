package com.blitz.ingenico.thread.concurrency;

/**
 * This class is used to make other threads wait or to notify them when the
 * serial thread is not busy. All the utility functions are already synchronized.
 * 
 * @author Pierre Degand
 * 
 */
public class WaitNotBusyHelper {

	private boolean notified = false;

	/**
	 * Use this function after a {@link WaitNotBusyHelper#waitNotBusy()} call to know if the
	 * thread notified this object or if a timeout occured.
	 * 
	 * @return true if it has been notified by the thread, otherwise false.
	 */
	public boolean isNotified() {
		return this.notified;
	}

	/**
	 * Notifies the waiting threads that the serial thread is ready.
	 * This sould be call by the serial thread to notify the service thread.
	 */
	public synchronized void notifyNotBusy() {
		this.notify();
		notified = true;
	}

	/**
	 * When called, the current thread will pause indefinitely and wait for the serial thread to be ready.
	 * The current thread will run again when {@link WaitNotBusyHelper#notifyNotBusy()} is called from the serial thread.
	 * @throws InterruptedException on interrupt
	 */
	public synchronized void waitNotBusy() throws InterruptedException {
		notified = false;
		this.wait();
	}

	/**
	 * When called, the current thread will pause during a given time and wait for the serial thread to be ready.
	 * The current thread will run again when {@link WaitNotBusyHelper#notifyNotBusy()} is called from the serial thread or the timout is reached.
	 * If the timeout is reached, the {@link WaitNotBusyHelper#isNotified()} call will return false.
	 * @param timeout timeout in millisecond
	 * @throws InterruptedException on interrupt
	 */
	public synchronized void waitNotBusy(int timeout) throws InterruptedException {
		notified = false;
		this.wait(timeout);
	}

}
