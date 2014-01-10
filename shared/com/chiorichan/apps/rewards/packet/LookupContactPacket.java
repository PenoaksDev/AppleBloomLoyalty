package com.chiorichan.apps.rewards.packet;

import com.chiorichan.apps.rewards.Contact;
import com.chiorichan.net.Packet;

public class LookupContactPacket extends Packet
{
	protected String phone, id;
	protected Contact contact = null;
	
	public LookupContactPacket(String phoneNumber, String _id)
	{
		phone = phoneNumber;
		id = _id;
	}
	
	public String getId()
	{
		return id;
	}
	
	public String getPhone()
	{
		return phone;
	}
	
	public void setContact( Contact _contact )
	{
		contact = _contact;
	}
	
	public Contact getContact()
	{
		return contact;
	}
}
