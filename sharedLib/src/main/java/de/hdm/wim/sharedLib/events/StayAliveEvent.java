package de.hdm.wim.sharedLib.events;

import de.hdm.wim.sharedLib.Constants.PubSub.AttributeKey;
import de.hdm.wim.sharedLib.Constants.PubSub.EventType;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by GezimKrasniqi on 20.06.17.
 */
public class StayAliveEvent extends IEvent {

	private Map<String, String> attributes = new HashMap<String, String>();

	public StayAliveEvent(){
		this.attributes.put(AttributeKey.EVENT_TYPE, EventType.STAYALIVE);
	}


	public String getUserId(){
		return this.attributes.get(AttributeKey.USER_ID).toString();
	}

	public void setUserId(String userId){
		this.attributes.put(AttributeKey.USER_ID, userId);
	}
}
