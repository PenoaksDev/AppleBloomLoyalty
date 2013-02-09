package co.applebloom.apps.rewards;

import static co.applebloom.apps.rewards.CommonUtils.SERVER_URL;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.chiorichan.android.HTTPParser;
import com.chiorichan.android.WebsocketHandler;
import com.google.android.gcm.GCMRegistrar;
import com.pushlink.android.PushLink;

public final class ServerUtils extends Thread
{
	private static final int MAX_ATTEMPTS = 5;
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	private static final Random random = new Random();
	public static SharedPreferences sharedPrefs = null;
	static final String TAG = "ABRewards";
	private static ServerUtils instance;
	
	public static Boolean registered = true;
	public static String DeviceUUID = null;
	public static WebsocketHandler chi = new WebsocketHandler();
	
	@Override
	public void run()
	{
		setInstance( this );
		sharedPrefs = LaunchActivity.getInstance().getSharedPreferences( "AppleBloomRewards", 0 );
		DeviceUUID = sharedPrefs.getString( "uuid", null );
		
		LaunchActivity.myHeart.addListener( new SystemTimerListener()
		{
			@Override
			public void onSystemTimeSignal()
			{
				if ( !registered && chi.preCheck() )
				{
					if ( DeviceUUID == null )
					{
						// We have no UUID stored in our preferences. Have the server assign us one.
						String seed = Settings.Secure.getString( LaunchActivity.getInstance().getContentResolver(), "android_id" );
						if ( seed == null )
							seed = UUID.randomUUID().toString();
						
						chi.send( "BOOT " + seed );
					}
					else
					{
						// Say hello to the nice Web Socket Server.
						chi.send( "HELO " + DeviceUUID );
					}
				}
			}
		} );
	}
	
	public static ServerUtils getInstance()
	{
		return instance;
	}
	
	private static void setInstance( ServerUtils instance )
	{
		ServerUtils.instance = instance;
	}
	
	/*
	 * public static class heartBeat extends AsyncTask<Void, JSONObj, Void> {
	 * 
	 * @Override protected void onProgressUpdate( JSONObj... values ) {
	 * 
	 * }
	 * 
	 * @Override protected Void doInBackground( Void... noparams ) { while ( true ) {
	 * 
	 * 
	 * 
	 * JSONObj json = null; Boolean networkActive = haveNetworkConnection(); Boolean force_resync = false;
	 * 
	 * if ( uuid == null || uuid.isEmpty() ) { // SharedPreferences sharedPrefs = //
	 * instance.getSharedPreferences("AppleBloomRewards", 0); String uuid_ = sharedPrefs.getString( "uuid", null );
	 * 
	 * if ( uuid_ == null ) { String seed = Settings.Secure.getString( getInstance().getContentResolver(), "android_id"
	 * );
	 * 
	 * if ( seed == null ) seed = UUID.randomUUID().toString();
	 * 
	 * Log.v( TAG, "UUID SEED: " + seed );
	 * 
	 * try { if ( networkActive ) { uuid_ = HTTPParser.getFromUrl( res.getString( R.string.firstRunUrl ) + "?seed=" +
	 * seed );
	 * 
	 * SharedPreferences.Editor editor = sharedPrefs.edit(); editor.putString( "uuid", uuid_ ); editor.commit(); } else {
	 * uuid_ = null; } } catch ( Exception e ) { e.printStackTrace(); PushLink.sendAsyncException( e ); } }
	 * 
	 * if ( uuid_ != null && !uuid_.isEmpty() ) { Log.v( TAG, "Device UUID: " + uuid_ ); PushLink.addMetadata(
	 * "Device UUID", uuid_ ); uuid = uuid_; } }
	 * 
	 * if ( networkActive ) { if ( uuid == null || uuid.isEmpty() ) { json = JSONObj.emptyObj();
	 * 
	 * /* if ( homeActivity.instance != null ) { homeActivity.instance.setResult(RESULT_CANCELED);
	 * homeActivity.instance.finish(); }
	 * 
	 * 
	 * deviceState = "There seems to be a problem with the Devices UUID."; } else { json = JSONObj.getFromUrlSafe(
	 * res.getString( R.string.pingerUrl ) + "?state=" + deviceState + "&appVersion=" + appVersion );
	 * 
	 * if ( json.getBooleanSafe( "success" ) ) { Log.v( TAG, "Successfully informed Apple Bloom Servers of our status."
	 * ); } else { Log.e( TAG,
	 * "Apple Bloom Servers have informed us that there was an error processing our request. See Logs." ); }
	 * 
	 * if ( json.getStringSafe( "command" ).toUpperCase().equals( "ADMIN" ) ) { Intent intent = new Intent(
	 * android.provider.Settings.ACTION_SETTINGS ); intent.setFlags( FLAG_ACTIVITY_NEW_TASK );
	 * getAppContext().startActivity( intent ); } else if ( json.getStringSafe( "command" ).toUpperCase().equals(
	 * "RESYNC" ) ) { force_resync = true; } else if ( json.getStringSafe( "command" ).toUpperCase().equals( "REBOOT" ) )
	 * { Log.v( TAG, "Apple Bloom Servers told us to Restart." ); restartDevice(); }
	 * 
	 * if ( json.getBooleanSafe( "response" ) ) { if ( json.getBooleanSafe( "locationAssigned" ) ) { deviceState =
	 * "All systems are operating within normal parameters.";
	 * 
	 * if ( !lastLocID.equals( json.getStringSafe( "locID" ) ) ) { // TODO: REMOVE? - Closed waiting screen is all was //
	 * good. /* if ( homeActivity.instance != null ) { homeActivity.instance.setResult(RESULT_CANCELED);
	 * homeActivity.instance.finish(); }
	 * 
	 * 
	 * lastLocID = json.getStringSafe( "locID" ); } } else { deviceState =
	 * "This device has not been assigned to any locations."; } }
	 * 
	 * if ( LaunchActivity.syncDB ) if ( LaunchActivity.sendTransactions() ) LaunchActivity.downloadAccounts();
	 * 
	 * if ( last_redeemable_download < System.currentTimeMillis() - 86400000 ) { downloadRedeemables();
	 * last_redeemable_download = System.currentTimeMillis(); }
	 * 
	 * if ( force_resync ) { LaunchActivity.sendTransactions(); LaunchActivity.downloadAccounts(); downloadRedeemables();
	 * last_redeemable_download = System.currentTimeMillis(); } }
	 * 
	 * publishProgress( json ); } else { publishProgress( JSONObj.emptyObj() ); deviceState =
	 * "It seems the internet connection is down."; Log.w( TAG,
	 * "It seems the internet connection is down and we may be unable to keep sync with the servers." ); }
	 * 
	 * SystemClock.sleep( 15000 ); } } }
	 */
	
