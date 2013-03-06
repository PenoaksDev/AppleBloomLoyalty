package co.applebloom.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.util.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import co.applebloom.apps.rewards.LaunchActivity;

import com.chiorichan.android.JSONObj;
import com.chiorichan.android.MD5Checksum;
import com.chiorichan.android.MyLittleDB;
import com.pushlink.android.PushLink;

public class WebSocketService extends IntentService
{
	public static Boolean registered = false;
	public static String deviceUUID = null;
	private static Context context;
	public static String deviceState = "This device has not been fully initalized!";
	public static SharedPreferences sharedPrefs = null;
	public static Boolean changesMade = true;
	
	public static WebsocketHandler chi = new WebsocketHandler();
	static final String TAG = "ABRewards";
	
	public WebSocketService()
	{
		super( WebSocketService.class.getSimpleName() );
		
		if ( sharedPrefs == null && LaunchActivity.getInstance() != null )
			sharedPrefs = LaunchActivity.getInstance().getSharedPreferences( "AppleBloomRewards", 0 );
	}
	
	@Override
	public void onCreate()
	{
		Toast.makeText( this, "Web Socket Service Started! :D", Toast.LENGTH_SHORT ).show();
		
		context = this;
		
		// FIXME: If activity is not running we get a NPE.
		if ( LaunchActivity.getInstance() == null )
			return;
		
		deviceUUID = sharedPrefs.getString( "uuid", null );
	}
	
	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		Log.d( TAG, "Registered: " + registered + " UUID: " + deviceUUID );
		
		if ( deviceUUID == null )
			registered = false;
		
		if ( chi.preCheck() )
		{
			register();
			chi.send( "PING" );
			
			/*
			// If true then changes have been made from the main activity. i.e. Some earned or redeemed points.
			if ( changesMade )
			{
				syncAccounts();
				changesMade = false;
			}
			*/
			
			sendSyncedMessages();
		}
		
		scheduleNextUpdate();
		
