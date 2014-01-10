package co.applebloom.apps.rewards;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
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
import android.widget.Toast;
import co.applebloom.apps.scanner.ScannerActivity;

import com.chiorichan.android.JSONObj;
import com.chiorichan.android.MyLittleDB;
import com.chiorichan.android.SplashView;
import com.chiorichan.apps.rewards.ConfigHandler;
import com.chiorichan.apps.rewards.Contact;
import com.chiorichan.apps.rewards.PostProcessing;
import com.chiorichan.apps.rewards.PostProcessing.ActionList;
import com.chiorichan.apps.rewards.packet.LookupContactPacket;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.net.CommonUtils;
import com.chiorichan.net.NetworkHandler;
import com.chiorichan.util.Common;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.pushlink.android.FriendlyPopUpStrategy;
import com.pushlink.android.PushLink;
import com.pushlink.android.StrategyEnum;

public class LaunchActivity extends Activity implements OnClickListener, OnLongClickListener
{
	public static SharedPreferences sharedPrefs = null;
	public static final int REQUEST_SCAN = 10000006;
	public static MyLittleDB myLittleDB = null;
	public static Boolean registered = false;
	public static String appVersion = "";
	public static Resources res;
	
	private Button go;
	private Handler uiThreadHandler = new Handler();
	protected TextView titlev, address, version, phone, deviceState;
	private boolean continueAllowed = false;
	private ImageButton scan, back;
	
	private static final String TAG = "ABRewards";
	private static LaunchActivity instance;
	private static ImageView headerImage;
	private static Context context;
	
	private static NetworkHandler tcpHandler = new NetworkHandler();
	private static ConfigHandler configHandler;
	private static List<String> cookingToast = new ArrayList<String>();
	private int updateUIInterval = 1000; // Every second until app loads.
	public static LookupContactPacket serverResult;
	
	public static boolean uiNeedsUpdating = true;
	
