package com.chiorichan.apps.rewards.packet;

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.net.Packet;

public class DeviceInformationPacket extends Packet
{
	public String title, image, address1, address2;
	//public Color bg, fb, font;
	
	protected DeviceInformationPacket()
	{
		
	}
	
	public DeviceInformationPacket(String _title, String _image, String _address1, String _address2)
	{
		title = _title;
		image = _image;
		address1 = _address1;
		address2 = _address2;
	}
	
	public YamlConfiguration saveToYaml()
	{
		YamlConfiguration yaml = new YamlConfiguration();
		
		yaml.set( "device.title", title );
		yaml.set( "device.img", image );
		yaml.set( "device.address1", address1 );
		yaml.set( "device.address2", address2 );
		
		return yaml;
	}
}
