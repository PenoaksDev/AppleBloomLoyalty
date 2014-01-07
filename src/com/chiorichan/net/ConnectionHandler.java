package com.chiorichan.net;

import static com.chiorichan.net.SocketService.TAG;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import co.applebloom.apps.rewards.LaunchActivity;

import com.chiorichan.net.packet.CommandPacket;

public class ConnectionHandler extends TcpConnection
{
	@Override
	public void onConnect()
	{
		Log.d( TAG, "Status: Connected to " + getRemoteAddressTCP() );
		
		SocketService.register( true );
		
		Toast.makeText( LaunchActivity.getAppContext(), "Successfully made a TCP Connection! YOUR GOLDEN!!! :D", Toast.LENGTH_SHORT ).show();
	}
	
	@Override
	public void onIdle()
	{
		
	}
	
	@Override
	public void onDisconnect()
	{
		Log.d( TAG, "Connection lost." );
		
		// TODO: Make me only appear every so often
		// Toast.makeText( LaunchActivity.getAppContext(), "TCP Connection was Lost! I just don't know went wrong!!! :(", Toast.LENGTH_SHORT ).show();
	}
	
	@Override
	public boolean onReceived( Packet var1 )
	{
		if ( var1 instanceof CommandPacket )
		{
			CommandPacket packet = ( (CommandPacket) var1 );
			
			String cmd = packet.getKeyword().toUpperCase();
			
			String payload = "";
			
			// TODO: String Payload Problems?
			if ( packet.getPayload() instanceof String )
				payload = (String) packet.getPayload();
			
			Log.d( TAG, "Got Message: " + cmd + " Payload: " + payload );
			
			if ( cmd.equals( "PONG" ) )
			{
				Log.d( TAG, "Receive a message of good health from the Apple Bloom Rewards Web Socket. :)" );
				SocketService.deviceState = "Received a message of good health from the Apple Bloom Rewards Web Socket. :)";
				
				if ( SocketService.lastPing > 0 )
					SocketService.lastLatency = System.currentTimeMillis() - SocketService.lastPing;
			}
			else if ( cmd.equals( "INVD" ) )
			{
				// The DeviceId I asked to use if not permitted. Get a new one.
				SocketService.deviceUUID = null;
				SocketService.registered = false;
			}
			else if ( cmd.equals( "LOCI" ) )
			{
				SocketService.setLocation( payload );
			}
			else if ( cmd.equals( "OPT" ) )
			{
				SocketService.handleOption( payload );
			}
			else if ( cmd.equals( "INFO" ) )
			{
				// The server requested that I send my device information.
				JSONObject jos = null;
				
				try
				{
					// serial, model, appVersion, state
					jos = new JSONObject( "{ 'appVersion': '" + LaunchActivity.appVersion + "', 'state': '" + SocketService.deviceState + "' }" );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				finally
				{
					sendTCP( new CommandPacket( "UPDT", jos.toString() ) );
				}
			}
			else if ( cmd.equals( "NOID" ) )
			{
				if ( SocketService.deviceUUID == null || SocketService.deviceUUID == "" )
				{
					// We have no UUID stored in our preferences. Have the server assign us one.
					String seed = Settings.Secure.getString( LaunchActivity.getInstance().getContentResolver(), "android_id" );
					
					if ( seed == null )
						seed = UUID.randomUUID().toString();
					
					sendTCP( new CommandPacket( "BOOT", seed ) );
				}
				else
				{
					// Say hello to the nice Web Socket Server.
					sendTCP( new CommandPacket( "HELO", SocketService.deviceUUID ) );
				}
			}
			else if ( cmd.equals( "UPDT" ) )
			{
				// There is new package update available. Automaticly install it.
			}
			else if ( cmd.equals( "REDM" ) )
			{
				// These are new redeemables that need adding to our database.
				SocketService.applyRedeemables( payload, true );
			}
			else if ( cmd.equals( "UTUB" ) )
			{
				// These are new youtube videos that need adding to our database.
				SocketService.appleYoutube( payload );
			}
			else if ( cmd.equals( "ACCT" ) )
			{
				// This is usually a response to the request to download all accounts from the Apple Bloom Servers.
				SocketService.syncAccounts( payload );
			}
			else if ( cmd.equals( "PREF" ) )
			{
				// Open Preferences
			}
			else if ( cmd.equals( "REBO" ) )
			{
				// Reboot the device.
				sendTCP( new CommandPacket( "INFO", "The device is now attempting to restart." ) );
				CommonUtils.restartDevice();
			}
			else if ( cmd.equals( "NOLO" ) )
			{
				// There is location registered with this device.
				SocketService.deviceState = "This device is not registered to any locations.";
			}
			else if ( cmd.equals( "UUID" ) )
			{
				// The server has assigned my new deviceId. Save.
				SocketService.registered = true;
				SocketService.deviceUUID = payload;
				
				SharedPreferences.Editor editor = SocketService.sharedPrefs.edit();
				
				editor.putString( "uuid", payload );
				
				editor.commit();
				
				SocketService.requestRedeemables( false );
				
				LaunchActivity.startPushLink( payload );
				
				Log.d( TAG, "Setting device UUID to " + payload );
			}
			
			return true;
		}
		
		return false;
	}
}
