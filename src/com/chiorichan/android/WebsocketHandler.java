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

package com.chiorichan.android;

import co.applebloom.apps.rewards.LaunchActivity;
import co.applebloom.apps.rewards.SystemTimerListener;
import android.util.Log;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class WebsocketHandler
{
	// Production URL
	// private static final String SERVER_URL = "ws://applebloom.co:8080/websocket";
	
	// Sandbox URL
	private static final String SERVER_URL = "ws://10.0.1.120:8080/websocket";
	
	private static final String TAG = "ABRewards";
	private final WebSocketConnection mConnection = new WebSocketConnection();
	private final WebsocketExtendedHandler weh = new WebsocketExtendedHandler();
	
	public Boolean isConnected = false;
	
	public Boolean preCheck()
	{
		if ( !isConnected )
			makeConnection();
		else
			return true;
		
		int timeout = 10000; // Ten Second Timeout
		
		while ( !isConnected && timeout > 0 )
		{
			try
			{
				Thread.sleep( 1 );
			} catch ( InterruptedException e ) {}
			timeout--;
		}
		
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
	
	public WebsocketHandler()
	{
		makeConnection();
		
		LaunchActivity.myHeart.addListener(	new SystemTimerListener() {
			@Override
			public void onSystemTimeSignal ()
			{
				if ( preCheck() )
				{
					send( "PING" );
				}
			}
		});
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
		Log.d( TAG, "We attempted to send the text \"" + text + "\" to the Web Socket." );
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
		}
		
		@Override
		public void onTextMessage( String payload )
		{
			Log.d( TAG, "Got Message: " + payload );
			
			// Receive server online message
			if ( payload.equalsIgnoreCase( "PONG" ) )
			{
				Log.d( TAG, "Receive a message of good health from the Apple Bloom Rewards Web Socket. :)" );
			}
		}
		
		@Override
		public void onClose( int code, String reason )
		{
			Log.d( TAG, "Connection lost." );
			isConnected = false;
		}
	}
}
