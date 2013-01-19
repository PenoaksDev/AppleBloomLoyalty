package co.applebloom.apps.rewards;

import android.content.Context;
import android.content.Intent;

public class CommonUtilities
{
	static final String SERVER_URL = "http://rewards.chiorichan.com";
	static final String SENDER_ID = "636108761171";
	static final String TAG = "Rewards";
	static final String EXTRA_MESSAGE = "message";
   static final String DISPLAY_MESSAGE_ACTION = "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";
	
	static void displayMessage( Context context, String message )
	{
		Intent intent = new Intent( DISPLAY_MESSAGE_ACTION );
		intent.putExtra( EXTRA_MESSAGE, message );
		context.sendBroadcast( intent );
	}
}
