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

import java.lang.reflect.Constructor;
import jpos.JposException;
import jpos.config.JposEntry;
import jpos.config.JposEntry.Prop;
import jpos.loader.JposServiceInstance;
import jpos.loader.JposServiceInstanceFactory;

public final class IngenicoMICRServiceInstanceFactory
  implements JposServiceInstanceFactory
{
  @SuppressWarnings("unchecked")
public JposServiceInstance createInstance(String paramString, JposEntry paramJposEntry)
    throws JposException
  {
    if (!(paramJposEntry.hasPropertyWithName("serviceClass")))
      throw new JposException(104, "The JposEntry does not contain the 'serviceClass' property");

    // Check that property "CommPortNumber" exist
    if (!(paramJposEntry.hasPropertyWithName("commPortNumber")))
        throw new JposException(104, "The JposEntry does not contain the 'commPortNumber' property");

    JposServiceInstance localJposServiceInstance = null;
    try
    {
      String str = (String)paramJposEntry.getPropertyValue("serviceClass");
      Class localClass = Class.forName(str);

      Class[] arrayOfClass = new Class[0];

      Constructor localConstructor = localClass.getConstructor(arrayOfClass);

      localJposServiceInstance = (JposServiceInstance)localConstructor.newInstance(arrayOfClass);

      IngenicoMICRService localDeviceService = (IngenicoMICRService)localJposServiceInstance;
      
      // Get the comm port number
      int lCommPortNumber = -1;
      Prop lCommPortNumberProp = paramJposEntry.getProp("commPortNumber");
      lCommPortNumber = Integer.parseInt(lCommPortNumberProp.getValueAsString());
      localDeviceService.setCommPortNumber(lCommPortNumber);
    }
    catch (Exception localException) {
      throw new JposException(jpos.JposConst.JPOS_E_NOSERVICE, "Could not create the service instance!", localException);
    }
    return localJposServiceInstance;
  }
}
