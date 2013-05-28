package com.blitz.ingenico.test;

import java.net.URL;
import java.net.URLClassLoader;

import jpos.JposConst;
import jpos.JposException;
import jpos.MICR;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.DirectIOEvent;
import jpos.events.DirectIOListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class RunMICR {
	
	private MICR micr = null;
	private EventListener listener = new EventListener();
	private class EventListener implements StatusUpdateListener, DirectIOListener, ErrorListener, DataListener{
        public void statusUpdateOccurred( StatusUpdateEvent sue ){
        }
        
        public void directIOOccurred( DirectIOEvent dioe){
        }
        
        public void errorOccurred(ErrorEvent ee){
            String msg = "Unknown Extended Error: ";
            int errorcode = ee.getErrorCode();
            System.out.println("Error occured "+errorcode);
        }
        
        public void dataOccurred(DataEvent dataEvent) {
            try {
				System.out.println("Data after reading  : " + micr.getRawData());
			} catch (JposException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

	/**
	 * Test function to run a CMC7 reading. Read code and adapt to your
	 * environment.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {	
        System.out.println("----- BEGIN -----");
		RunMICR run = new RunMICR();
		run.start();
	}
	
	public RunMICR() {
		micr = new MICR();
	}
	
	public void start() {
		try {
			System.out.println("##### Opening ...");
			micr.open("defaultMICR");
			micr.addDataListener(listener);
			micr.addDirectIOListener(listener);
			micr.addErrorListener(listener);
			micr.addStatusUpdateListener(listener);
			System.out.println("##### Claiming ...");
			micr.claim(0);
			System.out.println("##### Begin Insertion ...");
			micr.beginInsertion(JposConst.JPOS_FOREVER);
			System.out.println("##### End Insertion ...");
			micr.endInsertion();
			System.out.println("##### Begin Removal ...");
			micr.beginRemoval(JposConst.JPOS_FOREVER);
			System.out.println("##### End Removal ...");
			micr.endRemoval();
			System.out.println("##### Release ...");
			micr.release();
			System.out.println("##### Close ...");
			micr.close();
		} catch (JposException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