	public static void sendException( Throwable e )
	{
		PushLink.sendAsyncException( e );
		// Send Exception to Apple Bloom Servers
	}
	
	static boolean notifyServers()
	{
		// Ask The Servers To Send Me Device Information via Google Cloud Messaging.
		
		try
		{
			HTTPParser.getFromUrl( "http://api.applebloom.co/rewards/notify.text" );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	static boolean sendTransactions()
	{
		// Send most recent transactions to Servers
		
		return false;
	}
	
	static boolean register( final Context context, final String regId )
	{
		Log.i( TAG, "registering device (regId = " + regId + ")" );
		String serverUrl = SERVER_URL + "/register.text";
		Map<String, String> params = new HashMap<String, String>();
		params.put( "regId", regId );
		params.put( "appVersion", LaunchActivity.appVersion );
		long backoff = BACKOFF_MILLI_SECONDS + random.nextInt( 1000 );
		
		for ( int i = 1; i <= MAX_ATTEMPTS; i++ )
		{
			Log.d( TAG, "Attempt #" + i + " to register" );
			try
			{
				// CommonUtilities.displayMessage( context, context.getString( R.string.server_registering, i, MAX_ATTEMPTS
				// ) );
				post( serverUrl, params );
				GCMRegistrar.setRegisteredOnServer( context, true );
				String message = context.getString( R.string.server_registered );
				CommonUtils.displayMessage( context, message );
				return true;
			}
			catch ( IOException e )
			{
				// Here we are simplifying and retrying on any error; in a real
				// application, it should retry only on unrecoverable errors
				// (like HTTP error code 503).
				Log.e( TAG, "Failed to register on attempt " + i, e );
				if ( i == MAX_ATTEMPTS )
				{
					break;
				}
				try
				{
					Log.d( TAG, "Sleeping for " + backoff + " ms before retry" );
					Thread.sleep( backoff );
				}
				catch ( InterruptedException e1 )
				{
					Log.d( TAG, "Thread interrupted: abort remaining retries!" );
					Thread.currentThread().interrupt();
					return false;
				}
				backoff *= 2;
			}
		}
		String message = context.getString( R.string.server_register_error, MAX_ATTEMPTS );
		CommonUtils.displayMessage( context, message );
		return false;
	}
	
	static void unregister( final Context context, final String regId )
	{
		Log.i( TAG, "unregistering device (regId = " + regId + ")" );
		String serverUrl = SERVER_URL + "/unregister.text";
		Map<String, String> params = new HashMap<String, String>();
		params.put( "regId", regId );
		try
		{
			post( serverUrl, params );
			GCMRegistrar.setRegisteredOnServer( context, false );
			String message = context.getString( R.string.server_unregistered );
			CommonUtils.displayMessage( context, message );
		}
		catch ( IOException e )
		{
			// At this point the device is unregistered from GCM, but still
			// registered in the server.
			// We could try to unregister again, but it is not necessary:
			// if the server tries to send a message to the device, it will get
			// a "NotRegistered" error message and should unregister the device.
			String message = context.getString( R.string.server_unregister_error, e.getMessage() );
			CommonUtils.displayMessage( context, message );
		}
	}
	
	private static void post( String endpoint, Map<String, String> params ) throws IOException
	{
		URL url;
		try
		{
			url = new URL( endpoint );
		}
		catch ( MalformedURLException e )
		{
			throw new IllegalArgumentException( "invalid url: " + endpoint );
		}
		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
		// constructs the POST body using the parameters
		while ( iterator.hasNext() )
		{
			Entry<String, String> param = iterator.next();
			bodyBuilder.append( param.getKey() ).append( '=' ).append( param.getValue() );
			if ( iterator.hasNext() )
			{
				bodyBuilder.append( '&' );
			}
		}
		String body = bodyBuilder.toString();
		Log.v( TAG, "Posting '" + body + "' to " + url );
		byte[] bytes = body.getBytes();
		HttpURLConnection conn = null;
		try
		{
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput( true );
			conn.setUseCaches( false );
			conn.setFixedLengthStreamingMode( bytes.length );
			conn.setRequestMethod( "POST" );
			conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded;charset=UTF-8" );
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write( bytes );
			out.close();
			// handle the response
			int status = conn.getResponseCode();
			if ( status != 200 )
			{
				throw new IOException( "Post failed with error code " + status );
			}
		}
		finally
		{
			if ( conn != null )
			{
				conn.disconnect();
			}
		}
	}
}
