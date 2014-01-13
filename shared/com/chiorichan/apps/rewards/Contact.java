package com.chiorichan.apps.rewards;

public class Contact
{
	public String mobile_no, locId, name = null, email = null;
	public int bal;
	public long last_check;
	
	public Contact(String var1, String var2, int var3, long var4)
	{
		mobile_no = var1;
		locId = var2;
		bal = var3;
		last_check = var4;
	}
	
	public void setName( String _name )
	{
		name = _name;
	}
	
	public void setEmail( String _email )
	{
		email = _email;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getEmail()
	{
		return email;
	}
}
