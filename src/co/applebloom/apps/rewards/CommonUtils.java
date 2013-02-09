package co.applebloom.apps.rewards;

import static co.applebloom.apps.rewards.CommonUtils.TAG;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.pushlink.android.PushLink;

public class CommonUtils
{
	static final String SERVER_URL = "http://api.applebloom.co/rewards";
	static final String SENDER_ID = "636108761171";
	static final String TAG = "Rewards";
	static final String EXTRA_MESSAGE = "message";
	static final String DISPLAY_MESSAGE_ACTION = "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";
	
	static final HeartBeat heart = new HeartBeat();
	
	static void displayMessage( Context context, String message )
	{
		Intent intent = new Intent( DISPLAY_MESSAGE_ACTION );
		intent.putExtra( EXTRA_MESSAGE, message );
		context.sendBroadcast( intent );
	}
	
	public static String init( Activity it )
	{
		// Register this device with the new Apple Bloom Rewards Server
		GCMRegistrar.checkDevice( it );
		
		String regId = GCMRegistrar.getRegistrationId( it );
		
		if ( regId.equals( "" ) )
		{
			GCMRegistrar.register( it, SENDER_ID );
		}
		else
		{
			Log.v( TAG, "This device is already registered with the Apple Bloom Rewards App Engine! :)" );
		}
		
		// UI Updater
		heart.addListener( new SystemTimerListener() {
			public void onSystemTimeSignal ()
			{
				LaunchActivity.getInstance().updateUI();
			}
		});
		
		// Say Hello
		heart.addListener( new SystemTimerListener() {
			public void onSystemTimeSignal ()
			{
				if ( ServerUtils.notifyServers() )
					destroyListener();
			}
		});
		
		// Send Transactions
		heart.addListener( new SystemTimerListener() {
			public void onSystemTimeSignal ()
			{
				ServerUtils.sendTransactions();
			}
		});
		
		return regId;
	}
}
