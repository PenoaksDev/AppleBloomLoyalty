/*
 * Copyright (c) 2010 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.chiorichan.net;

import static com.chiorichan.net.SocketService.TAG;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import co.applebloom.apps.rewards.LaunchActivity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class SocketHandler
{
	private static final String SERVER_URL = "direct.applebloom.co";
	private static final int SERVER_PORT = 1024;
	
	public static Long lastData = 0L;
	private TcpClient mConnection;
	
	public SocketHandler()
	{
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy( policy );
	}
	
	public Boolean connectionCheck() throws UnknownHostException, IOException
	{
		
		if ( mConnection == null )
			mConnection = new TcpClient( InetAddress.getByName( SERVER_URL ), SERVER_PORT, new ConnectionHandler() );
		
		if ( mConnection != null && mConnection.isConnected() )
			return true;
		
		makeConnection();
		
		/*
		 * // TODO: Wait 10 seconds for the connection to complete.
		 * Long curTime = System.currentTimeMillis();
		 * Long endTime = System.currentTimeMillis() + 2000; // Ten Second Timeout
		 * while ( !isConnected && curTime < endTime )
		 * {
		 * Thread.yield();
		 * curTime = System.currentTimeMillis();
		 * }
		 */
		
		if ( mConnection.isConnected() )
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
			if ( mConnection.isConnected() )
				mConnection.disconnect();
			
			mConnection.attemptConnection( InetAddress.getByName( SERVER_URL ), SERVER_PORT );
		}
		catch ( UnknownHostException e )
		{
			Log.d( TAG, e.toString() );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public void send( Packet var1 )
	{
		Log.d( TAG, "We attempted to send \"" + var1 + "\" to the Remote Server." );
		
		if ( mConnection != null && mConnection.isConnected() && var1 != null )
			mConnection.sendPacket( var1 );
	}
	
	public boolean isConnected()
	{
		return mConnection.isConnected();
	}
}
