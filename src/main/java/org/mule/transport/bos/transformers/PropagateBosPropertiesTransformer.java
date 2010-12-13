package org.mule.transport.bos.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.bos.BosBpms;

public class PropagateBosPropertiesTransformer extends
AbstractMessageTransformer {

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		String[] props=new String[]{BosBpms.BOS_PROPERTY_ACTIVITY,BosBpms.BOS_PROPERTY_ACTIVITY_NAME,BosBpms.BOS_PROPERTY_DESTINATION_VARIABLE};
		Object o;
		for(String s:props)
		{
			o=message.getProperty(s, PropertyScope.INBOUND);
			if(o!=null)
			{
				message.setProperty(s, o, PropertyScope.OUTBOUND);
			}
		}
		return message;
	}

}
