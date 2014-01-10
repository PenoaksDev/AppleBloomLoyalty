package com.chiorichan.apps.rewards.packet;

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.net.Packet;

/**
 * Allows the server to transmit configuration updates to the client.
 * Suggested that you only use this packet for Server -> Client
 */
public class ConfigurationPacket extends Packet
{
	public YamlConfiguration config;
}
