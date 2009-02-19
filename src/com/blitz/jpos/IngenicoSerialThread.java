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
package com.blitz.jpos;

//derived from SUN's examples in the javax.comm package
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jpos.JposConst;
import jpos.JposException;

public class IngenicoSerialThread implements Runnable//, EventCallbacks// , SerialPortEventListener
{
	class SpecialCar {
		public static final byte STX = 2;
		public static final byte ETX = 3;
		public static final byte EOT = 4;
		public static final byte ENQ = 5;
		public static final byte ACK = 6;
		public static final byte NAK = 0x15;
	}

	public ArrayList<byte[]> pistes = new ArrayList<byte[]>();

	/*
	 * public static CommPortIdentifier portId; public static CommPortIdentifier
	 * saveportId; static Enumeration portList;
	 */
	InputStream inputStream;
	/*
	 * SerialPort serialPort;
	 */
	Thread readThread;
	RXTXPort serialPort;

	static String messageString = "Hello, world!";
	static OutputStream outputStream;
	static boolean outputBufferEmptyFlag = false;

	/*
	 * public static void main(String[] args) { boolean portFound = false;
	 * String defaultPort;
	 * 
	 * // determine the name of the serial port on several operating systems
	 * String osname = System.getProperty("os.name","").toLowerCase(); if (
	 * osname.startsWith("windows") ) { // windows defaultPort = "COM1"; } else
	 * if (osname.startsWith("linux")) { // linux defaultPort = "/dev/ttyS0"; }
	 * else if ( osname.startsWith("mac") ) { // mac defaultPort = "????"; }
	 * else {
	 * System.out.println("Sorry, your operating system is not supported");
	 * return; }
	 * 
	 * if (args.length > 0) { defaultPort = args[0]; }
	 * 
	 * System.out.println("Set default port to "+defaultPort);
	 * 
	 * // parse ports and if the default port is found, initialized the reader
	 * portList = CommPortIdentifier.getPortIdentifiers(); while
	 * (portList.hasMoreElements()) { portId = (CommPortIdentifier)
	 * portList.nextElement(); if (portId.getPortType() ==
	 * CommPortIdentifier.PORT_SERIAL) { if
	 * (portId.getName().equals(defaultPort)) {
	 * System.out.println("Found port: "+defaultPort); portFound = true; // init
	 * reader thread E210SerialThread reader = new E210SerialThread(); } }
	 * 
	 * } if (!portFound) { System.out.println("port " + defaultPort +
	 * " not found."); }
	 * 
	 * }
	 */

	public void initwritetoport() {
		// initwritetoport() assumes that the port has already been opened and
		// initialized by "public nulltest()"

		// try {
		// get the outputstream
		outputStream = serialPort.getOutputStream();
		// } catch (IOException e) {}

		try {
			// activate the OUTPUT_BUFFER_EMPTY notifier
			serialPort.notifyOnOutputEmpty(true);
		} catch (Exception e) {
			System.out.println("Error setting event notification");
			System.out.println(e.toString());
			System.exit(-1);
		}

	}

	public void writetoport() {
		System.out.println("Writing \"" + messageString + "\" to "
				+ serialPort.getName());
		try {
			// write string to serial port
			outputStream.write(messageString.getBytes());
		} catch (IOException e) {
		}
	}

	public void writeEnq() {
		// Write ENQ to serial port
		try {
			outputStream.write(5);
		} catch (IOException e) {
		}
	}

	private IngenicoSerialThread() {
	}

	public IngenicoSerialThread(String commName) {
		this();

		// portId = CommPortIdentifier.getPortIdentifier("COM3");

		// initalize serial port
		try {
			serialPort = new RXTXPort(commName);// (SerialPort)
												// portId.open("SimpleReadApp",
												// 2000);
		} catch (PortInUseException e) {
		}

		// try {
		inputStream = serialPort.getInputStream();
		// } catch (IOException e) {}

		// try {
		// serialPort.addEventListener(this);
		// } catch (TooManyListenersException e) {}

		// activate the DATA_AVAILABLE notifier
		serialPort.notifyOnDataAvailable(true);

		try {
			// set port parameters
			serialPort.setSerialPortParams(1200, SerialPort.DATABITS_7,
					SerialPort.STOPBITS_2, SerialPort.PARITY_EVEN);
		} catch (UnsupportedCommOperationException e) {

		}

		// start the read thread
		readThread = new Thread(this);
		readThread.start();
	}

	public enum E210_STATE {
		Repos,

		RcRecuENQ, RcAttenteSTX,

		EmEmetENQ, EmAttenteACKaENQ, EmData, EmAttenteACKaData, EmAttenteAckData,

		MASTER_SEND_COMMAND,

		SLAVE_IDE, SLAVE_RECEIVE,

		RcAttenteLRC, RcAttenteEOT, RcReceptionData,

		Delai, EmErreurData
	}

