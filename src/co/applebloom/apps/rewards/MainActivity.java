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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.chiorichan.android.HTTPParser;
import com.chiorichan.android.JSONObj;
import com.pushlink.android.PushLink;

public class MainActivity extends Activity
{
	public CookieStore cookieStore = new BasicCookieStore();
	public HttpContext localContext = new BasicHttpContext();
	
	public static String appVersion = "2.3.1206 (Derpy Hooves)";
	private final static String TAG = "AppleBloomRewards";
	public static final int REQUEST_STEP1 = 10000001;
	public static final int REQUEST_STEP2 = 10000002;
	public static final int REQUEST_STEP3 = 10000003;
	public static final int REQUEST_STEP4 = 10000004;
	public static final int REQUEST_STEP5 = 10000005;
	public static final int REQUEST_SCAN = 10000006;
	public static final int REQUEST_FINISH = 99999999;
	private static JSONObj myLittleJSON = null;
	private static String deviceState = "";
	public static MainActivity instance;
	public static String uuid = null;
    private static Context context;
	private static Resources res;
	private static Boolean userActive = false;
	public static String lastLocID = "";
	
	public static boolean emailActivityFinished;
	public static boolean firstActivityFinished;
	public String emailResult;
	public boolean firstResult;
	public boolean activityCancelled = false;
	
	public String deviceTitle = "";
	public String deviceAddress1 = "";
	public String deviceAddress2 = "";
			
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		res = this.getResources();
		
        super.onCreate(savedInstanceState);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
        setContentView(R.layout.pleasewait);
        
        context = getApplicationContext();
        instance = this;
        
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        
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
        
        try
        {
        	PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	appVersion = pInfo.versionName;
        }
        catch ( Exception e )
        {
        	PushLink.sendAsyncException( e );
        }
        
