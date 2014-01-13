package com.chiorichan.apps.rewards;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.apps.rewards.packet.ConfigurationPacket;
import com.chiorichan.apps.rewards.packet.DeviceInformationPacket;
import com.chiorichan.apps.rewards.packet.LookupContactPacket;
import com.chiorichan.apps.rewards.packet.RegistrationPacket;
import com.chiorichan.apps.rewards.packet.UUIDRequestPacket;
import com.chiorichan.apps.rewards.packet.UpdatePacket;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandExecutor;
import com.chiorichan.command.CommandSender;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.net.IncomingPacketEvent;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.net.Packet;
import com.chiorichan.plugin.java.JavaPlugin;
import com.chiorichan.util.Common;
import com.esotericsoftware.kryonet.Connection;

public class Plugin extends JavaPlugin implements Listener, CommandExecutor
{
	private List<DeviceAsset> devices = new ArrayList<DeviceAsset>();
	private SqlConnector db;
	
	public void onEnable()
	{
		db = Loader.getPersistenceManager().getSiteManager().getSiteById( "applebloom" ).getDatabase();
		
		Loader.registerPacket( ConfigurationPacket.class );
		Loader.registerPacket( RegistrationPacket.class );
		Loader.registerPacket( UpdatePacket.class );
		Loader.registerPacket( UUIDRequestPacket.class );
		Loader.registerPacket( DeviceInformationPacket.class );
		Loader.registerPacket( LookupContactPacket.class );
		
		Loader.getPluginManager().registerEvents( this, this );
		
		getCommand( "test" ).setExecutor( this );
		
		Loader.getScheduler().scheduleAsyncRepeatingTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				
			}
		}, 60L, 60L );
		
		try
		{
			loadDevices();
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	private void loadDevices() throws SQLException
	{
		DeviceTable devicest = (DeviceTable) new DeviceTable().select( db, "SELECT * FROM `devices`" );
		
		while ( devicest.next() )
		{
			DeviceAsset d = (DeviceAsset) devicest.toObject( new DeviceAsset() );
			
			Loader.getLogger().info( "Added device: " + d );
			
			devices.add( d );
		}
	}
	
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		sender.sendMessage( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Thanks for using the testing command. :D" );
		
		try
		{
			ResultSet rs = db.query( "select * from `contacts_rewards`;" );
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration( new File( "test.yaml" ) );
			
			while ( rs.next() )
			{
				Contact contact = new Contact( rs.getString( "mobile_no" ), rs.getString( "locID" ), rs.getInt( "balance" ), rs.getLong( "last_instore_check" ) );
				
				sender.sendMessage( contact + "" );
				
				yaml.set( "contacts." + rs.getString( "mobile_no" ), contact );
			}
			
			yaml.save( new File( "test.yaml" ) );
		}
		catch ( SQLException | IOException e )
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onIncomingPacketEvent( IncomingPacketEvent event ) throws SQLException
	{
		if ( event.isHandled() )
			return;
		
		Packet packet = event.getPacket();
		Connection con = event.getConnection();
		SqlConnector sql = Loader.getPersistenceManager().getSiteManager().getSiteById( "applebloom" ).getDatabase();
		
		event.setHandled( true );
		
		Loader.getLogger().info( con.getUUID() );
		
		if ( packet instanceof UUIDRequestPacket )
		{
			( (UUIDRequestPacket) packet ).isReply = true;
			( (UUIDRequestPacket) packet ).uuid = Common.md5( "TESTING RESPONSE" );
			
			con.sendTCP( packet );
		}
		else if ( packet instanceof RegistrationPacket )
		{
			RegistrationPacket rpacket = (RegistrationPacket) packet;
			
			String uuid = rpacket.getUUID();
			
			con.sendTCP( new DeviceInformationPacket( "Dunkin' Donuts", "http://images.applebloom.co/rewards/header_dunkin.png", "P.O. Box 753", "Hazel Crest, Illinois 60429" ) );
		}
		else if ( packet instanceof LookupContactPacket )
		{
			LookupContactPacket lpacket = (LookupContactPacket) packet;
			
			// TODO Fix this query so it selects using the device location ID, check if the result is good and checks if the device is even registered.
			ResultSet rs = sql.query( "SELECT * FROM `contacts_rewards` WHERE `mobile_no` = '" + lpacket.getPhone() + "'" );
			
			if ( rs != null && sql.getRowCount( rs ) > 0 )
			{
				Contact contact = new Contact( rs.getString( "mobile_no" ), rs.getString( "locID" ), rs.getInt( "balance" ), rs.getLong( "last_instore_check" ) );
				
				// contact.setName( rs.getString( "" ) );
				// contact.setEmail( rs.getString( "" ) );
				
				lpacket.setContact( contact );
			}
			
			con.sendTCP( lpacket );
		}
		else
		{
			event.setHandled( false );
		}
	}
}
