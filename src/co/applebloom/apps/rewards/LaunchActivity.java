package co.applebloom.apps.rewards;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.chiorichan.android.HTTPParser;
import com.chiorichan.android.JSONObj;
import com.chiorichan.android.MD5Checksum;
import com.chiorichan.android.MyLittleDB;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.pushlink.android.PushLink;

public class LaunchActivity extends Activity implements OnClickListener, OnLongClickListener
{
	public CookieStore cookieStore = new BasicCookieStore();
	public HttpContext localContext = new BasicHttpContext();
	
	public static String appVersion = "2.4.0110 (Derpy Hooves)";
	private final static String TAG = "Rewards";	
	private static Context context;
	private static Resources res;
	private static LaunchActivity instance;
	public static String uuid = null;
	public static final int REQUEST_SCAN = 10000006;
	
	public static MyLittleDB myLittleDB = null;
	public static JSONObj myLittleJSON = null;
	
	private TextView phone;
	private Button num0, num1, num2, num3, num4, num5, num6, num7, num8, num9, go;
	private ImageButton scan, back;
    private static TextView titlev;
    private static TextView address;
    
    public static String deviceImg = "http://images.applebloom.co/dunkin.png";
	public static String deviceTitle = "";
	public static String deviceAddress1 = "";
	public static String deviceAddress2 = "";
	private static String deviceState = "{App Error}";
	public static String lastLocID = "";
	public static String lastLocID2 = "";
	public static SharedPreferences sharedPrefs = null;
	
	private static boolean syncDB = true;
	
    private boolean continueAllowed = false;
    
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		res = this.getResources();
		context = this.getApplicationContext();
		setInstance(this);
        super.onCreate(savedInstanceState);
        
        myLittleDB = new MyLittleDB(context);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		
		sharedPrefs = getInstance().getSharedPreferences("AppleBloomRewards", 0);
		
        setContentView(R.layout.home);
        
        /*
        PushLink.start(this, 0, "3vcnlaneunf3k0k0");
        PushLink.setCurrentStrategy(StrategyEnum.FRIENDLY_POPUP);
        
        PushLink.addMetadata("App Version", appVersion);
        PushLink.addMetadata("Device UUID", uuid);
        PushLink.addMetadata("Android Version", Build.VERSION.RELEASE);
        
        FriendlyPopUpStrategy fps = (FriendlyPopUpStrategy) PushLink.getCurrentStrategy();
        fps.setPopUpMessage("New critical bugfixes are now available. Please update.");
        fps.setNotNowButton("Later");
        fps.setUpdateButton("Update Now");
        fps.setReminderTimeInSeconds(60 * 15);
        */
        
        new ScreenReceiver();
        new startMonitor().execute();
        
        TextView version = (TextView) findViewById( R.id.version );
    	version.setText( "Apple Bloom Rewards Version " +  LaunchActivity.appVersion );
    	
    	TextView uuid = (TextView) findViewById( R.id.uuid );
    	uuid.setText( "Device UUID: " + uuid );
    	
    	address = (TextView) findViewById(R.id.address);
        titlev = (TextView) findViewById(R.id.title);
        
        scan = (ImageButton) findViewById(R.id.button_scan);
        back = (ImageButton) findViewById(R.id.button_back);
        
        phone = (TextView) findViewById(R.id.phone);
        num0 = (Button) findViewById(R.id.button0);
        num1 = (Button) findViewById(R.id.button1);
        num2 = (Button) findViewById(R.id.button2);
        num3 = (Button) findViewById(R.id.button3);
        num4 = (Button) findViewById(R.id.button4);
        num5 = (Button) findViewById(R.id.button5);
        num6 = (Button) findViewById(R.id.button6);
        num7 = (Button) findViewById(R.id.button7);
        num8 = (Button) findViewById(R.id.button8);
        num9 = (Button) findViewById(R.id.button9);
        go = (Button) findViewById(R.id.gobutt);
        
        num0.setOnClickListener(this);
        num1.setOnClickListener(this);
        num2.setOnClickListener(this);
        num3.setOnClickListener(this);
        num4.setOnClickListener(this);
        num5.setOnClickListener(this);
        num6.setOnClickListener(this);
        num7.setOnClickListener(this);
        num8.setOnClickListener(this);
        num9.setOnClickListener(this);
        scan.setOnClickListener(this);
        back.setOnClickListener(this);
        back.setOnLongClickListener(this);
        go.setOnClickListener(this);
        phone.setCursorVisible(false);
        
