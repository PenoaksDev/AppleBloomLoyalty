package co.applebloom.apps.rewards;

import java.util.Random;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;
import android.util.Log;

import co.applebloom.api.WebsocketHandler;

import com.chiorichan.android.JSONObj;
import com.pushlink.android.PushLink;

public final class ServerUtils
{
	static final String TAG = "ABRewards";
	private static ServerUtils instance;
	
	public ServerUtils()
	{
		setInstance( this );
	}
	
	public static ServerUtils getInstance()
	{
		return instance;
	}
	
	private static void setInstance( ServerUtils instance )
	{
		ServerUtils.instance = instance;
	}
	
	public static void sendException( Throwable t )
	{
		PushLink.sendAsyncException( t );
		// Send Exception to Apple Bloom Servers
	}
	
	/*
	 * SQLiteDatabase db = myLittleDB.getWritableDatabase();
	 * 
	 * try { JSONArray log = result.getJSONArray("log");
	 * 
	 * for (int i = 0; i < log.length(); i++) { Log.d(TAG, "Removing " + log.getString(i) +
	 * " from transaction table."); //db.delete("trans", "", null); } } catch (Exception e) { e.printStackTrace();
	 * PushLink.sendAsyncException( e ); }
	 * 
	 * db.close();
	 */
	
	
	public void updateUI()
	{
		
		/*
		 * // Pull information from My Little JSON locID = myLittleJSON.getString( "locID", null ); deviceImg =
		 * myLittleJSON.getString( "img", deviceImg ); deviceTitle = myLittleJSON.getString( "title", "" ); deviceAddress1
		 * = myLittleJSON.getString( "address1", "" ); deviceAddress2 = myLittleJSON.getString( "address2", "" );
		 * 
		 * // This would mean there are new location details. if ( locID != null && !locID.equals( lastLocID2 ) ) {
		 * SharedPreferences.Editor editor = sharedPrefs.edit();
		 * 
		 * editor.putString( "img", deviceImg ); editor.putString( "title", deviceTitle ); editor.putString( "address1",
		 * deviceAddress1 ); editor.putString( "address2", deviceAddress2 ); editor.putString( "locID", locID );
		 * 
		 * editor.commit(); }
		 * 
		 * // Check Integrity of Data if ( locID == null ) { locID = sharedPrefs.getString( "locID", null );
		 * 
		 * deviceTitle = sharedPrefs.getString( "title", "{Internal Application Error}" ); deviceAddress1 =
		 * sharedPrefs.getString( "address1", "" ); deviceAddress2 = sharedPrefs.getString( "address2", "" ); deviceImg =
		 * sharedPrefs.getString( "img", deviceImg ); }
		 * 
		 * lastLocID2 = locID;
		 */
	}
	
	// TODO: Add Battery Monitor. Send a notice when power changes.
	/*
	 * // Register for the battery changed event IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	 * 
	 * / Intent is sticky so using null as receiver works fine // return value contains the status Intent batteryStatus =
	 * this.registerReceiver(null, filter);
	 * 
	 * // Are we charging / charged? int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1); boolean
	 * isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
	 * 
	 * boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;
	 * 
	 * // How are we charging? int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1); boolean
	 * usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB; boolean acCharge = chargePlug ==
	 * BatteryManager.BATTERY_PLUGGED_AC;
	 */
}
