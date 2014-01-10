package com.chiorichan.apps.rewards;

public class DeviceAsset
{
	private String connectionUUID = null;
	
	public void setUUID( String uuid )
	{
		connectionUUID = uuid;
	}
	
	public String getUUID()
	{
		return connectionUUID;
	}
}
