package com.blitz.ingenico.thread.concurrency;

public class WaitNotBusyHelper {

		
		private boolean notified = false;
		
		public boolean isNotified() {
			return this.notified;
		}

		public synchronized void notifyNotBusy() {
			this.notify();
			notified = true;
		}
		
		public synchronized void waitNotBusy() throws InterruptedException {
			notified = false;
			this.wait();
		}
		
		public synchronized void waitNotBusy(int timeout) throws InterruptedException {
			notified = false;
			this.wait(timeout);
		}

}
