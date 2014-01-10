package com.chiorichan.apps.rewards.packet;

import com.chiorichan.net.Packet;

public class RegistrationPacket extends Packet
{
	protected String uuid;
	public boolean approved;
	
	protected RegistrationPacket()
	{
		
	}
	
	public RegistrationPacket( String _uuid )
	{
		uuid = _uuid;
	}
	
	public String getUUID()
	{
		return uuid;
	}
}
