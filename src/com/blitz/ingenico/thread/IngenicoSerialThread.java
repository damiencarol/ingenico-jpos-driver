//
//	This file is part of Blitz B.S. JavaPOS library.
//
//    Blitz B.S. JavaPOS library is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Blitz B.S. JavaPOS library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Blitz B.S. JavaPOS library.  If not, see <http://www.gnu.org/licenses/>.
//
package com.blitz.ingenico.thread;

//derived from SUN's examples in the javax.comm package
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jpos.JposConst;
import jpos.JposException;

import com.blitz.ingenico.thread.concurrency.WaitDataHelper;
import com.blitz.ingenico.thread.concurrency.WaitNotBusyHelper;
import com.blitz.utils.SpecialByteChar;

public class IngenicoSerialThread implements Runnable {

	private final WaitNotBusyHelper notBusyWaiter = new WaitNotBusyHelper();

	/**
	 * Return the {@link WaitNotBusyHelper} instance managing this thread
	 * notifications on device ready to work. Use this to wait for the device to
	 * be ready for new tasks.
	 * 
	 * @return the {@link WaitNotBusyHelper} instance
	 */
	public WaitNotBusyHelper getNotBusyWaiter() {
		return this.notBusyWaiter;
	}

	private final WaitDataHelper dataWaiter = new WaitDataHelper();

	/**
	 * Return the {@link WaitDataHelper} instance managing this thread
	 * notifications on new datas. Use this ti wait for datas comming from the
	 * device.
	 * 
	 * @return the {@link WaitNotBusyHelper} instance
	 */
	public WaitDataHelper getDataWaiter() {
		return this.dataWaiter;
	}

	private boolean busy;

	/**
	 * Returns true if the device is busy.
	 * 
	 * @return true if the device is busy, otherwise false
	 */
	public boolean isBusy() {
		return this.busy;
	}

	private List<byte[]> errorMessages = new ArrayList<byte[]>();

	/**
	 * Get the error messages list. An error message is the raw data of a
	 * failure response. This list may be empty (not null) if there is no error.
	 * 
	 * @return a List<byte[]> containing the error messages received from the
	 *         device.
	 */
	public List<byte[]> getErrorMessages() {
		return this.errorMessages;
	}

	private List<byte[]> lines = new ArrayList<byte[]>();

	/**
	 * Get the lines list. A line is the raw data of a successful response. This
	 * list may be empty (not null) if there is no line.
	 * 
	 * @return a List<byte[]> containing the lines received from the device.
	 */
	public List<byte[]> getLines() {
		return this.lines;
	}

	public void clearLines() {
		lines.clear();
	}

	private IngenicoSerialThreadState state = IngenicoSerialThreadState.IDLE;

	/**
	 * Get the current state of the Ingenico Serial Thread.
	 * 
	 * @return the state of the thread
	 */
	public IngenicoSerialThreadState getState() {
		return this.state;
	}

	private InputStream inputStream;
	private Thread readThread;
	private RXTXPort serialPort;

	private int timeOut;
	private byte[] functionsToSend = new byte[0];
	private byte[] datasToSend = new byte[0];

	private byte[] dataReceived = new byte[0];

	private IngenicoSerialThread() {
	}

	/**
	 * Initialize a thread to run an Ingenico check reader/printer through COM
	 * port.
	 * 
	 * @param commName
	 *            The name of the COM port to us.
	 * @throws PortInUseException
	 *             If the COM port is already used by another application.
	 * @throws UnsupportedCommOperationException
	 *             If the COM port doesn't exist.
	 */
	public IngenicoSerialThread(String commName) throws PortInUseException, UnsupportedCommOperationException {
		this();
		serialPort = new RXTXPort(commName);
		inputStream = serialPort.getInputStream();

		// activate the DATA_AVAILABLE notifier
		serialPort.notifyOnDataAvailable(true);

		// these are default values for Ingenico check readers.
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_7, SerialPort.STOPBITS_2, SerialPort.PARITY_EVEN);

		this.busy = true;

