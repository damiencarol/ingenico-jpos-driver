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

import jpos.JposConst;
import jpos.JposException;
import jpos.MICRConst;
import jpos.events.DataEvent;
import jpos.events.ErrorEvent;
import jpos.services.EventCallbacks;
import jpos.services.MICRService110;

public class IngenicoMICRService implements MICRService110 {
	
	private static final int INGENICO_CANCEL_INSERT_CHECK = 30;
	private static final int INGENICO_INSERT_CHECK = 51;
	private static final int INGENICO_EJECT_CHECK = 52;
	
	private IngenicoSerialThread internalThread = null;
	//private boolean enabled = false;
	private int commPortNumber;
	//private boolean opened = false;
	private int state = JposConst.JPOS_S_CLOSED;
	private EventCallbacks cb;
	private DataEvent dataEvent;
	private int dataCount = 0;
	private String rawData;
	private boolean claimed = false;

	@Override
	public void clearInputProperties() throws JposException {
		this.rawData = null;
	}

	@Override
	public void compareFirmwareVersion(String firmwareFileName, int[] result)
			throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getCapCompareFirmwareVersion() throws JposException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getCapUpdateFirmware() throws JposException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateFirmware(String firmwareFileName) throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getCapStatisticsReporting() throws JposException {
		// E210 has no statistic
		return false;
	}

	@Override
	public boolean getCapUpdateStatistics() throws JposException {
		// E210 has no statistic
		return false;
	}

	@Override
	public void resetStatistics(String statisticsBuffer) throws JposException {
		// E210 has no statistic
		return;
	}

