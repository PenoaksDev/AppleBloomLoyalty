package com.chiorichan.apps.rewards;

import com.chiorichan.database.SqlColumn;
import com.chiorichan.database.SqlTable;

public class DeviceTable extends SqlTable
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
	
	public String getTable()
	{
		return "devices";
	}
	
}
