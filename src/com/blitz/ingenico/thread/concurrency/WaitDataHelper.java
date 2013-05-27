package com.blitz.ingenico.thread.concurrency;

/**
 * This class is used to make other threads wait or to notify them when the
 * serial thread has produced datas. All the utility functions are already synchronized.
 * 
 * @author Pierre Degand
 * 
 */
public class WaitDataHelper {
	
	private boolean notified = false;
	
	/**
	 * Use this function after a {@link WaitDataHelper#waitData(int)} call to know if the
	 * thread notified this object or if a timeout occured.
	 * 
	 * @return true if it has been notified by the thread, otherwise false.
	 */
	public boolean isNotified() {
		return this.notified;
	}

	/**
	 * Notifies the threads that are waiting for datas that datas are ready.
	 * This sould be call by the serial thread to notify the service thread.
	 */
	public synchronized void notifyData() {
		this.notify();
		notified = true;
	}
	
	/**
	 * When called, the current thread will pause indefinitely and wait for datas comming from the serial thread.
	 * The current thread will run again when {@link WaitDataHelper#notifyData()} is called from the serial thread.
	 * @throws InterruptedException on interrupt
	 */
	public synchronized void waitData() throws InterruptedException {
		notified = false;
		this.wait();
	}
	
	/**
	 * When called, the current thread will pause during a given time and wait for datas comming from the serial thread.
	 * The current thread will run again when {@link WaitDataHelper#notifyData()} is called from the serial thread or the timout is reached.
	 * If the timeout is reached, the {@link WaitDataHelper#isNotified()} call will return false.
	 * @param timeout timeout in millisecond
	 * @throws InterruptedException on interrupt
	 */
	public synchronized void waitData(int timeout) throws InterruptedException {
		notified = false;
		this.wait(timeout);
	}
}
