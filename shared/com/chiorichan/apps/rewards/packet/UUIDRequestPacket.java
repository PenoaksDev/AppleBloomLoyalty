package com.chiorichan.apps.rewards.packet;

import com.chiorichan.net.Packet;

public class UUIDRequestPacket extends Packet
{
	public boolean isReply = false;
	public String uuid = "";
	
	public UUIDRequestPacket()
	{}

	public UUIDRequestPacket(String _uuid)
	{
		uuid = _uuid;
	}
}
