package co.applebloom.apps.rewards;

import com.pushlink.android.PushLink;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;

public class ProcessingActivity extends Activity implements OnClickListener, OnLongClickListener
{
	public final static int COLLECT_EMAIL = 0x0ba7c1de;
	public final static int FIRST_TIME = 0x0ba7c2de;
	public final static int SHOW_RESULTS = 0x0d64b443;
	public final static int SHOW_ERROR = 0x0d64b444;
	
	public static boolean emailActivityFinished;
	public static boolean firstActivityFinished;
	public String emailResult;
	public boolean firstResult;
	public boolean activityCancelled = false;
	
	private static ProcessingActivity me;
	
	private Resources res;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
		setContentView(R.layout.processing);
		
        if ( LookupResultActivity.instance != null )
        	LookupResultActivity.instance.finish();
        
        me = this;
		res = getResources();
		
		Intent intent = getIntent();
        String m = intent.getStringExtra("PHONE_NUMBER");
        int p = intent.getIntExtra("BONUS_POINTS", 5);
		
        if ( m == null )
        	finish();
        
        String response = lookupPhone(m, p);
		
		if ( response != null )
		{
			Intent intent1 = new Intent(this, ErrorActivity.class);
			intent1.putExtra("", response);
			startActivityForResult(intent1, SHOW_ERROR);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
	}
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if ( requestCode == COLLECT_EMAIL )
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
    	else if ( requestCode == FIRST_TIME )
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
    	else if ( requestCode == SHOW_RESULTS )
    	{
    		activityCancelled = true;
    	}
    	else if ( requestCode == SHOW_ERROR )
    	{
    		finish();
    		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    	}
    }

	@Override
	public boolean onLongClick(View v)
	{
		return false;
	}

	@Override
	public void onClick(View v)
	{
		
	}
	
	public String lookupPhone (String phoneNumber, int bonusPoints)
	{
		String result = null;
		
		if ( phoneNumber.startsWith("555") )
			return "What do you think this is, a movie?\nPlease enter your real mobile number.";
		
		if ( phoneNumber == "1231231234" || phoneNumber == "1234567890" )
			return "Sorry, The number you entered has been blacklisted.";
		
		if ( phoneNumber == "7085296564" )
			return "Sorry, Being the service provider, we are exempt from using our services. So please don't use our phone number.";
		
		if ( !haveNetworkConnection() )
		{
			// TODO: Capture Customer Information
			
			return "Sorry, It seems the internet inaccessable.\nWe will credit your points when the connection is back up.";
		}
		
		try {
			final JSONObj json = JSONObj.getFromUrl(res.getString(R.string.lookupUrl) + "?points=" + bonusPoints + "&acct=" + phoneNumber);
			final String mobile_no = phoneNumber;
			
			if ( json == null )
				return "Sorry, Failed to lookup your information due to an unknown error";
			
			if ( !json.getStringSafe("err").isEmpty() )
				return json.getString("err", "");
			
			emailActivityFinished = !json.getBooleanSafe("collectEmail"); 
			firstActivityFinished = !json.getBooleanSafe("firstTime");
			
			Thread progressChecker = new Thread(new Runnable() {
				@Override
				public void run()
				{
					Boolean updateRecords = false;
					
					if ( json.getBooleanSafe("collectEmail") )
					{
						Intent intent = new Intent(me, EmailCollection.class);
						intent.putExtra("", json.toString());
						startActivityForResult(intent, COLLECT_EMAIL);
						overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
					}
					
					while (!emailActivityFinished)
					{
						SystemClock.sleep(1000);
						updateRecords = true;
						if ( activityCancelled )
						{
							finish();
							me.finish();
						}
					}
					
					if ( json.getBooleanSafe("firstTime") )
					{
						Intent intent = new Intent(me, FirsttimeCollection.class);
						intent.putExtra("", json.toString());
						startActivityForResult(intent, FIRST_TIME);
					}
					
					while (!firstActivityFinished)
					{
						SystemClock.sleep(1000);
						updateRecords = true;
						if ( activityCancelled )
						{
							finish();
							me.finish();
							me.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
						}
					}
					
					if ( activityCancelled )
					{
						finish();
						me.finish();
						me.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
					}
					
					if ( updateRecords )
					{
						JSONObj update = JSONObj.getFromUrlSafe( res.getString( R.string.updateUrl ) + "?acct=" + mobile_no + "&email=" + emailResult + "&texting=" + firstResult );
					}
					
					Intent intent = new Intent(me, LookupResultActivity.class);
					intent.putExtra("", json.toString());
					startActivityForResult(intent, SHOW_RESULTS);
					overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
					
					while (!activityCancelled)
						SystemClock.sleep(1000);
					
					me.finish();
		    		me.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
				}
			});
			
			progressChecker.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			PushLink.sendAsyncException(e);
			return "Sorry, Failed to lookup your information due to an application exception.";
		}
		
		return result;
	}
	
    private boolean haveNetworkConnection() {
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
}