	private E210_STATE state = E210_STATE.Repos;
	private int m_fonctionEL210 = 0;

	private int timeOut;

	private byte[] m_strData = new byte[0];

    private ArrayList<byte[]> m_trame = new ArrayList<byte[]>();

	//private String m_strCMC7 = "";

	//private int m_Erreurs = 0;

	public void run() {
		// first thing in the thread, we initialize the write operation
		initwritetoport();
		try {
			/*
			 * while (true) {
			 * 
			 * 
			 * // write string to port, the serialEvent will read it
			 * //writetoport(); writeEnq(); Thread.sleep(1000);
			 * 
			 * }
			 */

			while (true) {
				// Attente de 1s (1000 ms)
				// Thread.Sleep(1000);
				//System.out.println("etape=" + state.toString());
				if (m_strData.length >= 48 && m_strData[0] == 51)// 48
				{//System.out.println("new_data size=" + Integer.toString(m_strData.length));
					//m_strCMC7 = "C"	+ (new String(m_strData)).substring(14,	26);
					this.pistes.clear();
					//System.out.println("etape=" + state.toString());
					this.pistes.add(m_strData.clone());
				}
				/*System.out.println("new_data");
				 if (m_strData.length >= 40 && m_strData[0] == 0x51)//48
			{
					 m_strCMC7 = "C" + (new String(m_strData)).substring(14, 34);
					 this.pistes.add(m_strCMC7.getBytes());
				 }*/
				
				

				int LRC_Calcul = 0;
				switch (state) {

				// Repos
				case Repos:
					if (this.inputStream.available() > 0) {
						if (RecoiChar(serialPort) == SpecialCar.ENQ) {
							state = E210_STATE.RcRecuENQ;
						}
					} else {
						if (m_fonctionEL210 != 0) {
							state = E210_STATE.EmEmetENQ;
						} else {							
							Thread.sleep(100);
						}
					}
					break;

				// Emits ENQ
				case EmEmetENQ:
					EnvoiChar(serialPort, SpecialCar.ENQ);
					timeOut = 0;
					state = E210_STATE.EmAttenteACKaENQ;
					break;

				case EmAttenteACKaENQ:
					if (serialPort.getInputStream().available() > 0) {
						timeOut = 0;
						if (RecoiChar(serialPort) == SpecialCar.ACK) {
							state = E210_STATE.EmData;
						} else {
							state = E210_STATE.Repos;
						}
					} else {
						if (timeOut > 10) {
							state = E210_STATE.Repos;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case EmData:
					byte[] tab = new byte[1];
					tab[0] = (byte) m_fonctionEL210;
					// tab[1] = (byte)EL210_FONCTION_EJECTION_TOTALE;
					SendMessage(serialPort, tab);
					state = E210_STATE.EmAttenteAckData;
					timeOut = 0;
					break;

				case EmAttenteAckData:
					// Si on a recus quelque chose
					if (serialPort.getInputStream().available() > 0) {
						// Si on nous repond positivement (ACK)
						if (RecoiChar(serialPort) == SpecialCar.ACK) {
							// On envoi fin de transmition
							EnvoiChar(serialPort, SpecialCar.EOT);
							m_fonctionEL210 = 0;
							state = E210_STATE.Repos;
						} else {
							// Pas bon
							state = E210_STATE.EmErreurData;
						}
						timeOut = 0;
					} else {
						if (timeOut == 4) {
							state = E210_STATE.Repos;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				// Recois ENQ
				case RcRecuENQ:
					EnvoiChar(serialPort, SpecialCar.ACK);
					state = E210_STATE.RcAttenteSTX;
					m_strData = new byte[0];
					timeOut = 0;
					break;

				// Attente STX
				case RcAttenteSTX:
					if (serialPort.getInputStream().available() > 0) {
						if (RecoiChar(serialPort) == SpecialCar.STX) {
							state = E210_STATE.RcReceptionData;
							m_strData = new byte[0];
						}
					} else {
						if (timeOut == 4) {
							state = E210_STATE.Repos;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RcReceptionData:

					if (serialPort.getInputStream().available() > 0) {
						byte car = RecoiChar(serialPort);

						if (car == SpecialCar.ETX) {
							LRC_Calcul = LRC_Calcul ^ SpecialCar.ETX;
							state = E210_STATE.RcAttenteLRC;
							timeOut = 0;
						} else {
							LRC_Calcul = LRC_Calcul ^ car;
							byte[] new_data = new byte[m_strData.length + 1];
							for (int i = 0; i < m_strData.length; i++) {
								new_data[i] = m_strData[i];
							}

							new_data[new_data.length - 1] = car;
							m_strData = new_data;

							/*System.out.println("new_data");
							 if (m_strData.length >= 40 && m_strData[0] == 0x51)//48
						{
								 m_strCMC7 = "C" + (new String(m_strData)).substring(14, 34);
								 this.pistes.add(m_strCMC7.getBytes());
							 }*/
							 
						}
						timeOut = 0;
					} else {
						if (timeOut > 4) {
							state = E210_STATE.RcAttenteSTX;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RcAttenteLRC:

					if (serialPort.getInputStream().available() > 0) {
						byte LRC_Recu = RecoiChar(serialPort);

						if (LRC_Recu == LRC_Calcul) {
							EnvoiChar(serialPort, SpecialCar.ACK);
							timeOut = 0;
							state = E210_STATE.RcAttenteEOT;
						} else {
							EnvoiChar(serialPort, SpecialCar.NAK);
							timeOut = 0;
							state = E210_STATE.RcAttenteSTX;
						}
					} else {
						if (timeOut > 4) {
							state = E210_STATE.RcAttenteSTX;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case RcAttenteEOT:
					if (serialPort.getInputStream().available() > 0) {
						int car = RecoiChar(serialPort);

						if (car == SpecialCar.EOT) {
							//m_Erreurs = m_strData[2] & 0xF;
							if (m_strData[2] == 0x31) {
								//m_Erreurs = 0; // Annulation effectuee
								state = E210_STATE.Repos;
								// m_fonctionEL210 = EL210_FONCTION_LECTURECMC7;
							} else {
								/*
								 * if (m_strData.Length >= 48 && m_strData[0] ==
								 * 0x03) { m_strCMC7 = "C" +
								 * ASCIIEncoding.ASCII.GetString(m_strData, 14,
								 * 34); }
								 */
								state = E210_STATE.Delai;
							}
						} else if (car == SpecialCar.STX) // Il a encore quelque chose a dire !
						{
							timeOut = 0;
							state = E210_STATE.RcReceptionData;
						} else {
							state = E210_STATE.Repos;
						}
					} else {
						if (timeOut > 4) {
							state = E210_STATE.Repos;
							timeOut = 0;
						} else {
							Thread.sleep(100);
							timeOut++;
						}
					}
					break;

				case Delai:					
					// Thread.Sleep(2000); // Attends 2 secondes
					Thread.sleep(100); // Attends 2 secondes
					// m_fonctionEL210= EL210_FONCTION_LECTURECMC7;
					state = E210_STATE.Repos;
					break;

				default:
                    System.err.print("ERROR UNEXCPECTED STATE" + state.toString());
					// Attente de 500 ms
					// Thread.Sleep(500);
					throw new JposException(JposConst.JPOS_E_FAILURE,
							"erreur interne");
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

	private void SendMessage(RXTXPort serialPort2, byte[] tab) {

		byte[] tab_to_send = new byte[tab.length + 3];
		
		// Write STX
		tab_to_send[0] = SpecialCar.STX;
		// Write Data
		byte lrc = 0;
		for (int i = 0; i < tab.length; i++)
		{
			tab_to_send[i + 1] = tab[i];
			lrc = (byte) (lrc ^ tab[i]);
		}
		// Write ETX
		tab_to_send[tab.length + 1] = SpecialCar.ETX;
		lrc = (byte) (lrc ^ SpecialCar.ETX);
		// Write the lrc;
		tab_to_send[tab.length + 2] = lrc;

		
		// Send to serial buffer
		try {
			serialPort2.getOutputStream().write(tab_to_send);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void EnvoiChar(RXTXPort serialPort2, byte character) {
		try {
			serialPort2.getOutputStream().write(character);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private byte RecoiChar(RXTXPort serialPort2) {

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

	public void abort() {

		if (this.readThread != null) {
			this.readThread.interrupt();
			this.serialPort.close();
		}
	}
	
	byte[] readBuffer = null;

	public void setFunction(int ingenicoCheckFunction) {
		this.m_fonctionEL210 = ingenicoCheckFunction;
	}

    public int getFunction() {
		return this.m_fonctionEL210;
	}

    public E210_STATE getState() {
        return this.state;
    }

	/*
	 * public void serialEvent(SerialPortEvent event) { switch
	 * (event.getEventType()) { case SerialPortEvent.BI: case
	 * SerialPortEvent.OE: case SerialPortEvent.FE: case SerialPortEvent.PE:
	 * case SerialPortEvent.CD: case SerialPortEvent.CTS: case
	 * SerialPortEvent.DSR: case SerialPortEvent.RI: case
	 * SerialPortEvent.OUTPUT_BUFFER_EMPTY: break; case
	 * SerialPortEvent.DATA_AVAILABLE: // we get here if data has been received
	 * //byte[] readBuffer = new byte[20]; try { // read data while
	 * (inputStream.available() > 0) { int numBytes =
	 * inputStream..read(readBuffer); } // print data String result = new
	 * String(readBuffer); System.out.println("Read: "+result); } catch
	 * (IOException e) {}
	 * 
	 * break; } }
	 */
}