        new ScreenReceiver(); // Start Monitor that prevents the user from turning the screen off.
        new startMonitor().execute();
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        PushLink.setCurrentPopUpTarget(this);
    }
    
    public void updateUI ()
    {
    	TextView uuid_tv = (TextView) findViewById( R.id.uuid );
    	uuid_tv.setText( "Device UUID: " + uuid );
    	
    	TextView msg_tv = (TextView) findViewById( R.id.msg );
    	msg_tv.setText( deviceState );
    	
    	deviceTitle = myLittleJSON.getStringSafe("title");
    	deviceAddress1 = myLittleJSON.getStringSafe("address1");
    	deviceAddress2 = myLittleJSON.getStringSafe("address2");
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
	
	public static class startMonitor extends AsyncTask<Void, JSONObj, Void>
	{
		private static final int FLAG_ACTIVITY_NEW_TASK = 268435456;

		@Override
        protected void onProgressUpdate(JSONObj... values)
        {
            myLittleJSON = values[0];
            instance.updateUI();
        }
		
		@Override
		protected Void doInBackground(Void... noparams)
		{
			while(true)
			{
				JSONObj json = null;
				
				if ( uuid == null || uuid.isEmpty() )
				{
					SharedPreferences sharedPrefs = instance.getSharedPreferences("AppleBloomRewards", 0);
					String uuid = sharedPrefs.getString("uuid", null);
					
					if ( uuid == null )
					{
						String seed = Settings.Secure.getString(instance.getContentResolver(), "android_id");
						
						if ( seed == null )
			    			seed = UUID.randomUUID().toString();
						
						Log.v(TAG, "UUID SEED: " + seed);
						
						try
						{
							uuid = HTTPParser.getFromUrl(res.getString(R.string.firstRunUrl) + "?seed=" + seed);
							
							SharedPreferences.Editor editor = sharedPrefs.edit();
				    		editor.putString("uuid", uuid);
				    		editor.commit();
						}
						catch ( Exception e )
						{
							e.printStackTrace();
			    			PushLink.sendAsyncException(e);
						}
					}
					
					if ( uuid != null && !uuid.isEmpty() )
					{
						Log.v(TAG, "Device UUID: " + uuid);
						MainActivity.uuid = uuid;
						PushLink.addMetadata("Device UUID", uuid);
						
					}
			    	
			    	Log.v(TAG, "uuid: " + uuid);
			    	
			    	/*
			    	homeActivity.uuid = uuid;
			    	TextView id = (TextView) this.findViewById(R.id.uuid);
			    	id.setText("Device UUID: " + uuid);
			    	*/
				}
				
				if ( uuid == null || uuid.isEmpty() )
				{
					json = JSONObj.emptyObj();
					
					if ( homeActivity.instance != null )
					{
						homeActivity.instance.setResult(RESULT_CANCELED);
						homeActivity.instance.finish();
					}
					
					MainActivity.deviceState = "There seems to be a problem with the Devices UUID.";
				}
				else
				{
					json = JSONObj.getFromUrlSafe( res.getString(R.string.pingerUrl) + "?state=" + "Running" + "&appVersion=" + appVersion );
					
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
							
							if ( !userActive && homeActivity.instance == null )
							{
								Intent intent = new Intent( instance, homeActivity.class );
								intent.putExtra("json", json.toString());
								instance.startActivityForResult(intent, REQUEST_STEP1);
							}
						}
						else
						{
							deviceState = "This device has not been assigned to any locations.";
							
							if ( instance != null )
							{
								instance.setResult(RESULT_CANCELED);
								instance.finish();
							}
						}
					}
				}
				
				if ( userActive )
				{
					// TODO: Check integrity
				}
				
				publishProgress( json );
				
				SystemClock.sleep(15000);
			}
		}
	}
	
	public boolean haveNetworkConnection()
	{
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        boolean haveConnectedEthernet = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
	
	public void resumeHome ()
	{
		userActive = false;
		
		if ( MainActivity.instance == null )
		{
			Intent intent = new Intent( this, homeActivity.class );
			intent.putExtra("json", myLittleJSON.toString());
			startActivityForResult(intent, REQUEST_STEP1);
		}
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if ( requestCode == REQUEST_STEP1 && resultCode == RESULT_OK )
    	{
    		userActive = true;
    		
    		if ( intent.getStringExtra("ACTION") == "scan" )
    		{
    			Intent intentScan = new Intent ( "co.applebloom.apps.scanner.SCAN" );
                intentScan.addCategory(Intent.CATEGORY_DEFAULT);
                this.startActivityForResult(intentScan, REQUEST_SCAN);
    		}
    		else
    		{
    			String err = null;
    			String mobile_no = intent.getStringExtra("MOBILE_NO");
    			
    			if ( mobile_no.startsWith("555") )
    				err = "What do you think this is, a movie?\nPlease enter your real mobile number.";
    			
    			if ( mobile_no == "1231231234" || mobile_no == "1234567890" )
    				err = "Sorry, The number you entered has been blacklisted.";
    			
    			if ( mobile_no == "7085296564" )
    				err = "Sorry, Being the service provider, we are exempt from using our services. So please don't use our phone number.";
    			
    			// TODO: Replace with an actual point capture
    			if ( !haveNetworkConnection() )
    				err = "Sorry, It seems the internet is inaccessable.";
    			
    			if ( err != null )
    			{
    				Intent intent1 = new Intent(this, ErrorActivity.class);
    				intent1.putExtra("ERROR", err);
    				startActivityForResult(intent1, REQUEST_FINISH);
    				return;
    			}
    			
    			JSONObj jObj = JSONObj.emptyObj();
    			
    			try
    			{
    				jObj = JSONObj.getFromUrl(res.getString(R.string.lookupUrl) + "?points=" + myLittleJSON.getString("bonusPoints") + "&acct=" + mobile_no);
    			}
    			catch ( Exception e )
    			{
    				e.printStackTrace();
    				PushLink.sendAsyncException(e);
    				
    				// TODO: Replace with an actual point capture
    				err = "Sorry, We had a problem retreiving your information for our servers due to unknown reason.";
    			}
    			
    			if ( !jObj.getStringSafe("err").isEmpty() )
    				err = jObj.getStringSafe("err");
    			
    			if ( err != null )
    			{
    				Intent intent1 = new Intent(this, ErrorActivity.class);
    				intent1.putExtra("ERROR", err);
    				startActivityForResult(intent1, REQUEST_FINISH);
    				return;
    			}
    			
    			final JSONObj json = jObj;
    			emailActivityFinished = !json.getBooleanSafe("collectEmail"); 
    			firstActivityFinished = !json.getBooleanSafe("firstTime");
    			activityCancelled = false;
    			
    			Thread progressChecker = new Thread(new Runnable() {
    				@Override
    				public void run()
    				{
    					Boolean updateRecords = false;
    					
    					if ( json.getBooleanSafe("collectEmail") )
    					{
    						Intent intent = new Intent( instance, EmailCollection.class );
    						intent.putExtra( "json", json.toString() );
    						startActivityForResult(intent, REQUEST_STEP2);
    					}
    					
    					while (!emailActivityFinished)
    					{
    						SystemClock.sleep(1000);
    						updateRecords = true;
    						if ( activityCancelled )
    						{
    							
    							resumeHome();
    							return;
    						}
    					}
    					
    					if ( json.getBooleanSafe("firstTime") )
    					{
    						Intent intent = new Intent(instance, FirsttimeCollection.class);
    						intent.putExtra( "json", json.toString() );
    						startActivityForResult(intent, REQUEST_STEP3);
    					}
    					
    					while (!firstActivityFinished)
    					{
    						SystemClock.sleep(1000);
    						updateRecords = true;
    						if ( activityCancelled )
    						{
    							resumeHome();
    							return;
    						}
    					}
    					
    					if ( activityCancelled )
    					{
    						resumeHome();
							return;
    					}
    					
    					if ( updateRecords )
    					{
    						JSONObj update = JSONObj.getFromUrlSafe( res.getString( R.string.updateUrl ) + "?acct=" + json.getStringSafe("rawNumber") + "&email=" + emailResult + "&texting=" + firstResult );
    					}
    					
    					Intent intent = new Intent( instance, DoneActivity.class );
    					intent.putExtra( "json", json.toString() );
    					startActivityForResult(intent, REQUEST_FINISH );
    					
    					while (!activityCancelled)
    						SystemClock.sleep(1000);
    					
    					resumeHome();
    				}
    			});
    			
    			progressChecker.start();
    		}
    	}
    	else if ( requestCode == REQUEST_STEP2 )
    	{
    		if ( resultCode == RESULT_CANCELED )
    		{
    			activityCancelled = true;
    		}
    		else
    		{
    			try
    			{
    				emailResult = intent.getStringExtra("EMAIL");    				
    			}
    			catch ( Exception e )
    			{
    				emailResult = "";
    			}
    			emailActivityFinished = true;
    		}
    	}
    	else if ( requestCode == REQUEST_STEP3 )
    	{
    		if ( resultCode == RESULT_CANCELED )
    		{
    			activityCancelled = true;
    		}
    		else
    		{
    			try
    			{
    				firstResult	= intent.getBooleanExtra("TEXTING", false);    				
    			}
    			catch ( Exception e )
    			{
    				firstResult = false;
    			}
    			
        		firstActivityFinished = true;    			
    		}
    	}
    	else if ( requestCode == REQUEST_FINISH )
    	{
    		activityCancelled = true;
    		resumeHome();
    	}
    	else if ( requestCode == REQUEST_SCAN )
    	{
    		if (resultCode == Activity.RESULT_OK)
    		{
    			String result = intent.getStringExtra("SCAN_RESULT");
    			
    			if ( result.substring( result.length() - 6 ).toLowerCase().equals("cg092m") || result.substring( result.length() - 6 ).toLowerCase().equals("aa126k") )
    			{
    				Intent intent1 = new Intent( android.provider.Settings.ACTION_SETTINGS);
                	startActivity(intent1);
    			}
    			else
    			{
    				/*
    				Intent intent1 = new Intent(this, ProcessingActivity.class);
    				intent1.putExtra("PHONE_NUMBER", result);
    				intent1.putExtra("BONUS_POINTS", 5);
    				startActivity(intent1);
    				*/
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
    		
    		resumeHome();
    	}
    }
	
	/*
	public static void setUUID( String uuid )
	{
		SharedPreferences sharedPrefs = main.getSharedPreferences("AppleBloomRewards", 0);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString("uuid", uuid);
		editor.commit();
	}
	*/
}