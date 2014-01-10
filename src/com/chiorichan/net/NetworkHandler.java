package com.chiorichan.net;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import co.applebloom.apps.rewards.LaunchActivity;

import com.chiorichan.apps.rewards.packet.ConfigurationPacket;
import com.chiorichan.apps.rewards.packet.DeviceInformationPacket;
import com.chiorichan.apps.rewards.packet.LookupContactPacket;
import com.chiorichan.apps.rewards.packet.RegistrationPacket;
import com.chiorichan.apps.rewards.packet.UUIDRequestPacket;
import com.chiorichan.apps.rewards.packet.UpdatePacket;
import com.chiorichan.net.packet.CommandPacket;
import com.chiorichan.util.Common;

@SuppressLint( "UseSparseArrays" )
public class NetworkHandler extends AsyncTask<Void, String, Void>
{
	public static final String TAG = "ChioriTCPHandler";
	
	private static final String SERVER_URL = "direct.applebloom.co";
	private static final int SERVER_PORT = 1024;
	private static Map<Integer, Packet> pendingPackets = new HashMap<Integer, Packet>();
	private static Map<Integer, Packet> sendingPackets = new HashMap<Integer, Packet>();
	private TcpClient mConnection;
	
	private String uuid = null;
	private boolean isRegistered = false;
	private int registrationTimeout = 0;
	
	public boolean sendPacket( Packet var1 )
	{
		if ( mConnection != null && mConnection.isConnected() )
		{
			sendingPackets.put( Common.getEpoch(), var1 );
			return true;
		}
		else
			return false;
	}
	
	public static void qPacket( Packet var1 )
	{
		pendingPackets.put( Common.getEpoch(), var1 );
	}
	
	@Override
	protected void onProgressUpdate( String... values )
	{
		
	}
	
	@Override
	protected Void doInBackground( Void... params )
	{
		do
		{
			try
			{
				if ( mConnection == null )
				{
					mConnection = new TcpClient( InetAddress.getByName( SERVER_URL ), SERVER_PORT, new ConnectionHandler() );
					
					mConnection.registerPacket( ConfigurationPacket.class );
					mConnection.registerPacket( RegistrationPacket.class );
					mConnection.registerPacket( UpdatePacket.class );
					mConnection.registerPacket( UUIDRequestPacket.class );
					mConnection.registerPacket( DeviceInformationPacket.class );
					mConnection.registerPacket( LookupContactPacket.class );
				}
				else if ( !mConnection.isConnected() )
					mConnection.attemptConnection( InetAddress.getByName( SERVER_URL ), SERVER_PORT );
				else
				{
					for ( Entry<Integer, Packet> packet : pendingPackets.entrySet() )
					{
						if ( packet.getKey() < Common.getEpoch() - 60000 )
							Log.w( TAG, "Is the client or network overwhelmed? There seems to be a major delay between receiving packets and processing them." );
						
						Log.w( TAG, "Receiving Packet " + packet.getKey() + " > " + packet.getValue() );
						
						if ( packet.getValue() instanceof CommandPacket )
						{
							CommandPacket var1 = ( (CommandPacket) packet.getValue() );
							
							if ( var1.getKeyword().toUpperCase().equals( "PONG" ) )
							{	
								
							}
						}
						else if ( packet.getValue() instanceof ConfigurationPacket )
						{
							LaunchActivity.getConfigHandler().pushChanges( ( (ConfigurationPacket) packet.getValue() ).config );
						}
						else if ( packet.getValue() instanceof UUIDRequestPacket )
						{
							uuid = ( (UUIDRequestPacket) packet.getValue() ).uuid;
							LaunchActivity.getConfigHandler().getConfig().set( "device.uuid", uuid );
							LaunchActivity.getConfigHandler().saveConfig();
							
							Log.i( TAG, "We got a NEW UUID assigned to us from the server: " + uuid );
						}
						else if ( packet.getValue() instanceof DeviceInformationPacket )
						{
							DeviceInformationPacket var = ( (DeviceInformationPacket) packet.getValue() );
							LaunchActivity.getConfigHandler().pushChanges( var.saveToYaml() );
							LaunchActivity.uiNeedsUpdating = true;
						}
						else if ( packet.getValue() instanceof LookupContactPacket )
						{
							LaunchActivity.serverResult = ( (LookupContactPacket) packet.getValue() );
						}
						
						pendingPackets.remove( packet.getKey() );
					}
					
					for ( Entry<Integer, Packet> packet : sendingPackets.entrySet() )
					{
						Log.w( TAG, "Sending Packet " + packet.getKey() + " > " + packet.getValue() );
						
						// TODO Make sure we are not sending out dated packets?
						mConnection.sendPacket( packet.getValue() );
						
						sendingPackets.remove( packet.getKey() );
					}
					
					sendPingCountdown--;
					
					if ( sendPingCountdown < 1 )
					{
						// Send ping every minute
						sendPingCountdown = 30;
						mConnection.sendPing();
					}
					
					/**
					 * The main activity will set the UUID once it has the opportunity to read from configuration.
					 * As long as the UUID is NULL registration will not take place.
					 */
					if ( uuid != null && !isRegistered )
						registerMe( false );
					
					if ( uuid == null )
						isRegistered = false;
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
			
			// publishProgress( null );
			SystemClock.sleep( 2000 );
		}
		while ( true );
	}
	
	private int sendPingCountdown = 0;
	
	public void registerMe( boolean force )
	{
		if ( uuid != null && ( !isRegistered || force ) )
		{
			// Unless were forcing this registration, We need to make sure we're giving the server a proper amount of time to register us. 90 seconds.
			if ( registrationTimeout > Common.getEpoch() - 90 && !force )
				return;
			
			registrationTimeout = Common.getEpoch();
			
			if ( !Common.isValidMD5( uuid ) )
			{
				Log.i( TAG, "Device Registration: FAILED! - UUID is not valid" );
				LaunchActivity.cookToast( "Device Registration: FAILED! - UUID is not valid" );
				isRegistered = false;
				uuid = null;
				
				// We have no UUID stored in our preferences. Have the server assign us one.
				// String seed = Settings.Secure.getString( LaunchActivity.getInstance().getContentResolver(), "android_id" );
				
				// if ( seed == null )
				// seed = UUID.randomUUID().toString();
				
				// mConnection.sendPacket( new UUIDRequestPacket( seed ) );
			}
			else
			{
				Log.i( TAG, "Device Registration: Send Registration to Server" );
				mConnection.sendPacket( new RegistrationPacket( uuid ) );
			}
		}
	}
	
	public boolean isRegistered()
	{
		return isRegistered;
	}
	
	public void setUUID( String _uuid )
	{
		uuid = _uuid;
	}
	
	public String getUUID()
	{
		return uuid;
	}
	
	public String getDeviceState()
	{
		if ( uuid == null || !Common.isValidMD5( uuid ) )
		{
			return "Unregistered!";
		}
		else
		{
			if ( isRegistered )
			{
				return "Registered!";
			}
			else
			{
				return "Pending Registration!";
			}
		}
	}
	
	public String getNetworkState()
	{
		if ( mConnection.isConnected() )
			return "Connected!";
		else
			return "Disconnected!";
	}
	
	public boolean isConnected()
	{
		return mConnection.isConnected();
	}
}
