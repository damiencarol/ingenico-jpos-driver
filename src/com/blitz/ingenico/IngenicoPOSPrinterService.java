package com.blitz.ingenico;

import java.text.SimpleDateFormat;
import java.util.Date;

import jpos.JposConst;
import jpos.JposException;

import com.blitz.ingenico.thread.IngenicoSerialThread;

/**
 * POSPrinter Service for Ingenico Check Reader. Please note that this is
 * <strong>NOT</strong> a POSPrinterService API implementation and should not be
 * used as so. The JPOS API implementation is a futur feature. This service is
 * only usable in a standard JSE application.
 * 
 * @author Pierre Degand
 * 
 */
public class IngenicoPOSPrinterService {

	private IngenicoSerialThread internalThread = null;
	private int commPortNumber;
	private String rawData;
	private boolean claimed = false;

	public void setCommPortNumber(int pCommPortNumber) throws JposException {
		// save the port number
		this.commPortNumber = pCommPortNumber;
		if (!(pCommPortNumber > 0 && pCommPortNumber < 255)) {
			throw new JposException(JposConst.JPOS_E_FAILURE, "Invalid comm port number");
		}
	}

	public void claim(int timeout) throws JposException {
		try {
			// Create the internal thread
			this.internalThread = new IngenicoSerialThread("COM" + this.commPortNumber);
			System.out.println(1);
			// Wait the thread is not busy
			waitThreadNotBusy();
			System.out.println(2);
			// Command the physical device to cancel insert operation
			byte[] data = { IngenicoFunction.INGENICO_CANCEL_INSERT_CHECK };
			this.internalThread.sendSimpleOrderMessage(data);
			System.out.println(3);
			waitThreadNotBusy();
			System.out.println(4);
			// Command the physical device to eject check if their are one in
			byte[] data2 = { IngenicoFunction.INGENICO_EJECT_CHECK };
			this.internalThread.sendSimpleOrderMessage(data2);
			System.out.println(5);
			// Wait that the communication thread is not busy
			waitThreadNotBusy();
			System.out.println(6);
			this.claimed = true;
		} catch (Exception e) {
			e.printStackTrace();
			throw new JposException(JposConst.JPOS_E_NOTCLAIMED, "Error in device preparation", e);
		}
	}

	/**
	 * Ask the printer to insert a check.
	 * 
	 * @param timeout
	 * @throws JposException
	 */
	public void beginInsertion(int timeout) throws JposException {
		// this.state = JposConst.JPOS_S_BUSY;

		// Check that timeout is correct
		if ((timeout != JposConst.JPOS_FOREVER) && (timeout < 0)) {
			throw new JposException(JposConst.JPOS_E_ILLEGAL, "An invalid timeout parameter was specified");
		}

		// Throw INSERT COMMAND
		waitThreadNotBusy();

		byte[] data = { IngenicoFunction.INGENICO_INSERT_CHECK };
		this.internalThread.sendSimpleOrderMessage(data);
		// wait command is received
		waitThreadNotBusy();

		this.internalThread.clearLines();

		if (!waitThreadDatas(timeout)) {
			throw new JposException(JposConst.JPOS_E_TIMEOUT, "The specified time has elapsed without the form being properly inserted");
		}

		// Change state to idle
		// this.state = JposConst.JPOS_S_IDLE;
	}

	/**
	 * Print some raw text 80 char max
	 * 
	 * @param station
	 * @param text
	 * @throws JposException
	 */
	public void printNormal(int station, String text) throws JposException {
		if (text == null || text.trim().length() == 0) {
			throw new JposException(JposConst.JPOS_E_ILLEGAL, "An invalid text parameter was specified");
		}

		waitThreadNotBusy();

		byte[] functions = { IngenicoFunction.INGENICO_PRINT_TEXT };
		byte[] datas = text.getBytes();
		System.out.print("Input : ");
		for(int i = 0; i < datas.length; i++) {
			System.out.print(Integer.toHexString(datas[i]));
			System.out.print(' ');
		}
		System.out.println();
		internalThread.sendOrderMessage(functions, datas);
		// wait command is received
		waitThreadNotBusy();
		// wait printing is done
		waitThreadDatas();
		
		if(internalThread.getErrorMessages().size() > 0) {
			beginRemoval(JposConst.JPOS_FOREVER);
			throw new JposException(JposConst.JPOS_E_FAILURE, "Fail to print message");
		}
		if(internalThread.getLines().size() > 0) {
			byte[] line = internalThread.getLines().get(0);
			System.out.print("Output : ");
			for(int i = 0; i < line.length; i++) {
				System.out.print(Integer.toHexString(line[i]));
				System.out.print(' ');
			}
			System.out.println();
		}
	}

	public void printText(String text) throws JposException {
		String textToSend = "";
		StringBuilder builder;

		builder = new StringBuilder();
		if (text != null) {
			text = text.trim().toUpperCase();
			builder.append('e');
			builder.append(text);
			textToSend = builder.toString();
		}
		this.printNormal(0, textToSend);
	}

