package com.chiorichan.apps.rewards;

import com.chiorichan.database.SqlColumn;

public class DeviceAsset
{
	@SqlColumn( name = "deviceId" )
	public String uuid;
	@SqlColumn( name = "lastIp" )
	public String lastIp;
	@SqlColumn( name = "lastActive" )
	public long lastActive;
	@SqlColumn( name = "created" )
	public long created;
	@SqlColumn( name = "serial" )
	public String serial;
	@SqlColumn( name = "model" )
	public String model;
	@SqlColumn( name = "appVersion" )
	public String appVersion;
	@SqlColumn( name = "status" )
	public String status;
	@SqlColumn( name = "type" )
	public String type;
	@SqlColumn( name = "locationId" )
	public String locId;
	@SqlColumn( name = "state" )
	public String state;
	
	public DeviceAsset(DeviceTable row)
	{
		uuid = row.uuid;
		lastIp = row.lastIp;
		lastActive = row.lastActive;
		created = row.created;
		serial = row.serial;
		model = row.model;
		appVersion = row.appVersion;
		status = row.status;
		locId = row.locId;
		state = row.state;
	}
	
	public String toString()
	{
		return "DeviceAsset(uuid='" + uuid + "', lastIp='" + lastIp + "', lastActive='" + lastActive + "', created='" + created + "', serial='" + serial + "', model='" + model + "', appVersion='" + appVersion + "', status='" + status + "', locId='" + locId + "', state='" + state + "')";
	}
	
	public DeviceAsset()
	{
	}
	
	public void setUUID( String _uuid )
	{
		uuid = _uuid;
	}
	
	public String getUUID()
	{
		return uuid;
	}
}