		return Service.START_STICKY;
	}
	
	public void sendSyncedMessages()
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		
		if ( LaunchActivity.myLittleDB == null )
			return;
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getReadableDatabase();
		SQLiteDatabase dbw = LaunchActivity.myLittleDB.getWritableDatabase();
		
		Cursor cursor = null;
		
		try
		{
			cursor = db.query( "pending", null, null, null, null, null, null );
		}
		catch ( SQLiteException e )
		{
			try
			{
				dbw.execSQL("CREATE TABLE pending (id, time, msg, expire);");
			}
			catch ( SQLiteException e1 )
			{}
			
			return;
		}

		if ( cursor.getCount() > 0 )
		{
			cursor.moveToFirst();
			
			do
			{
				if ( send( cursor.getString( 2 ) ) )
					dbw.delete( "pending", "`id` = '" + cursor.getString( 0 ) + "'", null );
			}
			while ( cursor.moveToNext() );
		}
	}
	
	public static Boolean send( String msg )
	{
		if ( !chi.isConnected )
			return false;
			
		chi.send( msg );
		
		return true;
	}
	
	public static void setLocation( String json )
	{
		try
		{
			SharedPreferences.Editor editor = sharedPrefs.edit();
			JSONObject jsn = new JSONObject( json );
			
			editor.putString( "locID", jsn.getString( "locId" ) );
			editor.putString( "img", jsn.getString( "header" ) );
			editor.putString( "title", jsn.getString( "title" ) );
			editor.putString( "address1", jsn.getString( "address1" ) );
			editor.putString( "address2", jsn.getString( "address2" ) );
			
			editor.commit();
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
	}
	
	public String getDeviceUUID()
	{
		return deviceUUID;
	}
	
	@Override
	protected void onHandleIntent( Intent paramIntent )
	{
		
	}
	
	public void sendException( Exception e )
	{
		// TODO: Does this work?
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		e.printStackTrace( pw );
		
		chi.send( "EXCP " + sw.toString() );
	}
	
	private void scheduleNextUpdate()
	{
		Intent intent = new Intent( this, this.getClass() );
		PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
		
		long currentTimeMillis = System.currentTimeMillis();
		long nextUpdateTimeMillis = currentTimeMillis + 5000;
		
		AlarmManager alarmManager = (AlarmManager) getSystemService( Context.ALARM_SERVICE );
		alarmManager.set( AlarmManager.RTC, nextUpdateTimeMillis, pendingIntent );
	}
	
	public static void register()
	{
		register( false );
	}
	
	public static void register( Boolean force )
	{
		if ( !registered || force )
		{
			if ( deviceUUID == null )
				deviceUUID = sharedPrefs.getString( "uuid", null );
			
			if ( deviceUUID == null || deviceUUID == "" )
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
				chi.send( "HELO " + deviceUUID );
			}
		}
	}
	
	/**
	 * Calling this method without argument will cause the Apple Bloom Server to spill a JSON Array of Accounts onto this
	 * device;
	 */
	public static void syncAccounts()
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		
		if ( LaunchActivity.myLittleDB == null )
			return;
		
		chi.send( "UPAC" ); // Force Update Accounts
	}
	
	/**
	 * This method saves a message to the database for redundency to make sure it reaches the server.
	 * 
	 * @param msg
	 * @return success
	 */
	public Boolean sendMessageSync( String msg )
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		
		if ( LaunchActivity.myLittleDB == null )
			return false;
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getWritableDatabase();
		
		ContentValues insert = new ContentValues();
		
		insert.put( "id", MD5Checksum.get( msg ) );
		insert.put( "time", System.currentTimeMillis() );
		insert.put( "msg", msg );
		insert.put( "expire", 0 );
		
		db.insert( "pending", null, insert );
		
		Log.d( TAG, "" );
		
		return true;
	}
	
	/**
	 * This method is called when the response from the Servers is received.
	 * 
	 * @param json
	 */
	public static void syncAccounts( String json )
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		
		if ( LaunchActivity.myLittleDB == null )
			return;
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getWritableDatabase();
		SQLiteDatabase dbr = LaunchActivity.myLittleDB.getReadableDatabase();
		
		try
		{
			JSONObject result = new JSONObject( json );
			
			JSONArray users = result.getJSONArray( "users" );
			
			for ( int i = 0; i < users.length(); i++ )
			{
				JSONObj user = JSONObj.convertObj( users.getJSONObject( i ) );
				
				// Log.d(TAG, "Adding " + user.getString("id") + " (" +
				// user.getLong("balance") + ") to contacts.");
				Cursor cursor = dbr.query( "users", null, "`id` = '" + user.getString( "id" ) + "'", null, null, null, null );
				
				if ( cursor.getCount() < 1 )
				{
					ContentValues insert = new ContentValues();
					
					insert.put( "id", user.getString( "id" ) );
					insert.put( "name", user.getString( "name" ) );
					insert.put( "email", user.getString( "email" ) );
					insert.put( "first_added", user.getLong( "first_added", System.currentTimeMillis() ) );
					insert.put( "balance", user.getInt( "balance" ) );
					insert.put( "last_instore_check", user.getLong( "last_instore_check", 0L ) );
					
					db.insert( "users", null, insert );
				}
				else
				{
					ContentValues update = new ContentValues();
					update.put( "balance", user.getInt( "balance" ) );
					
					if ( !user.getString( "name" ).endsWith( "" ) )
						update.put( "name", user.getString( "name" ) );
					
					if ( !user.getString( "email" ).endsWith( "" ) )
						update.put( "email", user.getString( "email" ) );
					
					update.put( "last_instore_check", user.getLong( "last_instore_check" ) );
					
					cursor.moveToFirst();
					
					// Should we overwrite the local copy??? - Is the server copy newer?
					if ( user.getLong( "last_instore_check" ) > cursor.getLong( 5 ) )
					{
						db.update( "users", update, "`id` = '" + user.getString( "id" ) + "'", null );
					}
				}
			}
			
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			// sendException( e );
		}
		
		db.close();
		dbr.close();
		
		dbr = LaunchActivity.myLittleDB.getReadableDatabase();
		
		Cursor c = dbr.query( "users", null, null, null, null, null, null );
		ArrayList<String> lst = new ArrayList<String>();
		
		if ( c.getCount() > 0 )
		{
			c.moveToFirst();
			
			do
			{
				lst.add( "{'id': '" + c.getString( 0 ) + "', 'name': '" + c.getString( 1 ) + "', 'email': '" + c.getString( 2 ) + "', 'first_added': '" + c.getString( 3 ) + "', 'balance': '" + c.getString( 4 ) + "', 'last_instore_check': '" + c.getString( 5 ) + "'}" );
			}
			while ( c.moveToNext() );
			
			String jsonr = "{'users':[";
			String sep = "";
			
			for ( String s : lst )
			{
				jsonr += sep + s;
				sep = ",";
			}
			
			jsonr += "]}";
			
			try
			{
				JSONObject json1 = new JSONObject( jsonr );
				
				System.out.println( json1.toString() );
				
				chi.send( "ACCT " + json1.toString() );
			}
			catch ( JSONException e )
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void applyRedeemables( String json, Boolean clearFirst )
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		
		if ( LaunchActivity.myLittleDB == null )
			return;
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getWritableDatabase();
		
		if ( clearFirst )
		{
			db.execSQL( "DELETE FROM `redeemables`;" );
			db.execSQL( "VACUUM;" );
		}
		
		try
		{
			JSONObject result = new JSONObject( json );
			JSONArray users = result.getJSONArray( "list" );
			
			for ( int i = 0; i < users.length(); i++ )
			{
				JSONObj user = JSONObj.convertObj( users.getJSONObject( i ) );
				
				ContentValues insert = new ContentValues();
				
				insert.put( "id", user.getString( "id" ) );
				insert.put( "title", user.getString( "title" ) );
				insert.put( "cost", user.getLong( "cost" ) );
				
				db.insert( "redeemables", null, insert );
				
				Log.d( TAG, "Inserting " + user.toString() );
			}
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
		
		db.close();
	}
	
	public static void requestRedeemables( boolean force )
	{
		if ( LaunchActivity.myLittleDB == null )
			LaunchActivity.myLittleDB = new MyLittleDB( context );
		;
		
		if ( LaunchActivity.myLittleDB == null )
			return;
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getReadableDatabase();
		Cursor cursor = db.query( "redeemables", null, null, null, null, null, null );
		
		if ( cursor.getCount() < 1 || force )
		{
			chi.send( "UPRE" ); // Update Redeemables
		}
	}
	
	public static boolean sendTransactions()
	{
		SQLiteDatabase db = LaunchActivity.myLittleDB.getReadableDatabase();
		Cursor cursor = db.query( "trans", null, null, null, null, null, null );
		
		if ( cursor.getCount() < 1 )
			return false;
		
		JSONArray trans = new JSONArray();
		JSONObj j = JSONObj.emptyObj();
		
		for ( cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext() )
		{
			if ( trans.toString().length() > 8000 )
			{
				chi.send( "SYNC " + trans.toString() );
				trans = new JSONArray();
			}
			
			String id = cursor.getString( 0 );
			Long time = cursor.getLong( 1 );
			int n = cursor.getInt( 2 );
			int p = cursor.getInt( 3 );
			String action = cursor.getString( 4 );
			String comment = cursor.getString( 5 );
			
			try
			{
				j = JSONObj.emptyObj();
				
				Cursor cursor1 = db.query( "users", null, "`id` = '" + id + "'", null, null, null, null );
				cursor1.moveToFirst();
				
				j.put( "id", id );
				j.put( "time", time );
				j.put( "n", n );
				j.put( "p", p );
				j.put( "action", action );
				j.put( "comment", comment );
				
				j.put( "email", cursor1.getString( 2 ) );
				j.put( "first_added", cursor1.getString( 3 ) );
				
				trans.put( j );
				
			}
			catch ( JSONException e )
			{
				e.printStackTrace();
			}
		}
		
		chi.send( "SYNC " + trans.toString() );
		
		db.close();
		
		db = LaunchActivity.myLittleDB.getWritableDatabase();
		db.execSQL( "DELETE FROM `trans`;" );
		db.execSQL( "VACUUM;" );
		db.close();
		
		return true;
	}
	
	private final IBinder mBinder = new MyBinder();
	
	@Override
	public IBinder onBind( Intent arg0 )
	{
		return mBinder;
	}
	
	public class MyBinder extends Binder
	{
		public WebSocketService getService()
		{
			return WebSocketService.this;
		}
	}
}