	@Override
	public void retrieveStatistics(String[] statisticsBuffer)
			throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateStatistics(String statisticsBuffer) throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getCapPowerReporting() throws JposException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPowerNotify() throws JposException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPowerState() throws JposException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPowerNotify(int powerNotify) throws JposException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * TODO call CANCEL_INSERT with a timeout
	 */
	@Override
	public void beginInsertion(int timeout) throws JposException {

        this.state = JposConst.JPOS_S_BUSY;
		
		// Check that timeout is correct
		if ((timeout != JposConst.JPOS_FOREVER) && (timeout < 0)) {
			throw new JposException(JposConst.JPOS_E_ILLEGAL, "An invalid timeout parameter was specified");	
		}


		// Throw INSERT COMMAND
        while (this.internalThread.getBusy() == true)
		{}
		//System.out.println("ENVOI INGENICO_INSERT_CHECK");
        this.internalThread.setFunction(INGENICO_INSERT_CHECK);
        // wait command is received
		while (this.internalThread.getBusy() == true)
		{}
		//System.out.println("recu par device INGENICO_INSERT_CHECK");

        this.internalThread.pistes.clear();

		

		// If timeout is set to forever
		if (timeout == JposConst.JPOS_FOREVER)
		{
            // wait a check information
			while (this.internalThread.pistes.size() == 0)
			{
                if (this.internalThread.errorMessages.size() > 0)
                {
                    for (int i=0; i<this.internalThread.errorMessages.size(); i++)
                    {
                        System.err.println("[" + new String(this.internalThread.errorMessages.get(i)) + "]");
                    }
                    throw new JposException(MICRConst.JPOS_EMICR_BADDATA, "");
                }
            }
		}
		else
		{
			int timeoutPassed = 0;
			// wait a check information
			while (this.internalThread.getBusy() != false)
			{
				if (timeoutPassed > timeout){
					throw new JposException(JposConst.JPOS_E_TIMEOUT, "The specified time has elapsed without the form being properly inserted");
				}
				
				// Wait 100 milliseconds and save that time
				timeoutPassed += 100;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void beginRemoval(int timeout) throws JposException {

	    // Wait the device is not busy
        while (this.internalThread.getBusy() == true)
		{}
        // Throw INSERT COMMAND
		this.internalThread.setFunction(INGENICO_EJECT_CHECK);
        // wait command is received
		while (this.internalThread.getBusy() == true)
		{}
		







        int timeoutPassed = 0;
        // Wait that reader release check
        while (this.internalThread.getBusy() != false)
        {
           if (timeout > 0)
           {
               if (timeoutPassed > timeout){
					throw new JposException(JposConst.JPOS_E_TIMEOUT, "");
				}

               // Wait 100 milliseconds and save that time
				timeoutPassed += 100;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
           }
        }
	}

	@Override
	public void clearInput() throws JposException {
		this.dataCount=0;
		this.dataEvent=null;
	}

	@Override
	public void endInsertion() throws JposException {
		// Test si une piste a ete renseignee
		if (this.internalThread.pistes.size() > 0)
		{
			this.dataEvent = new DataEvent(this, 0);
			this.dataCount = 1;
	

			this.rawData = new String(this.internalThread.pistes.get(0));
			
			this.cb.fireDataEvent(this.dataEvent);
		}
        else
        {
            ErrorEvent error = new ErrorEvent(this, JposConst.JPOS_E_EXTENDED, 0, 0, 0);
            // Aucune data EROR
            this.cb.fireErrorEvent(error);
        }
	}
	
	@Override
	public void endRemoval() throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAccountNumber() throws JposException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String getAmount() throws JposException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public boolean getAutoDisable() throws JposException {
		// By default auto-disable
		return true;
	}

	@Override
	public String getBankNumber() throws JposException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public boolean getCapValidationDevice() throws JposException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getCheckType() throws JposException {
		// TODO Auto-generated method stub
		return MICRConst.MICR_CT_UNKNOWN;
	}

	@Override
	public int getCountryCode() throws JposException {
		// Elite 210 is CMC7 check reader that correspond 
		// to 'UNKNOWN'
		return MICRConst.MICR_CC_UNKNOWN;
	}

	@Override
	public int getDataCount() throws JposException {
		// Number of Magnetic printing
		return this.dataCount;
	}

	@Override
	public boolean getDataEventEnabled() throws JposException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getEPC() throws JposException {
		// CMC7 had not this field 
		return "";
	}

	@Override
	public String getRawData() throws JposException {
		// Return the CMC7
		return this.rawData;
	}

	@Override
	public String getSerialNumber() throws JposException {
		return "";
	}

	@Override
	public String getTransitNumber() throws JposException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void setAutoDisable(boolean autoDisable) throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDataEventEnabled(boolean dataEventEnabled)
			throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkHealth(int level) throws JposException {
		// TODO Auto-generated method stub
		
	}

	
	@Override
    public void claim(int timeout) throws JposException {
	    
	    try
    	{
            // Create the internal thread
            this.internalThread = new IngenicoSerialThread("COM" + this.commPortNumber);
            
            // Wait that the communication thread is not busy
            while (this.internalThread.getBusy() == true) 
            {}
            // Command the physical device to cancel insert operation
            this.internalThread.setFunction(INGENICO_CANCEL_INSERT_CHECK);
            // Wait that the communication thread is not busy
            while (this.internalThread.getBusy() == true) 
            {}
            // Command the physical device to eject check if their are one in
            this.internalThread.setFunction(INGENICO_EJECT_CHECK);
            // Wait that the communication thread is not busy
            while (this.internalThread.getBusy() == true) 
            {}
            
            this.claimed = true;
    	}
	    catch (Exception e)
	    {
	        throw new JposException(JposConst.JPOS_E_NOTCLAIMED, "Error in device preparation", e);
	    }
    }
    
	@Override
	public void close() throws JposException {
		this.claimed = false;
	}

	@Override
	public void directIO(int command, int[] data, Object object)
			throws JposException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCheckHealthText() throws JposException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public boolean getClaimed() throws JposException {
		// Return internal value
		return this.claimed;
	}

	@Override
	public boolean getDeviceEnabled() throws JposException {
		return true;
	}

	@Override
	public String getDeviceServiceDescription() throws JposException {
		// TODO Auto-generated method stub
		return "Blitz B.S. JavaPOS Ingenico MICR devices Service";
	}

    @Override
    public int getDeviceServiceVersion() throws JposException {
        // Driver is developed for version 1.10
        return 1010000;
    }

	@Override
	public boolean getFreezeEvents() throws JposException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPhysicalDeviceDescription() throws JposException {
		// Information about hardware
		return "???";
	}

	@Override
	public String getPhysicalDeviceName() throws JposException {
		// Information about hardware
		return "???";
	}

	@Override
	public int getState() throws JposException {
		return this.state;
	}

	@Override
	public void open(String logicalName, EventCallbacks cb)
			throws JposException {

	
	    this.state = JposConst.JPOS_S_IDLE;
	    this.cb = cb;
	}

	
    @Override
    public void release() throws JposException {
        
        // Check if the Service is not claimed
        if (this.claimed == false){
            return;
        }
    
        // Stop the thread
        this.internalThread.abort();
        
        // signify that it's ok
        this.claimed = false;
        this.state = JposConst.JPOS_S_IDLE;        
    }


	@Override
    public void setDeviceEnabled(boolean deviceEnabled) throws JposException {
	    if (deviceEnabled == true)
	    {
	     // Wait that the communication thread is not busy
	        while (this.internalThread.getBusy() == true) 
	        {}
	        // Command the physical device to cancel insert operation
	        this.internalThread.setFunction(INGENICO_CANCEL_INSERT_CHECK);
	        // Wait that the communication thread is not busy
	        while (this.internalThread.getBusy() == true) 
	        {}
	        // Command the physical device to eject check if their are one in
	        this.internalThread.setFunction(INGENICO_EJECT_CHECK);
	        // Wait that the communication thread is not busy
	        while (this.internalThread.getBusy() == true) 
	        {}
	    }
    }

    @Override
    public void setFreezeEvents(boolean freezeEvents) throws JposException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteInstance() throws JposException {
    }

    public void setCommPortNumber(int pCommPortNumber) throws JposException {
        // save the port number
        this.commPortNumber = pCommPortNumber;
        if (!(pCommPortNumber > 0 && pCommPortNumber < 255)) {
            throw new JposException(JposConst.JPOS_E_FAILURE,
                    "Invalid comm port number");
        }
    }
}
