package co.applebloom.apps.rewards;

import static co.applebloom.apps.rewards.CommonUtilities.SENDER_ID;
import static co.applebloom.apps.rewards.CommonUtilities.TAG;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService
{
	public GCMIntentService()
	{
		super( SENDER_ID );
	}
	
	@Override
	protected void onMessage( Context context, Intent paramIntent )
	{
		Log.i( TAG, "Received message" );
		String message = getString( R.string.gcm_message );
		CommonUtilities.displayMessage( context, message );
		// notifies user
		generateNotification( context, message );
	}
	
	@Override
	protected void onError( Context context, String errorId )
	{
		CommonUtilities.displayMessage( context, getString( R.string.gcm_error, errorId ) );
	}
	
	@Override
	protected void onRegistered( Context context, String registrationId )
	{
		Log.i( TAG, "Device registered: regId = " + registrationId );
		CommonUtilities.displayMessage( context, getString( R.string.gcm_registered ) );
		ServerUtilities.register( context, registrationId );
	}
	
	@Override
	protected void onUnregistered( Context context, String registrationId )
	{
		Log.i( TAG, "Device unregistered" );
		CommonUtilities.displayMessage( context, getString( R.string.gcm_unregistered ) );
		if ( GCMRegistrar.isRegisteredOnServer( context ) )
		{
			ServerUtilities.unregister( context, registrationId );
		}
		else
		{
			// This callback results from the call to unregister made on
			// ServerUtilities when the registration to the server failed.
			Log.i( TAG, "Ignoring unregister callback" );
		}
	}
	
	private static void generateNotification( Context context, String message )
	{
		int icon = R.drawable.ic_stat_gcm;
		long when = System.currentTimeMillis();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
		Notification notification = new Notification( icon, message, when );
		String title = context.getString( R.string.app_name );
		Intent notificationIntent = new Intent( context, LaunchActivity.class );
		// set intent so it does not start a new activity
		notificationIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		PendingIntent intent = PendingIntent.getActivity( context, 0, notificationIntent, 0 );
		notification.setLatestEventInfo( context, title, message, intent );
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify( 0, notification );
	}
}
