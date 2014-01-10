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
	
	public void onEnable()
	{
		Loader.registerPacket( ConfigurationPacket.class );
		Loader.registerPacket( RegistrationPacket.class );
		Loader.registerPacket( UpdatePacket.class );
		Loader.registerPacket( UUIDRequestPacket.class );
		Loader.registerPacket( DeviceInformationPacket.class );
		
		Loader.getPluginManager().registerEvents( this, this );
		
		getCommand( "test" ).setExecutor( this );
		
		Loader.getScheduler().scheduleAsyncRepeatingTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				
			}
		}, 60L, 60L );
	}
	
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		sender.sendMessage( ChatColor.NEGATIVE + "" + ChatColor.GOLD + "Thanks for using the testing command. :D" );
		
		SqlConnector db = Loader.getPersistenceManager().getSiteManager().getSiteById( "applebloom" ).getDatabase();
		
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
	public void onIncomingPacketEvent( IncomingPacketEvent event )
	{
		if ( event.isHandled() )
			return;
		
		Packet packet = event.getPacket();
		Connection con = event.getConnection();
		
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
			con.sendTCP( new DeviceInformationPacket( "Dunkin' Donuts", "http://images.applebloom.co/rewards/header_dunkin.png", "P.O. Box 753", "Hazel Crest, Illinois 60429" ) );
		}
		else
		{
			event.setHandled( false );
		}
	}
}