        phone.addTextChangedListener(new TextWatcher()
        {
            public void afterTextChanged(Editable s)
            {
            	testLength();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        
        try
        {
        	PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	appVersion = pInfo.versionName;
        }
        catch ( Exception e )
        {
        	PushLink.sendAsyncException( e );
        }
	}
	
    public void updateUI ()
    {
    	TextView uuid_tv = (TextView) findViewById( R.id.uuid );
    	uuid_tv.setText( "Device UUID: " + uuid );
    	
    	TextView msg_tv = (TextView) findViewById( R.id.msg );
    	msg_tv.setText( "Device State: " + deviceState );
    	
    	if ( haveNetworkConnection() )
    	{
	    	String locID = myLittleJSON.getString("locID", null);
	    	
	    	if ( locID != null && !locID.equals(lastLocID2) )
	    	{
	    		// Update UI Information
	    		lastLocID2 = locID;
	    		
	    		SharedPreferences.Editor editor = sharedPrefs.edit();

	    		deviceImg = myLittleJSON.getString("img", deviceImg);
	    		deviceTitle = myLittleJSON.getString("title", "");
	    		deviceAddress1 = myLittleJSON.getString("address1", "");
	    		deviceAddress2 = myLittleJSON.getString("address2", "");
	    		
	    		editor.putString("img", deviceImg);
	    		editor.putString("title", deviceTitle);
	    		editor.putString("address1", deviceAddress1);
	    		editor.putString("address2", deviceAddress2);
	    		editor.putString("locID", locID);
	    	
	    		titlev.setText( deviceTitle );
	    		address.setText( deviceAddress1 + ", " + deviceAddress2 );
	    		
	    		editor.commit();
	    	}
	    	
	    	ImageView iv = (ImageView) findViewById( R.id.headerImage );
	    	
	    	if ( iv.getDrawable() == null )
	    		UrlImageViewHelper.setUrlDrawable(iv, deviceImg);
    	}
    	else
    	{
    		String locID = sharedPrefs.getString("locID", null);
    		
    		if ( locID != null && !locID.equals(lastLocID2) )
	    	{
	    		// Get information from shared prefs
	    		lastLocID2 = locID;
	    		
	    		titlev.setText( sharedPrefs.getString("title", "{Internal Application Error}") );
	    		address.setText( sharedPrefs.getString("address1", "") + ", " + sharedPrefs.getString("address2", "") );
	    	}
    	}
    }
	
	public static class startMonitor extends AsyncTask<Void, JSONObj, Void>
	{
		private static final int FLAG_ACTIVITY_NEW_TASK = 268435456;
		private static long last_redeemable_download = 0;

		@Override
        protected void onProgressUpdate(JSONObj... values)
        {
			if ( haveNetworkConnection() )
				myLittleJSON = values[0];

			getInstance().updateUI();
        }
		
		@Override
		protected Void doInBackground(Void... noparams)
		{
			while(true)
			{
				JSONObj json = null;
				Boolean networkActive = haveNetworkConnection();
				Boolean force_resync = false;
				
				if ( uuid == null || uuid.isEmpty() )
				{
					//SharedPreferences sharedPrefs = instance.getSharedPreferences("AppleBloomRewards", 0);
					String uuid_ = sharedPrefs.getString("uuid", null);
					
					if ( uuid_ == null )
					{
						String seed = Settings.Secure.getString(getInstance().getContentResolver(), "android_id");
						
						if ( seed == null )
			    			seed = UUID.randomUUID().toString();
						
						Log.v(TAG, "UUID SEED: " + seed);
						
						try
						{
							if ( networkActive )
							{
								uuid_ = HTTPParser.getFromUrl(res.getString(R.string.firstRunUrl) + "?seed=" + seed);
								
								SharedPreferences.Editor editor = sharedPrefs.edit();
					    		editor.putString("uuid", uuid_);
					    		editor.commit();
							}
							else
							{
								uuid_ = null; 
							}
						}
						catch ( Exception e )
						{
							e.printStackTrace();
			    			PushLink.sendAsyncException(e);
						}
					}
					
					if ( uuid_ != null && !uuid_.isEmpty() )
					{
						Log.v(TAG, "Device UUID: " + uuid_);
						PushLink.addMetadata("Device UUID", uuid_);
						uuid = uuid_;
					}
				}
				
				if ( networkActive )
				{
					if ( uuid == null || uuid.isEmpty() )
					{
						json = JSONObj.emptyObj();
					
						/*
						if ( homeActivity.instance != null )
						{
							homeActivity.instance.setResult(RESULT_CANCELED);
							homeActivity.instance.finish();
						}
						*/
						
						deviceState = "There seems to be a problem with the Devices UUID.";
					}
					else
					{
						json = JSONObj.getFromUrlSafe( res.getString(R.string.pingerUrl) + "?state=" + deviceState + "&appVersion=" + appVersion );
						
						if ( json.getBooleanSafe("success") )
						{
							Log.v(TAG, "Successfully informed Apple Bloom Servers of our status.");
						}
						else
						{
							Log.e(TAG, "Apple Bloom Servers have informed us that there was an error processing our request. See Logs.");
						}
						
						if ( json.getStringSafe("command").toUpperCase().equals("ADMIN") )
						{
							Intent intent = new Intent( android.provider.Settings.ACTION_SETTINGS );
							intent.setFlags( FLAG_ACTIVITY_NEW_TASK );
							getAppContext().startActivity(intent);
						}
						else if ( json.getStringSafe("command").toUpperCase().equals("RESYNC") )
						{
							force_resync = true;
						}
						else if ( json.getStringSafe("command").toUpperCase().equals("REBOOT") )
						{
							Log.v(TAG, "Apple Bloom Servers told us to Restart.");
							restartDevice();
						}
						
						if ( json.getBooleanSafe("response") )
						{
							if ( json.getBooleanSafe("locationAssigned") )
							{
								deviceState = "All systems are operating within normal parameters.";
								
								if ( !lastLocID.equals( json.getStringSafe( "locID" ) ) )
								{
									if ( homeActivity.instance != null )
									{
										homeActivity.instance.setResult(RESULT_CANCELED);
										homeActivity.instance.finish();
									}
									lastLocID = json.getStringSafe( "locID" );
								}
							}
							else
							{
								deviceState = "This device has not been assigned to any locations.";
							}
						}
						
						if ( LaunchActivity.syncDB )
							if (LaunchActivity.sendTransactions())
								LaunchActivity.downloadAccounts();
						
						if ( last_redeemable_download < System.currentTimeMillis() - 86400000 )
						{
							downloadRedeemables();
							last_redeemable_download = System.currentTimeMillis();
						}
						
						if ( force_resync )
						{
							LaunchActivity.sendTransactions();
							LaunchActivity.downloadAccounts();
							downloadRedeemables();
							last_redeemable_download = System.currentTimeMillis();
						}
					}
					
					publishProgress( json );
				}
				else
				{
					publishProgress( JSONObj.emptyObj() );
					deviceState = "It seems the internet connection is down.";
					Log.w(TAG, "It seems the internet connection is down and we may be unable to keep sync with the servers.");
				}
				
				SystemClock.sleep(15000);
			}
		}
	}
	
	public static void downloadRedeemables ()
	{
		JSONObj result = JSONObj.getFromUrlSafe( res.getString( R.string.downloadRedeemablesUrl ) );
		
		SQLiteDatabase db = myLittleDB.getWritableDatabase();
		
		try {
			db.execSQL("DELETE FROM `redeemables`;");
			db.execSQL("VACUUM;");
			
			JSONArray users = result.getJSONArray("list");
			
			for (int i = 0; i < users.length(); i++)
			{
				JSONObj user = JSONObj.convertObj( users.getJSONObject( i ) );
				
				ContentValues insert = new ContentValues();
				
				insert.put("id",  user.getString("redeemID") );
				insert.put("title", user.getString("title") );
				insert.put("cost", user.getLong("cost") );
				
				db.insert("redeemables", null, insert);
				
				Log.d(TAG, "Inserting " + user.toString());
			}
			
		} catch (Exception e) { e.printStackTrace(); PushLink.sendAsyncException( e ); }
		
		db.close();
	}
	
	public static void downloadAccounts ()
	{
		JSONObj result = JSONObj.getFromUrlSafe( res.getString( R.string.downloadUrl ) );
		
		SQLiteDatabase db = myLittleDB.getWritableDatabase();
		SQLiteDatabase dbr = myLittleDB.getReadableDatabase();
		
		try {
			JSONArray users = result.getJSONArray("users");
			
			for (int i = 0; i < users.length(); i++)
			{
				JSONObj user = JSONObj.convertObj( users.getJSONObject( i ) );
				
				//Log.d(TAG, "Adding " + user.getString("id") + " (" + user.getLong("balance") + ") to contacts.");
				Cursor cursor = dbr.query("users", null, "`id` = '" + user.getString("id") + "'", null, null, null, null);
				
				if ( cursor.getCount() < 1 )
				{
					ContentValues insert = new ContentValues();
					
					insert.put("id",  user.getString("id") );
					insert.put("name", user.getString("name") );
					insert.put("email", user.getString("email") );
					insert.put("first_added", user.getLong("first_added", System.currentTimeMillis() ) );
					insert.put("balance", user.getInt("balance"));
					insert.put("last_instore_check", user.getLong("last_instore_check", 0L ) );
					
					db.insert("users", null, insert);
				}
				else
				{
					ContentValues update = new ContentValues();
					update.put("balance", user.getInt("balance") );
					update.put("name", user.getString("name") );
					update.put("email", user.getString("email") );
					update.put("last_instore_check", user.getLong("last_instore_check") );
					
					db.update("users", update, "`id` = '" + user.getString("id") + "'", null);
				}
			}
			
		} catch (Exception e) { e.printStackTrace(); PushLink.sendAsyncException( e ); }
		
		db.close();
		dbr.close();
	}
	
	public static boolean sendTransactions ()
	{
		SQLiteDatabase db = myLittleDB.getReadableDatabase();
		Cursor cursor = db.query("trans", null, null, null, null, null, null);
		
		if ( cursor.getCount() < 1 )
			return false;
		
		JSONArray trans = new JSONArray();
		JSONObj j = JSONObj.emptyObj();
		
		for ( cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext() )
		{
			if ( trans.toString().length() > 8000 )
			{
				__sendTransactions ( trans.toString() );
				trans = new JSONArray();
			}
			
			String id = cursor.getString(0);
			Long time = cursor.getLong(1);
			int n = cursor.getInt(2);
			int p = cursor.getInt(3);
			String action = cursor.getString(4);
			String comment = cursor.getString(5);
			
			try {
			j = JSONObj.emptyObj();
			
			Cursor cursor1 = db.query("users", null, "`id` = '" + id + "'", null, null, null, null);
			cursor1.moveToFirst();
			
			j.put("id", id );
			j.put("time", time );
			j.put("n", n );
			j.put("p", p );
			j.put("action", action );
			j.put("comment", comment );
			
			j.put("email", cursor1.getString(2) );
			j.put("first_added", cursor1.getString(3) );
			
			trans.put( j );
			
			} catch (JSONException e) { e.printStackTrace(); }
		}
		
		__sendTransactions ( trans.toString() );
		
		db.close();
		
		db = myLittleDB.getWritableDatabase();
		//db.execSQL("DELETE FROM `trans`;");
		//db.execSQL("VACUUM;");
		db.close();
		//LaunchActivity.syncDB = false;
		
		return true;
	}
	
	public static void __sendTransactions ( String data )
	{
		JSONObj result = JSONObj.getFromUrlSafe( res.getString( R.string.syncUrl ) + "?data=" + data );
		
		/*
		SQLiteDatabase db = myLittleDB.getWritableDatabase();
		
		try
		{
			JSONArray log = result.getJSONArray("log");
			
			for (int i = 0; i < log.length(); i++)
			{
				Log.d(TAG, "Removing " + log.getString(i) + " from transaction table.");
				//db.delete("trans", "", null);
			}
		} catch (Exception e) { e.printStackTrace(); PushLink.sendAsyncException( e ); }
		
		db.close();
		*/
	}
	
	public void onClick (View v)
	{    
		String s = phone.getText().toString();
		
		if ( num0 == v || num1 == v || num2 == v || num3 == v || num4 == v || num5 == v || num6 == v || num7 == v || num8 == v || num9 == v )
		{
			Button b = (Button) v;
			phone.setText( phone.getText().toString() + b.getText().toString() );
		}
		else if ( scan == v )
		{
			Intent intent = new Intent( getIntent().getAction() );
	        intent.putExtra("ACTION", "scan");
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
		else if ( back == v )
		{
			if ( s.length() > 0 )
				phone.setText( s.substring(0, s.length() - 1) );
		}
		else if ( go == v )
		{
			if ( continueAllowed )
			{
				String result = processNumber( s );
				
				if ( result == null )
				{
					new AlertDialog.Builder(this)
			    	.setMessage( "Sorry, We could not process your request at this time. Please try again later." )
			    	.setPositiveButton("Ok", null)
			    	.show();
				}
				else if ( result.equalsIgnoreCase("success") || result.equalsIgnoreCase("firstTime") )
				{
					phone.setText("");
				}
				else
				{
					new AlertDialog.Builder(this)
			    	.setMessage( result )
			    	.setPositiveButton("Ok", null)
			    	.show();
				}
			}
			else
			{
				new AlertDialog.Builder(this)
            	.setMessage("You must enter the full 10 digit mobile #\n w/o country code (i.e. 7085296564)")
            	.setPositiveButton("Ok", null)
            	.show();
			}
		}
		
		testLength();
	}
	
	public static boolean isNumeric ( String value )
	{
		try
		{
			double dd = Double.parseDouble( value );  
		}
		catch( NumberFormatException nfe )  
		{
			return false;
		}
		
		return true;
	}
	
	public String processNumber ( String phoneNumber )
	{
		if ( !isNumeric( phoneNumber ) )
			return "It seems there is a problem with the Phone Number you provided. Try again.";
		
		if ( phoneNumber.startsWith("555") )
			return "What do you think this is, a movie?\nPlease enter your real mobile number.";
		
		if ( phoneNumber == "1231231234"
				|| phoneNumber == "0000000000"
				|| phoneNumber == "1234567890" )
			return "Sorry, The number you entered has been blacklisted.";
		
		if ( phoneNumber == "7085296564" )
			return "Sorry, Being the service provider, we are exempt from using our services. So please don't use our phone number.";
		
		//	return "Sorry, The entered number did not pass our phone number validation system.";
		
		SQLiteDatabase db = myLittleDB.getReadableDatabase();
		Cursor cursor = db.query("users", null, "`id` = '" + phoneNumber + "'", null, null, null, null);
		
		int balance = 5;
		String name = "";
		String email = "";
		Long last_instore_check = 0L;
		String err = "Sorry, Not enough time has pasted since your last visit!";
		String msg = "Congrats, You have hust earned 5 points for visiting today!";
		
		if ( cursor.getCount() > 0 )
		{
			cursor.moveToFirst();
			name = cursor.getString(1);
			email = cursor.getString(2);
			balance = cursor.getInt(4);
			last_instore_check = cursor.getLong(5);
			
			if ( last_instore_check < System.currentTimeMillis() - 10800000 )
			{
				balance += 5;
				err = null;
			}
				
			if ( name == null || name.equals("") || name.equals("null") )
				name = formatPhoneNumber( phoneNumber );
			
			ContentValues update = new ContentValues();
			update.put("balance", balance);
			update.put("last_instore_check", System.currentTimeMillis());
			
			db.update("users", update, "`id` = '" + phoneNumber + "'", null);
		}
		else
		{
			/*
			name = formatPhoneNumber( phoneNumber );
			
			ContentValues insert = new ContentValues();
			
			insert.put("id", phoneNumber);
			insert.put("first_added", System.currentTimeMillis());
			insert.put("balance", 5);
			insert.put("last_instore_check", System.currentTimeMillis());
			
			db.insert("users", "id", insert);
			db.close();
			
			err = null;
			*/
			
			Intent intent = new Intent(getApplicationContext(), FirstTimeActivity.class);
			intent.putExtra("com.applebloom.apps.phoneNumber", phoneNumber);
			startActivity(intent);
			return "firstTime";
		}
		
		Intent intent = new Intent(getApplicationContext(), DoneActivity.class);
		
		if ( err == null )
		{
			ContentValues trans = new ContentValues();
			trans.put("id", phoneNumber);
			trans.put("time", System.currentTimeMillis());
			trans.put("n", 0);
			trans.put("p", 5);
			trans.put("action", "Earned Points");
			trans.put("comment", "Device Balance: " + balance);
			SQLiteDatabase tdb = myLittleDB.getWritableDatabase();
			tdb.insert("trans", null, trans);
			tdb.close();
			
			intent.putExtra("com.applebloom.apps.message", msg);
		}
		else
		{
			intent.putExtra("com.applebloom.apps.warning", err);
		}
		
		intent.putExtra("com.applebloom.apps.phoneNumber", phoneNumber);
		
		syncDB = true;
		startActivity(intent);
		
		return "success";
	}
	
	public static String formatPhoneNumber ( String rawPhoneNumber )
	{
		PhoneNumberUtil putil = PhoneNumberUtil.getInstance();
		try
		{
			PhoneNumber num = putil.parse(rawPhoneNumber, "US");
			return putil.format(num, PhoneNumberFormat.NATIONAL);
		}
		catch ( NumberParseException e )
		{
			return rawPhoneNumber;
		}
	}
	
	public void testLength ()
	{
		String s = phone.getText().toString();
		
		if ( s.length() == 10 )
		{
			if ( s.substring(0, 1).equals("1") )
			{
				phone.setText( s.substring(1, s.length()) );
			}
			else
			{
				continueAllowed = true;
				
				go.setBackgroundResource(R.drawable.go_button_green);
				// Enable GO!
				
				return;
			}
		}
		
		continueAllowed = false;
		
		go.setBackgroundResource(R.drawable.go_button_gray);
	}

	public boolean onLongClick(View v)
	{
		if ( back == v )
		{
			phone.setText("");
		}
		
		return false;
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if (keyCode == KeyEvent.KEYCODE_FOCUS
    			|| keyCode == KeyEvent.KEYCODE_CAMERA
    			|| keyCode == KeyEvent.KEYCODE_BACK
    			|| keyCode == KeyEvent.KEYCODE_POWER
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
    		) {
    		
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);    		  
    	}
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        PushLink.setCurrentPopUpTarget(this);
    }
    
	public static void restartDevice ()
	{
		try {
			doCmds("reboot");
		} catch (Exception e) {
			e.printStackTrace();
			PushLink.sendAsyncException(e);
		}
	}
	
	public static Context getAppContext()
	{
        return context;
    }
	
	public static void doCmds(String cmd) throws Exception
	{
	    Process process = Runtime.getRuntime().exec("su");
	    DataOutputStream os = new DataOutputStream(process.getOutputStream());
	    //DataInputStream is = new DataInputStream(process.getInputStream());
	    
	    BufferedReader is = new BufferedReader( new InputStreamReader(process.getInputStream()) );
	    
	    Log.v(TAG, "Sending Command to System");
	    
	    /*
	    for (String tmpCmd : cmds) {
	            os.writeBytes(tmpCmd+"\n");
	    }
	    */
	    
	    os.writeBytes(cmd+"\n");

	    os.writeBytes("exit\n");
	    
	    String line;
		while ((line = is.readLine()) != null) {
	    	System.out.println(line);
	    	Log.v(TAG, "Command Result: " + line);
	    }
	    
		os.flush();
	    os.close();
	    is.close();
	    
	    process.waitFor();
	}
	
	private class ScreenReceiver extends BroadcastReceiver
	{
		protected ScreenReceiver() {
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Log.w(TAG, "Screen has been turned off.");
				
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				@SuppressWarnings("deprecation")
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
						PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
				wl.acquire();
				wl.release();
				
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				Log.w(TAG, "Screen has been turned on.");
			}
		}
	}
	
	public static boolean haveNetworkConnection()
	{
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        boolean haveConnectedEthernet = false;

        ConnectivityManager cm = (ConnectivityManager) getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
            if (ni.getTypeName().equalsIgnoreCase("ETHERNET") || ni.getTypeName().equalsIgnoreCase("ETH"))
                if (ni.isConnected())
                    haveConnectedEthernet = true;
            
            //if (ni.isConnected())
            	//Log.d(TAG, "We found an active internet connection using: " + ni.getTypeName());
        }
        
        return haveConnectedWifi || haveConnectedMobile || haveConnectedEthernet;
    }

	public static LaunchActivity getInstance()
	{
		return instance;
	}

	private static void setInstance(LaunchActivity instance)
	{
		LaunchActivity.instance = instance;
	}
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if ( requestCode == REQUEST_SCAN )
    	{
    		if (resultCode == Activity.RESULT_OK)
    		{
    			String result = intent.getStringExtra("SCAN_RESULT");
    			
    			if ( result.substring( result.length() - 6 ).toLowerCase().equals("cg092m") || result.substring( result.length() - 6 ).toLowerCase().equals("aa126k") )
    			{
    				//Open Settings
    				//Intent intent1 = new Intent( android.provider.Settings.ACTION_SETTINGS);
                	//startActivity(intent1);
    			}
    			else
    			{
    				processNumber( result );
    			}
                
                Log.d(TAG, "Barcode scan succeded with Value \"" + intent.getStringExtra("SCAN_RESULT") + "\".");
    		}
    		else if (resultCode == Activity.RESULT_CANCELED) 
    		{
                Log.d(TAG, "Barcode scan has been canceled.");
    		}
    		else
    		{
    			Log.d(TAG, "Barcode scan failed to return data for an unknown reason. Check logs.");
    		}
    	}
    }
}