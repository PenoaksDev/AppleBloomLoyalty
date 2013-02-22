/*
 * Copyright (c) 2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package co.applebloom.api;

import java.util.Random;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import co.applebloom.apps.rewards.CommonUtils;
import co.applebloom.apps.rewards.LaunchActivity;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class WebsocketHandler
{
	private final int MAX_ATTEMPTS = 5;
	private final int BACKOFF_MILLI_SECONDS = 2000;
	private final Random random = new Random();
	
	// Production URL
	private static final String SERVER_URL = "ws://applebloom.co:8080/websocket";
	
	// Sandbox URL
	// private static final String SERVER_URL = "ws://10.0.1.120:8080/websocket";
	
	public static Long lastData = 0L;
	private static final String TAG = "ABRewards";
	private final WebSocketConnection mConnection = new WebSocketConnection();
	private final WebsocketExtendedHandler weh = new WebsocketExtendedHandler();
	
	public Boolean isConnected = false;
	
	public Boolean preCheck()
	{
		if ( isConnected )
			return true;
		else
			makeConnection();
		
		/*
		// TODO: Wait 10 seconds for the connection to complete.
		Long curTime = System.currentTimeMillis();
		Long endTime = System.currentTimeMillis() + 2000; // Ten Second Timeout
		
		while ( !isConnected && curTime < endTime )
		{
			Thread.yield();
			curTime = System.currentTimeMillis();
		}
		*/
		
		if ( isConnected )
		{
			return true;
		}
		else
		{
			Log.e( TAG, "It seems our attempt to connect with the Apple Bloom Rewards Web Socket has timed out. Try again later." );
			return false;
		}
	}
	
	public void makeConnection()
	{
		try
		{
			mConnection.connect( SERVER_URL, weh);
		}
		catch ( WebSocketException e )
		{
			Log.d( TAG, e.toString() );
		}
	}
	
	public void send( String text )
	{
		Log.d( TAG, "We attempted to send \"" + text + "\" to the Web Socket." );
		
		if ( mConnection != null && mConnection.isConnected() )
			mConnection.sendTextMessage( text );
	}
	
	class WebsocketExtendedHandler extends WebSocketHandler
	{
		@Override
		public void onOpen()
		{
			Log.d( TAG, "Status: Connected to " + SERVER_URL );
			//mConnection.sendTextMessage( "ECHO " );
			isConnected = true;
			
			WebSocketService.register( true );
			
			Toast.makeText( LaunchActivity.getAppContext(), "Successfully Make a Websocket Connection! :D", Toast.LENGTH_SHORT ).show();
		}
		
		@Override
		public void onTextMessage( String payload )
		{
			String arr[] = payload.split(" ", 2);
			String cmd = arr[0].toUpperCase();
			payload = ( arr.length > 1 ) ? arr[1].trim() : "";
			
			Log.d( TAG, "Got Message: " + cmd + " Payload: " + payload );
			
			if ( cmd.equals( "PONG" ) )
			{
				Log.d( TAG, "Receive a message of good health from the Apple Bloom Rewards Web Socket. :)" );
				WebSocketService.deviceState = "Received a message of good health from the Apple Bloom Rewards Web Socket. :)";
			}
			else if ( cmd.equals( "INVD" ) )
			{
				// The DeviceId I asked to use if not permitted. Get a new one.
				WebSocketService.deviceUUID = null;
				WebSocketService.registered = false;
			}
			else if ( cmd.equals( "LOCI" ) )
			{
				WebSocketService.setLocation( payload );
			}
			else if ( cmd.equals( "INFO" ) )
			{
				// The server requested that I send my device information.
				JSONObject jos = null;
				
				try
				{
					// serial, model, appVersion, state
					jos = new JSONObject( "{ 'appVersion': '" + LaunchActivity.appVersion + "', 'state': '" + WebSocketService.deviceState + "' }" );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				finally
				{
					send( "UPDT " + jos.toString() );
				}
			}
			else if ( cmd.equals( "NOID" ) )
			{
				if ( WebSocketService.deviceUUID == null || WebSocketService.deviceUUID == "" )
				{
					// We have no UUID stored in our preferences. Have the server assign us one.
					String seed = Settings.Secure.getString( LaunchActivity.getInstance().getContentResolver(), "android_id" );
					
					if ( seed == null )
						seed = UUID.randomUUID().toString();
					
					send( "BOOT " + seed );
				}
				else
				{
					// Say hello to the nice Web Socket Server.
					send( "HELO " + WebSocketService.deviceUUID );
				}
			}
			else if ( cmd.equals( "UPDT" ) )
			{
				// There is new package update available. Automaticly install it.
			}
			else if ( cmd.equals( "REDM" ) )
			{
				// These are new redeemables that need adding to our database.
				WebSocketService.applyRedeemables( payload, true );
			}
			else if ( cmd.equals( "ACCT" ) )
			{
				// This is usually a response to the request to download all accounts from the Apple Bloom Servers.
				WebSocketService.syncAccounts( payload );
			}
			else if ( cmd.equals( "REBO" ) )
			{
				// Reboot the device.
				send( "INFO The device is now attempting to restart." );
				CommonUtils.restartDevice();
			}
			else if ( cmd.equals( "NOLO" ) )
			{
				// There is location registered with this device.
				WebSocketService.deviceState = "This device is not registered to any locations.";
			}
			else if ( cmd.equals( "UUID" ) )
			{
				// The server has assigned my new deviceId. Save.
				WebSocketService.registered = true;
				WebSocketService.deviceUUID = payload;
				
				SharedPreferences.Editor editor = WebSocketService.sharedPrefs.edit();
				
				editor.putString( "uuid", payload );
				
				editor.commit();
				
				WebSocketService.requestRedeemables( false );
				
				LaunchActivity.startPushLink( payload );
				
				Log.d( TAG, "Setting device UUID to " + payload );
			}
			
			lastData = System.currentTimeMillis();
		}
		
		@Override
		public void onClose( int code, String reason )
		{
			Log.d( TAG, "Connection lost." );
			isConnected = false;
			
			Toast.makeText( LaunchActivity.getAppContext(), "Websocket Connection was Lost! :(", Toast.LENGTH_SHORT ).show();
		}
	}
}