	public void printCheck(String amount, Date date, String place, String beneficiary) throws JposException {
		StringBuilder builder;
		String amountToSend = "";
		String dateToSend = "";
		String placeToSend = "";
		String beneficiaryToSend = "";

		builder = new StringBuilder();
		if (amount != null) {
			amount = amount.trim();
			int amountLength = amount.length();
			builder.append('a');
			if (amountLength > 10) {
				builder.append(amount.substring(0, 10));
			} else {
				for (int i = 0; i < 10 - amountLength; i++) {
					builder.append('0');
				}
				builder.append(amount);
			}
			amountToSend = builder.toString();
		}

		builder = new StringBuilder();
		if (date != null) {
			builder.append('b');
			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			dateToSend = builder.append(formatter.format(date)).toString();
		}

		builder = new StringBuilder();
		if (place != null) {
			place = place.trim();
			place = place.toUpperCase();
			int placeLength = place.length();
			builder.append('c');
			if (placeLength > 12) {
				placeToSend = place.substring(0, 12);
			} else {
				builder.append(place);
				for (int i = 0; i < 12 - placeLength; i++) {
					builder.append(' ');
				}
				placeToSend = builder.toString();
			}
		}

		builder = new StringBuilder();
		if (beneficiary != null) {
			beneficiary = beneficiary.trim();
			beneficiary = beneficiary.toUpperCase();
			int beneficiaryLength = beneficiary.length();
			builder.append('d');
			if (beneficiaryLength > 25) {
				beneficiaryToSend = beneficiary.substring(0, 25);
			} else {
				builder.append(beneficiary);
				for (int i = 0; i < 25 - beneficiaryLength; i++) {
					builder.append(' ');
				}
				beneficiaryToSend = builder.toString();
			}
		}

		builder = new StringBuilder();
		builder.append(amountToSend).append(dateToSend).append(placeToSend).append(beneficiaryToSend).append('f').append('9');
		this.printNormal(0, builder.toString());
	}

	/**
	 * Remove the check from the check printer.
	 * 
	 * @param timeout
	 * @throws JposException
	 */
	public void beginRemoval(int timeout) throws JposException {

		// Change state to busy
		// this.state = JposConst.JPOS_S_BUSY;

		// Check that timeout is correct
		if ((timeout != JposConst.JPOS_FOREVER) && (timeout < 0)) {
			throw new JposException(JposConst.JPOS_E_ILLEGAL, "An invalid timeout parameter was specified");
		}
		// Wait the device is not busy
		waitThreadNotBusy();
		// Throw EJECT function
		byte[] data = { IngenicoFunction.INGENICO_EJECT_CHECK };
		this.internalThread.sendSimpleOrderMessage(data);
		// wait command is received
		waitThreadNotBusy();

		// Wait that reader release check
		if (!waitThreadDatas(timeout)) {
			throw new JposException(JposConst.JPOS_E_TIMEOUT, "The specified time has elapsed without a check being properly ejected");
		}

		// Change state to busy
		// this.state = JposConst.JPOS_S_IDLE;
	}

	public void release() throws JposException {

		// Check if the Service is not claimed
		if (this.claimed == false) {
			return;
		}

		// Stop the thread
		this.internalThread.abort();

		// signify that it's ok
		this.claimed = false;
		// this.state = JposConst.JPOS_S_IDLE;
	}

	public void close() throws JposException {
		this.claimed = false;
		// this.state = JposConst.JPOS_S_CLOSED;
	}

	private boolean waitThreadNotBusy() throws JposException {
		try {
			internalThread.getNotBusyWaiter().waitNotBusy();
		} catch (InterruptedException e) {
			throw new JposException(JposConst.JPOS_E_FAILURE, "The waiting service has been interrupted");
		}
		return internalThread.getNotBusyWaiter().isNotified();
	}

	private boolean waitThreadNotBusy(int timeout) throws JposException {
		if (timeout == JposConst.JPOS_FOREVER) {
			return waitThreadDatas();
		} else {
			try {
				internalThread.getNotBusyWaiter().waitNotBusy(timeout);

			} catch (InterruptedException e) {
				throw new JposException(JposConst.JPOS_E_FAILURE, "The waiting service has been interrupted");
			}
			return internalThread.getNotBusyWaiter().isNotified();
		}
	}

	private boolean waitThreadDatas() throws JposException {
		try {
			internalThread.getDataWaiter().waitData();
		} catch (InterruptedException e) {
			throw new JposException(JposConst.JPOS_E_FAILURE, "The waiting service has been interrupted");
		}
		return internalThread.getDataWaiter().isNotified();
	}

	private boolean waitThreadDatas(int timeout) throws JposException {
		if (timeout == JposConst.JPOS_FOREVER) {
			return waitThreadDatas();
		} else {
			try {
				internalThread.getDataWaiter().waitData(timeout);

			} catch (InterruptedException e) {
				throw new JposException(JposConst.JPOS_E_FAILURE, "The waiting service has been interrupted");
			}
			return internalThread.getDataWaiter().isNotified();
		}
	}
}
