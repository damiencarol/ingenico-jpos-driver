package com.blitz.ingenico.test;

import com.blitz.ingenico.jpos.micr.IngenicoMICRService;

import jpos.BaseControl;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.DataEvent;
import jpos.events.DirectIOEvent;
import jpos.events.ErrorEvent;
import jpos.events.OutputCompleteEvent;
import jpos.events.StatusUpdateEvent;
import jpos.services.EventCallbacks;

public class RunMICR {

	/**
	 * Test function to run a CMC7 reading. Read code and adapt to your
	 * environment.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			IngenicoMICRService service = new IngenicoMICRService();
			EventCallbacks cb = new EventCallbacks() { // Lazy callback init ...

				@Override
				public BaseControl getEventSource() {
					System.out.println("BaseControl");
					return null;
				}

				@Override
				public void fireStatusUpdateEvent(StatusUpdateEvent arg0) {
					System.out.println("fireStatusUpdateEvent");

				}

				@Override
				public void fireOutputCompleteEvent(OutputCompleteEvent arg0) {
					System.out.println("fireOutputCompleteEvent");

				}

				@Override
				public void fireErrorEvent(ErrorEvent arg0) {
					System.out.println("fireErrorEvent");

				}

				@Override
				public void fireDirectIOEvent(DirectIOEvent arg0) {
					System.out.println("fireDirectIOEvent");

				}

				@Override
				public void fireDataEvent(DataEvent arg0) {
					IngenicoMICRService source = (IngenicoMICRService) arg0.getSource();
					try {
						System.out.println("Data Received : " + source.getRawData());
					} catch (JposException e) {
						e.printStackTrace();
					}

				}
			};

			System.out.println("##### Begin reader initialization.");
			service.setCommPortNumber(1); // ADAPT THIS
			System.out.println("##### Opening ...");
			service.open("", cb);
			System.out.println("##### Claiming ...");
			service.claim(JposConst.JPOS_FOREVER);
			System.out.println("##### Begin Insertion ...");
			service.beginInsertion(JposConst.JPOS_FOREVER);
			System.out.println("##### End Insertion ...");
			service.endInsertion();
			System.out.println("##### Begin Removal ...");
			service.beginRemoval(JposConst.JPOS_FOREVER);
			System.out.println("##### End Removal ...");
			service.endRemoval();
			System.out.println("##### Release ...");
			service.release();
			System.out.println("##### Close ...");
			service.close();

		} catch (JposException e) {
			e.printStackTrace();
		}

	}

}
