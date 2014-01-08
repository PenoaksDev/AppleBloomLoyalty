package com.chiorichan.apps.rewards;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.chiorichan.net.ConnectionHandler;
import com.chiorichan.net.Packet;
import com.chiorichan.net.TcpClient;
import com.chiorichan.net.packet.CommandPacket;
import com.chiorichan.net.packet.PingPacket;
import com.chiorichan.util.Common;

public class NetworkHandler extends AsyncTask<Void, String, Void>
{
	public static final String TAG = "ChioriTCPHandler";
	
	private static final String SERVER_URL = "direct.applebloom.co";
	private static final int SERVER_PORT = 1024;
	@SuppressLint( "UseSparseArrays" )
	private static Map<Integer, Packet> pendingPackets = new HashMap<Integer, Packet>();
	private TcpClient mConnection;
	private long lastPongReceived = System.currentTimeMillis();
	
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
					mConnection = new TcpClient( InetAddress.getByName( SERVER_URL ), SERVER_PORT, new ConnectionHandler() );
				else if ( !mConnection.isConnected() )
					mConnection.attemptConnection( InetAddress.getByName( SERVER_URL ), SERVER_PORT );
				else
				{
					for ( Entry<Integer, Packet> packet : pendingPackets.entrySet() )
					{
						if ( packet.getKey() < Common.getEpoch() - 60000 )
							Log.w( TAG, "Is the client laggy? There seems to be a major delay between receiving packets and processing them." );
						
						Log.w( TAG, "Receiving Packet " + packet.getKey() + " > " + packet.getValue() );
						
						if ( packet.getValue() instanceof CommandPacket )
						{
							CommandPacket var1 = ((CommandPacket) packet.getValue());
							
							if ( var1.getKeyword().toUpperCase().equals( "PONG" ) )
							{
								lastPongReceived = (Long) var1.getPayload();
							}
						}
					}
					
					pendingPackets.clear();
					
					mConnection.sendPing();
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
			
			// publishProgress( null );
			SystemClock.sleep( 15000 );
		}
		while ( true );
	}
}