	@Override
	public void onCreate( final Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN );
		
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );
		
		setInstance( this );
		res = this.getResources();
		context = this.getApplicationContext();
		myLittleDB = new MyLittleDB( context );
		
		SplashView splashView = new SplashView( this );
		
		final LaunchActivity mainThis = this;
		
		splashView.setSplashEventHandler( new SplashView.SplashEvents()
		{
			@Override
			public void onSplashDrawComplete()
			{
				// Post the runnable that will put up the main view
				uiThreadHandler.post( new Runnable()
				{
					@Override
					public void run()
					{
						launchMainView( mainThis, savedInstanceState );
					}
				} );
			}
		} );
		
		this.setContentView( splashView );
		
		configHandler = new ConfigHandler();
		
		tcpHandler.execute();
		
		new updateUI().execute();
		new ScreenReceiver();
		
		YamlConfiguration config = configHandler.getConfig();
		
		tcpHandler.setUUID( config.getString( "device.uuid", "{unregistered}" ) );
	}
	
	public void launchMainView( LaunchActivity mainThis, Bundle savedInstanceState )
	{
		setContentView( R.layout.home );
		
		sharedPrefs = getSharedPreferences( "AppleBloomRewards", 0 );
		
		headerImage = (ImageView) findViewById( R.id.headerImage );
		
		address = (TextView) findViewById( R.id.address );
		titlev = (TextView) findViewById( R.id.title );
		version = (TextView) findViewById( R.id.version );
		deviceState = (TextView) findViewById( R.id.msg );
		
		scan = (ImageButton) findViewById( R.id.button_scan );
		back = (ImageButton) findViewById( R.id.button_back );
		
		phone = (TextView) findViewById( R.id.phone );
		go = (Button) findViewById( R.id.gobutt );
		
		scan.setOnClickListener( this );
		back.setOnClickListener( this );
		back.setOnLongClickListener( this );
		go.setOnClickListener( this );
		phone.setCursorVisible( false );
		
		phone.addTextChangedListener( new TextWatcher()
		{
			public void afterTextChanged( Editable s )
			{
				testLength();
			}
			
			public void beforeTextChanged( CharSequence s, int start, int count, int after )
			{
			}
			
			public void onTextChanged( CharSequence s, int start, int before, int count )
			{
			}
		} );
		
		try
		{
			PackageInfo pInfo = getPackageManager().getPackageInfo( getPackageName(), 0 );
			appVersion = pInfo.versionName;
			version.setText( "Apple Bloom Rewards Version " + appVersion );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			// sendException( e );
		}
		
		// Update to every 10 seconds because the app is now loaded.
		updateUIInterval = 10000;
	}
	
	public static String getPrefString( String key )
	{
		return getPrefString( key, null );
	}
	
	public static String getPrefString( String key, String dft )
	{
		return sharedPrefs.getString( key, dft );
	}
	
	public static Boolean getPrefBoolean( String key )
	{
		return getPrefBoolean( key, false );
	}
	
	public static Boolean getPrefBoolean( String key, Boolean dft )
	{
		return sharedPrefs.getBoolean( key, dft );
	}
	
	public static void startPushLink( String DeviceUUID )
	{
		PushLink.start( getAppContext(), R.drawable.ic_launcher, "3vcnlaneunf3k0k0", DeviceUUID );
		
		PushLink.setCurrentStrategy( StrategyEnum.FRIENDLY_POPUP );
		PushLink.addMetadata( "Version", appVersion );
		PushLink.addMetadata( "Android Version", Build.VERSION.RELEASE );
		
		FriendlyPopUpStrategy fps = (FriendlyPopUpStrategy) PushLink.getCurrentStrategy();
		fps.setPopUpMessage( "New critical bugfixes are now available. Please update." );
		fps.setNotNowButton( "Later" );
		fps.setUpdateButton( "Update Now" );
		fps.setReminderTimeInSeconds( 60 * 15 );
	}
	
	public void applyHeaderImage( String filename )
	{
		if ( headerImage == null )
			return;
		
		if ( filename == null || filename == "" )
		{
			// It seems that no image was available. Set it do that system default.
			headerImage.setBackgroundResource( R.drawable.default_logo );
			return;
		}
		else
		{
			headerImage.setBackgroundDrawable( null );
			UrlImageViewHelper.setUrlDrawable( headerImage, filename );
		}
	}
	
	public void numpadClick( View v )
	{
		phone.setText( phone.getText().toString() + ( (Button) v ).getText().toString() );
	}
	
	public void onClick( View v )
	{
		String s = phone.getText().toString();
		
		if ( scan == v )
		{
			Intent intent = new Intent( this, ScannerActivity.class );
			intent.putExtra( "ACTION", "scan" );
			setResult( Activity.RESULT_OK, intent );
		}
		else if ( back == v )
		{
			if ( s.length() > 0 )
				phone.setText( s.substring( 0, s.length() - 1 ) );
		}
		else if ( go == v )
		{
			if ( s.equalsIgnoreCase( "016564" ) )
			{
				Intent intent = new Intent( this, AdminActivity.class );
				startActivity( intent );
				phone.setText( "" );
				return;
			}
			
			if ( continueAllowed )
			{
				new goTask( this, s ).execute();
				phone.setText( "" );
			}
			else
			{
				new AlertDialog.Builder( this ).setMessage( "You must enter the full 10 digit mobile #\n w/o country code (i.e. 7085296564)" ).setPositiveButton( "Ok", null ).show();
			}
		}
		
		testLength();
	}
	
	class goTask extends AsyncTask<Void, Void, String>
	{
		Context context;
		String phoneNumber;
		ProgressDialog mDialog;
		
		goTask(Context context, String phone)
		{
			this.context = context;
			phoneNumber = phone;
		}
		
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			
			mDialog = new ProgressDialog( context );
			mDialog.setMessage( "Please wait..." );
			mDialog.show();
		}
		
		@Override
		protected String doInBackground( Void... params )
		{
			// Does this phone number use valid US formatting rules?
			if ( !CommonUtils.isNumeric( phoneNumber ) )
				return "It seems there is a problem with the Phone Number you provided. Try again.";
			
			// Check #2 - Phone numbers can not start with a 0
			if ( phoneNumber.startsWith( "0" ) )
				return "According to the US Phone Number formatting rules, You do not exist.\nSo, I'm sorry but I will have to disallow the use of this number.";
			
			// Check #3 - Is the customer trying to use a fictional phone number from a movie?
			if ( phoneNumber.startsWith( "555" ) )
				return "We're Sorry, We can not provide service to fictional people at this time.\n\nHasta la vista, baby!";
			
			// Check #4 - Is the customer trying to use a number that is known too be fake but is also used all too frequently?
			if ( phoneNumber == "1231231234" || phoneNumber == "0000000000" || phoneNumber == "1234567890" )
				return "We're Sorry, The number you entered has been blacklisted.";
			
			// Check #5 - Is the customer trying to use our service number?
			if ( phoneNumber == "7085296564" )
				return "We're Sorry, Being the service provider it would not be right of us to earn points. So please don't use our phone number.";
			
			LookupContactPacket serverResult = null;
			
			// If we are currently active with the server. Attempt to get up to date information about this contact.
			if ( LaunchActivity.getTcpHandler().isConnected() && LaunchActivity.getTcpHandler().isRegistered() )
			{
				long timeout = System.currentTimeMillis() + 10000;
				boolean done = false;
				String requestId = Common.md5( System.currentTimeMillis() + "JUST A SECURITY MEASURE!!!" );
				
				// Void any previous results
				LaunchActivity.serverResult = null;
				
				// Send the packet
				LaunchActivity.getTcpHandler().sendPacket( new LookupContactPacket( phoneNumber, requestId ) );
				
				do
				{
					// Check if the packet has been returned from the server.
					if ( LaunchActivity.serverResult != null && LaunchActivity.serverResult.getId().equals( requestId ) )
					{
						serverResult = LaunchActivity.serverResult;
						done = true;
					}
					
					// Allow up to 10 seconds for the server to respond.
					if ( timeout > System.currentTimeMillis() )
						done = true;
					
					// Sleep duh! 100 milliseconds
					SystemClock.sleep( 100 );
				}
				while ( !done );
			}
			
			// Pull the contact information from the YAML datastore.
			Contact contact;
			Object obj = LaunchActivity.getConfigHandler().getConfig().get( "contacts." + phone );
			
			if ( obj instanceof Contact )
			{
				// Success - Cast it for reading
				contact = (Contact) obj;
			}
			else
			{
				// Failure - Create a new one
				contact = new Contact( phoneNumber, "", 0, 0 );
			}
			
			// Check if the earlier request for contact information was successful
			if ( serverResult != null && serverResult.getContact() != null )
			{
				Contact serverContact = serverResult.getContact();
				
				// Has the customer used their account elsewhere more recently?
				if ( serverContact.last_check > contact.last_check )
				{
					contact.last_check = serverContact.last_check;
					contact.bal = serverContact.bal;
				}
			}
			
			// Save the known data
			LaunchActivity.getConfigHandler().getConfig().set( "contacts." + phone, contact );
			
			// TO BE CONTINUED
			
			return null;
		}
		
		@Override
		protected void onPostExecute( String result )
		{
			super.onPostExecute( result );
			
			if ( result == null )
			{
				new AlertDialog.Builder( context ).setMessage( "Sorry, We could not process your request at this time. Please try again later." ).setPositiveButton( "Ok", null ).show();
			}
			else if ( result.equalsIgnoreCase( "success" ) )
			{	
				
			}
			else if ( result.equalsIgnoreCase( "firstTime" ) )
			{
				Intent intent = new Intent( getApplicationContext(), FirstTimeActivity.class );
				intent.putExtra( "com.applebloom.apps.phoneNumber", phoneNumber );
				startActivity( intent );
			}
			else
			{
				new AlertDialog.Builder( context ).setMessage( result ).setPositiveButton( "Ok", null ).show();
			}
			
			mDialog.dismiss();
		}
		
		public String processNumber( String phoneNumber )
		{
			
			// return
			// "Sorry, The entered number did not pass our phone number validation system.";
			
			SQLiteDatabase db = myLittleDB.getReadableDatabase();
			Cursor cursor = db.query( "users", null, "`id` = '" + phoneNumber + "'", null, null, null, null );
			
			int balance = 5;
			String name = "";
			String email = "";
			Long first_added = 0L;
			Long last_instore_check = 0L;
			String err = "Sorry, Not enough time has pasted since your last visit!";
			String msg = "Congrats, You have hust earned 5 points for visiting today!";
			
			if ( cursor.getCount() > 0 )
			{
				cursor.moveToFirst();
				name = cursor.getString( 1 );
				email = cursor.getString( 2 );
				first_added = cursor.getLong( 2 );
				balance = cursor.getInt( 4 );
				last_instore_check = cursor.getLong( 5 );
				
				if ( last_instore_check < System.currentTimeMillis() - 10800000 )
				{
					balance += 5;
					err = null;
				}
				
				if ( name == null || name.equals( "" ) || name.equals( "null" ) )
					name = CommonUtils.formatPhoneNumber( phoneNumber );
				
				ContentValues update = new ContentValues();
				update.put( "balance", balance );
				update.put( "last_instore_check", System.currentTimeMillis() );
				
				db.update( "users", update, "`id` = '" + phoneNumber + "'", null );
			}
			else
			{
				
				return "firstTime";
			}
			
			Intent intent = new Intent( getApplicationContext(), DoneActivity.class );
			
			if ( err == null )
			{
				ContentValues trans = new ContentValues();
				trans.put( "id", phoneNumber );
				trans.put( "time", System.currentTimeMillis() );
				trans.put( "n", 0 );
				trans.put( "p", 5 );
				trans.put( "action", "Earned Points" );
				trans.put( "comment", "Balance: " + balance );
				SQLiteDatabase tdb = myLittleDB.getWritableDatabase();
				tdb.insert( "trans", null, trans );
				tdb.close();
				
				JSONObj jsn = null;
				try
				{
					jsn = new JSONObj( "{}" );
					
					jsn.put( "id", phoneNumber );
					jsn.put( "name", ( ( name.equals( CommonUtils.formatPhoneNumber( phoneNumber ) ) ) ? "" : name ) );
					jsn.put( "email", email );
					jsn.put( "first_added", first_added );
					jsn.put( "balance", balance );
					jsn.put( "last_instore_check", System.currentTimeMillis() );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				finally
				{
					// s.sendMessageSync( "ACCT " + jsn.toString() );
				}
				
				intent.putExtra( "com.applebloom.apps.message", msg );
			}
			else
			{
				intent.putExtra( "com.applebloom.apps.warning", err );
			}
			
			intent.putExtra( "com.applebloom.apps.phoneNumber", phoneNumber );
			
			startActivity( intent );
			
			return "success";
		}
	}
	
	public void testLength()
	{
		String s = phone.getText().toString();
		
		if ( s.length() == 10 )
		{
			if ( s.substring( 0, 1 ).equals( "1" ) )
			{
				phone.setText( s.substring( 1, s.length() ) );
			}
			else
			{
				continueAllowed = true;
				
				go.setBackgroundResource( R.drawable.go_button_green );
				// Enable GO!
				
				return;
			}
		}
		
		continueAllowed = false;
		
		go.setBackgroundResource( R.drawable.go_button_gray );
	}
	
	public boolean onLongClick( View v )
	{
		if ( back == v )
		{
			phone.setText( "" );
		}
		
		return false;
	}
	
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event )
	{
		if ( keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE )
		{
			
			return true;
		}
		else
		{
			return super.onKeyDown( keyCode, event );
		}
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		PushLink.setCurrentActivity( this );
	}
	
	public static Context getAppContext()
	{
		return context;
	}
	
	public static LaunchActivity getInstance()
	{
		return instance;
	}
	
	private static void setInstance( LaunchActivity instance )
	{
		LaunchActivity.instance = instance;
	}
	
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent intent )
	{
		if ( requestCode == REQUEST_SCAN )
		{
			if ( resultCode == Activity.RESULT_OK )
			{
				String result = intent.getStringExtra( "SCAN_RESULT" );
				
				if ( result.substring( result.length() - 6 ).toLowerCase().equals( "cg092m" ) || result.substring( result.length() - 6 ).toLowerCase().equals( "aa126k" ) )
				{
					// Open Settings
					// Intent intent1 = new Intent(
					// android.provider.Settings.ACTION_SETTINGS);
					// startActivity(intent1);
				}
				else
				{
					// processNumber( result );
				}
				
				Log.d( TAG, "Barcode scan succeded with Value \"" + intent.getStringExtra( "SCAN_RESULT" ) + "\"." );
			}
			else if ( resultCode == Activity.RESULT_CANCELED )
			{
				Log.d( TAG, "Barcode scan has been canceled." );
			}
			else
			{
				Log.d( TAG, "Barcode scan failed to return data for an unknown reason. Check logs." );
			}
		}
	}
	
	public class updateUI extends AsyncTask<Void, PostProcessing, Void>
	{
		String lastState = "";
		
		@Override
		protected void onProgressUpdate( PostProcessing... values )
		{
			try
			{
				for ( PostProcessing p : values )
				{
					if ( p.action == ActionList.TOAST )
					{
						Toast.makeText( getApplicationContext(), p.payload, Toast.LENGTH_LONG ).show();
					}
					else if ( p.action == ActionList.UPDATEUI )
					{
						YamlConfiguration conf = configHandler.getConfig();
						
						applyHeaderImage( conf.getString( "device.img", null ) );
						titlev.setText( conf.getString( "device.title", "Bloomin' Rewards" ) );
						address.setText( conf.getString( "device.address1", "" ) + ", " + conf.getString( "device.address2", "" ) );
						
						deviceState.setText( getStateString() );
						Toast.makeText( LaunchActivity.context, "The UI has been updated!", Toast.LENGTH_LONG ).show();
						
						uiNeedsUpdating = false;
						lastState = Common.md5( getStateString() );
					}
				}
			}
			catch ( Exception e )
			{
				Log.w( TAG, "Exception Thrown in the UI Updater: " + e.getMessage() );
			}
		}
		
		@Override
		protected Void doInBackground( Void... noparams )
		{
			do
			{
				for ( String s : cookingToast )
				{
					publishProgress( new PostProcessing( ActionList.TOAST, s ) );
					cookingToast.remove( s );
				}
				
				if ( uiNeedsUpdating || !lastState.equals( Common.md5( getStateString() ) ) )
					publishProgress( new PostProcessing( ActionList.UPDATEUI, null ) );
				
				SystemClock.sleep( updateUIInterval );
			}
			while ( true );
		}
		
		protected String getStateString()
		{
			return "State: " + tcpHandler.getDeviceState() + "  Network: " + tcpHandler.getNetworkState() + "  UUID: " + tcpHandler.getUUID();
		}
	}
	
	private class ScreenReceiver extends BroadcastReceiver
	{
		protected ScreenReceiver()
		{
			IntentFilter filter = new IntentFilter();
			filter.addAction( Intent.ACTION_SCREEN_ON );
			filter.addAction( Intent.ACTION_SCREEN_OFF );
			registerReceiver( this, filter );
		}
		
		@Override
		public void onReceive( Context context, Intent intent )
		{
			if ( intent.getAction().equals( Intent.ACTION_SCREEN_OFF ) )
			{
				Log.w( TAG, "Screen has been turned off." );
				
				PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
				@SuppressWarnings( "deprecation" )
				PowerManager.WakeLock wl = pm.newWakeLock( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag" );
				wl.acquire();
				wl.release();
				
			}
			else if ( intent.getAction().equals( Intent.ACTION_SCREEN_ON ) )
			{
				Log.w( TAG, "Screen has been turned on." );
			}
		}
	}
	
	public static ConfigHandler getConfigHandler()
	{
		return configHandler;
	}
	
	public static void cookToast( String msg )
	{
		cookingToast.add( msg );
	}
	
	public static NetworkHandler getTcpHandler()
	{
		return tcpHandler;
	}
}
