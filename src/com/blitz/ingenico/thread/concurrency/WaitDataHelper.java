package com.blitz.ingenico.thread.concurrency;

public class WaitDataHelper {
	
	private boolean notified = false;
	
	public boolean isNotified() {
		return this.notified;
	}

	public synchronized void notifyData() {
		this.notify();
		notified = true;
	}
	
	public synchronized void waitData() throws InterruptedException {
		notified = false;
		this.wait();
	}
	
	public synchronized void waitData(int timeout) throws InterruptedException {
		notified = false;
		this.wait(timeout);
	}
}