		// start the read thread
		readThread = new Thread(this);
		readThread.start();
	}

	/**
	 * Automatically called to run the main thread.
	 */
	public void run() {
		// first thing in the thread, we initialize the write operation
		initwritetoport();
		try {

			int lrcComputed = 0;
			int busyCount = 0;

			while (true) {
				// System.out.println(">> Thread State - " + state);
				switch (state) {

				// Repos
				case IDLE:
					if (this.inputStream.available() > 0) {
						busyCount = 0;
						busy = true;
						if (receiveChar(serialPort) == SpecialByteChar.ENQ) {
							state = IngenicoSerialThreadState.RC_RECEIVED_ENQ;
						}
					} else {
						if (functionsToSend.length != 0) {
							busyCount = 0;
							busy = true;
							state = IngenicoSerialThreadState.EM_SEND_ENQ;
						} else {
							Thread.sleep(100);

							busyCount++;
							if (busyCount > 6) {
								this.busy = false;
								synchronized (notBusyWaiter) {
									// Notify that not busy
									notBusyWaiter.notifyNotBusy();
								}
							}
						}
					}
					break;

				// Emits ENQ
				case EM_SEND_ENQ:
					sendChar(serialPort, SpecialByteChar.ENQ);
					timeOut = 0;
					state = IngenicoSerialThreadState.EM_WAIT_ACK_AFTER_ENQ;
					break;

				case EM_WAIT_ACK_AFTER_ENQ:
					if (serialPort.getInputStream().available() > 0) {
						timeOut = 0;
						byte car = receiveChar(serialPort);

						if (car == SpecialByteChar.ACK) {
							state = IngenicoSerialThreadState.EM_SEND_DATA;
						} else {
							if (car == SpecialByteChar.ENQ) {
								state = IngenicoSerialThreadState.RC_RECEIVED_ENQ;
							} else
								state = IngenicoSerialThreadState.IDLE;
						}
					} else {
						if (timeOut > 10) {
							state = IngenicoSerialThreadState.IDLE;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case EM_SEND_DATA:
					// Erase error messages
					this.errorMessages.clear();

					// byte[] tab = functionsToSend.clone();
					int functionsLength = functionsToSend.length;
					byte[] datas = new byte[functionsLength + datasToSend.length];
					for (int i = 0; i < functionsLength; i++) {
						datas[i] = functionsToSend[i];
					}
					for (int i = functionsLength; i < datas.length; i++) {
						datas[i] = datasToSend[i - functionsLength];
					}
					sendMessage(serialPort, datas);
					state = IngenicoSerialThreadState.EM_WAIT_ACK_AFTER_DATA;
					timeOut = 0;
					break;

				case EM_WAIT_ACK_AFTER_DATA:
					// If datas were received
					if (serialPort.getInputStream().available() > 0) {
						// If it's positive (ACK)
						if (receiveChar(serialPort) == SpecialByteChar.ACK) {
							// Sending EOT
							sendChar(serialPort, SpecialByteChar.EOT);
							functionsToSend = new byte[0];
							datasToSend = new byte[0];
							state = IngenicoSerialThreadState.IDLE;
						} else {
							functionsToSend = new byte[0];
							datasToSend = new byte[0];
							state = IngenicoSerialThreadState.IDLE;
						}
						timeOut = 0;
					} else {
						if (timeOut == 4) {
							state = IngenicoSerialThreadState.IDLE;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				// Recois ENQ
				case RC_RECEIVED_ENQ:
					sendChar(serialPort, SpecialByteChar.ACK);
					state = IngenicoSerialThreadState.RC_WAIT_STX;
					dataReceived = new byte[0];
					timeOut = 0;
					break;

				// Attente STX
				case RC_WAIT_STX:
					if (serialPort.getInputStream().available() > 0) {
						if (receiveChar(serialPort) == SpecialByteChar.STX) {
							state = IngenicoSerialThreadState.RC_RECEIVING_DATA;
							dataReceived = new byte[0];
						}
					} else {
						if (timeOut == 4) {
							state = IngenicoSerialThreadState.IDLE;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RC_RECEIVING_DATA:

					if (serialPort.getInputStream().available() > 0) {
						byte car = receiveChar(serialPort);

						if (car == SpecialByteChar.ETX) {
							lrcComputed = lrcComputed ^ SpecialByteChar.ETX;
							state = IngenicoSerialThreadState.RC_WAIT_LRC;
							timeOut = 0;
						} else {
							lrcComputed = lrcComputed ^ car;
							byte[] newData = new byte[dataReceived.length + 1];
							for (int i = 0; i < dataReceived.length; i++) {
								newData[i] = dataReceived[i];
							}
							newData[newData.length - 1] = car;
							dataReceived = newData;
						}
						timeOut = 0;
					} else {
						if (timeOut > 4) {
							state = IngenicoSerialThreadState.RC_WAIT_STX;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RC_WAIT_LRC:

					if (serialPort.getInputStream().available() > 0) {

						byte lrcReceived = receiveChar(serialPort);

						if (lrcReceived == lrcComputed) {
							// this was a successful receiving
							sendChar(serialPort, SpecialByteChar.ACK);
							lrcComputed = 0;

							if (dataReceived[0] == 0x20 || dataReceived[1] != 0x30) {
								// bad order or error while process
								this.errorMessages.add(dataReceived.clone());
							} else {
								// good datas
								this.lines.add(dataReceived.clone());
							}
							// notify that datas are available
							dataWaiter.notifyData();

							// // Add check signature
							// if (dataReceived.length == 48 && dataReceived[0]
							// == 51) {
							// this.lines.add(dataReceived.clone());
							// } else {
							// // ERROR information
							// if (dataReceived.length > 2) {
							// String thirdCara = new
							// String(dataReceived.clone()).substring(0, 2);
							//
							// if (thirdCara.charAt(1) != '0') {
							// this.errorMessages.add(dataReceived.clone());
							// }
							// }
							// }

							timeOut = 0;
							state = IngenicoSerialThreadState.RC_WAIT_EOT;
						} else {
							sendChar(serialPort, SpecialByteChar.NAK);

							timeOut = 0;
							state = IngenicoSerialThreadState.RC_WAIT_STX;
						}
					} else {
						if (timeOut > 4) {
							state = IngenicoSerialThreadState.RC_WAIT_STX;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RC_WAIT_EOT:
					if (serialPort.getInputStream().available() > 0) {
						int car = receiveChar(serialPort);

						if (car == SpecialByteChar.EOT) {
							if (dataReceived[2] == 0x31) {
								state = IngenicoSerialThreadState.IDLE;
								// m_fonctionEL210 = EL210_FONCTION_LECTURECMC7;
							} else {
								state = IngenicoSerialThreadState.PAUSE;
							}
						} else if (car == SpecialByteChar.STX) { // MOAR DATAS
							timeOut = 0;
							state = IngenicoSerialThreadState.RC_RECEIVING_DATA;
						} else {
							state = IngenicoSerialThreadState.IDLE;
						}
					} else {
						if (timeOut > 4) {
							state = IngenicoSerialThreadState.IDLE;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case PAUSE:
					Thread.sleep(100);
					state = IngenicoSerialThreadState.IDLE;
					break;

				default:
					System.err.print("ERROR UNEXCPECTED STATE" + state.toString());
					throw new JposException(JposConst.JPOS_E_FAILURE, "erreur interne");
				}
			}
		} catch (InterruptedException e) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JposException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Abort this thread. Close the serial port.
	 */
	public void abort() {
		if (this.readThread != null) {
			this.readThread.interrupt();
			this.serialPort.close();
		}
	}

	/**
	 * Send a simple order message to the device. A simple order message is an
	 * order that requires no data. This is a low level function. You need
	 * knowledges about the Ingenico protocol to use this one.
	 * 
	 * @param functions
	 *            a byte array of function codes. There can't be more than 16
	 *            codes. If the array contains more than 16 codes, it will be
	 *            truncated to 16. The device will execute theses codes
	 *            sequentially. The sequence must be coherent to ensure correct
	 *            running of the device.
	 */
	public void sendSimpleOrderMessage(byte[] functions) {
		// TODO truncate the functions array to 16
		this.busy = true;
		this.functionsToSend = functions;
	}

	/**
	 * Send an order message to the device. This is a low level function. You
	 * need knowledges about the Ingenico protocol to use this one.
	 * 
	 * @param functions
	 *            A byte array of function codes. There can't be more than 16
	 *            codes. If the array contains more than 16 codes, it will be
	 *            truncated to 16. The device will execute theses codes
	 *            sequentially. The sequence must be coherent to ensure correct
	 *            running of the device.
	 * @param datas
	 *            A byte array of datas to send with the codes. Can be null.
	 */
	public void sendOrderMessage(byte[] functions, byte[] datas) {
		this.sendSimpleOrderMessage(functions);
		if (datas != null) {
			this.datasToSend = datas;
		}
	}

	/**
	 * Initiate the serial Port.
	 */
	private void initwritetoport() {
		try {
			// activate the OUTPUT_BUFFER_EMPTY notifier
			serialPort.notifyOnOutputEmpty(true);
		} catch (Exception e) {
			System.out.println("Error setting event notification");
			System.out.println(e.toString());
			System.exit(-1);
		}
	}

	/**
	 * Send a complete message to the device. Complete message is
	 * [STX][DATAS][ETX][LRC]. STX, ETX and LRC are automatically added by the
	 * function.
	 * 
	 * @param serialPort
	 *            Serial port to use.
	 * @param tab
	 *            Datas to send.
	 */
	private void sendMessage(RXTXPort serialPort, byte[] datas) {

		byte[] datasToSend = new byte[datas.length + 3];

		// Write STX
		datasToSend[0] = SpecialByteChar.STX;
		// Write Data
		byte lrc = 0;
		for (int i = 0; i < datas.length; i++) {
			datasToSend[i + 1] = datas[i];
			lrc = (byte) (lrc ^ datas[i]);
		}
		// Write ETX
		datasToSend[datas.length + 1] = SpecialByteChar.ETX;
		lrc = (byte) (lrc ^ SpecialByteChar.ETX);
		// Write the lrc;
		datasToSend[datas.length + 2] = lrc;

		// Send to serial buffer
		try {
			serialPort.getOutputStream().write(datasToSend);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Send one byte to the device.
	 * 
	 * @param serialPort
	 *            Serial port to use.
	 * @param character
	 *            Char to send.
	 */
	private void sendChar(RXTXPort serialPort, byte character) {
		try {
			serialPort.getOutputStream().write(character);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Receive one byte from the device.
	 * 
	 * @param serialPort
	 *            Serial port to use.
	 * @return The byte read received from the device.
	 */
	private byte receiveChar(RXTXPort serialPort2) {
		while (true) {
			int res = -1;
			try {
				res = serialPort2.getInputStream().read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (res != -1)
				return (byte) res;
		}
	}
}
