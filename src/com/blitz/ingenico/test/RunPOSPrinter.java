package com.blitz.ingenico.test;

import java.util.Date;

import jpos.JposConst;
import jpos.JposException;

import com.blitz.ingenico.IngenicoPOSPrinterService;

public class RunPOSPrinter {

	/**
	 * Test function for printing on a check. Read code and adapt to your
	 * environment.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		IngenicoPOSPrinterService service = new IngenicoPOSPrinterService();
		try {
			System.out.println("##### Begin reader initialization.");
			service.setCommPortNumber(1); // ADAPT THIS
			System.out.println("##### Claiming ...");
			service.claim(JposConst.JPOS_FOREVER);
			System.out.println("##### Begin Insertion ...");
			service.beginInsertion(JposConst.JPOS_FOREVER);
			String textToPrint = "ID:012345678912";
			System.out.println("##### Printing text ... - " + textToPrint);
			service.printText(textToPrint);
//			String amountToPrint = "83425"; //76.99e
//			Date today = new Date();
//			String placeToPrint = "Puteaux";
//			String beneficiaryToPrint = "INGENICO S.A.";
//			System.out.println("##### Printing check ...");
//			service.printCheck(amountToPrint, today, placeToPrint, beneficiaryToPrint);
			System.out.println("##### Release ...");
			service.release();
			System.out.println("##### Close ...");
			service.close();
		} catch (JposException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

}
