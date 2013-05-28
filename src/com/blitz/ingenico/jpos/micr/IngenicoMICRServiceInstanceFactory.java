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

package com.blitz.ingenico.jpos.micr;

import jpos.JposException;
import jpos.config.JposEntry;
import jpos.config.JposEntry.Prop;
import jpos.loader.JposServiceInstance;
import jpos.loader.JposServiceInstanceFactory;

/**
 * Build Ingenico MICR service
 * <p>
 * Control the properties in JavaPOS/JCL file
 * 
 * @author D.Carol
 */
public final class IngenicoMICRServiceInstanceFactory implements
		JposServiceInstanceFactory {
	public JposServiceInstance createInstance(String paramString,
			JposEntry paramJposEntry) throws JposException {
		if (!(paramJposEntry.hasPropertyWithName("serviceClass")))
			throw new JposException(104,
					"The JposEntry does not contain the 'serviceClass' property");

		// Check that property "CommPortNumber" exist
		if (!(paramJposEntry.hasPropertyWithName("portName")))
			throw new JposException(104,
					"The JposEntry does not contain the 'commPortNumber' property");

		// Get the serial port number
		int lCommPortNumber = -1;
		Prop lCommPortNumberProp = paramJposEntry.getProp("portName");
		lCommPortNumber = Integer.parseInt(lCommPortNumberProp
				.getValueAsString());

		IngenicoMICRService localJposServiceInstance;
		try {
			localJposServiceInstance = new IngenicoMICRService();
			localJposServiceInstance.setCommPortNumber(lCommPortNumber);

		} catch (Exception localException) {
			throw new JposException(jpos.JposConst.JPOS_E_NOSERVICE,
					"Could not create the service instance!", localException);
		}
		return localJposServiceInstance;
	}
}
