package co.applebloom.apps.rewards;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.os.IBinder;
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
import co.applebloom.api.WebSocketService;
import co.applebloom.apps.scanner.ScannerActivity;

import com.chiorichan.android.JSONObj;
import com.chiorichan.android.MyLittleDB;
import com.chiorichan.android.SplashView;
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
	private TextView titlev, address, version, uuid, phone, deviceState;
	private boolean continueAllowed = false;
	private ImageButton scan, back;
	public WebSocketService s;
	
	private static final String TAG = "ABRewards";
	private static LaunchActivity instance;
	private static ImageView headerImage;
	private static Context context;
	
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
	}
	
	public void launchMainView( LaunchActivity mainThis, Bundle savedInstanceState )
	{
		setContentView( R.layout.home );
		
		sharedPrefs = getSharedPreferences( "AppleBloomRewards", 0 );
		
		headerImage = (ImageView) findViewById( R.id.headerImage );
		
		uuid = (TextView) findViewById( R.id.uuid );
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
			version.setText( "Apple Bloom Rewards Version " + LaunchActivity.appVersion );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			sendException( e );
		}
		
		uuid.setText( "Device UUID: " + WebSocketService.deviceUUID );
		deviceState.setText( "Device State: " + WebSocketService.deviceState );
		
		applyHeaderImage( sharedPrefs.getString( "img", null ) );
		titlev.setText( sharedPrefs.getString( "title", "Apple Bloom Rewards" ) );
		address.setText( sharedPrefs.getString( "address1", "" ) + ", " + sharedPrefs.getString( "address2", "" ) );
		
		Intent intent = new Intent( this, WebSocketService.class );
		startService( intent );
		bindService( intent, mConnection, Context.BIND_AUTO_CREATE );
		
		new updateUI().execute();
		new ScreenReceiver();
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
	
	/**
	 * Send a thrown exception to the Apple Bloom Websocket
	 * 
	 * @param e
	 */
	public static void sendException( Exception e )
	{
		getInstance().s.sendException( e );
	}
	
	public static WebSocketService getSocketService()
	{
		try
		{
			return LaunchActivity.getInstance().s;
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	public static void startPushLink( String DeviceUUID )
	{
		PushLink.start( getAppContext(), R.drawable.ic_launcher, "3vcnlaneunf3k0k0", DeviceUUID );
		
		PushLink.setCurrentStrategy( StrategyEnum.FRIENDLY_POPUP );
		PushLink.addMetadata( "App Version", appVersion );
		PushLink.addMetadata( "Android Version", Build.VERSION.RELEASE );
		
		FriendlyPopUpStrategy fps = (FriendlyPopUpStrategy) PushLink.getCurrentStrategy();
		fps.setPopUpMessage( "New critical bugfixes are now available. Please update." );
		fps.setNotNowButton( "Later" );
		fps.setUpdateButton( "Update Now" );
		fps.setReminderTimeInSeconds( 60 * 15 );
	}
	
	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected( ComponentName className, IBinder binder )
		{
			s = ( (WebSocketService.MyBinder) binder ).getService();
		}
		
		public void onServiceDisconnected( ComponentName className )
		{
			s = null;
		}
	};
	
	public void applyHeaderImage( String filename )
	{
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
				String result = processNumber( s );
				
				if ( result == null )
				{
					new AlertDialog.Builder( this ).setMessage( "Sorry, We could not process your request at this time. Please try again later." ).setPositiveButton( "Ok", null ).show();
				}
				else if ( result.equalsIgnoreCase( "success" ) || result.equalsIgnoreCase( "firstTime" ) )
				{
					phone.setText( "" );
				}
				else
				{
					new AlertDialog.Builder( this ).setMessage( result ).setPositiveButton( "Ok", null ).show();
				}
			}
			else
			{
				new AlertDialog.Builder( this ).setMessage( "You must enter the full 10 digit mobile #\n w/o country code (i.e. 7085296564)" ).setPositiveButton( "Ok", null ).show();
			}
		}
		
		testLength();
	}
	
	public String processNumber( String phoneNumber )
	{
		if ( !CommonUtils.isNumeric( phoneNumber ) )
			return "It seems there is a problem with the Phone Number you provided. Try again.";
		
		if ( phoneNumber.startsWith( "0" ) )
			return "According to the US Phone Number formatting rules, You do not exist.\nSo, I'm sorry but I will have to disallow the use of this number.";
		
		if ( phoneNumber.startsWith( "555" ) )
			return "What do you think this is, a movie?\nPlease enter your real mobile number.";
		
		if ( phoneNumber == "1231231234" || phoneNumber == "0000000000" || phoneNumber == "1234567890" )
			return "Sorry, The number you entered has been blacklisted.";
		
		if ( phoneNumber == "7085296564" )
			return "Sorry, Being the service provider it would not be right of us to earn points. So please don't use our phone number.";
		
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
			Intent intent = new Intent( getApplicationContext(), FirstTimeActivity.class );
			intent.putExtra( "com.applebloom.apps.phoneNumber", phoneNumber );
			startActivity( intent );
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
				if ( s != null )
					s.sendMessageSync( "ACCT " + jsn.toString() );
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
		PushLink.setCurrentPopUpTarget( this );
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
					processNumber( result );
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
	
	public class updateUI extends AsyncTask<Void, String, Void>
	{
		@Override
		protected void onProgressUpdate( String... values )
		{
			if ( !deviceState.getText().toString().equals( WebSocketService.deviceState ) )
				deviceState.setText( "Device State: " + WebSocketService.deviceState );
			
			if ( !uuid.getText().toString().equals( WebSocketService.deviceUUID ) )
				uuid.setText( "Device UUID: " + WebSocketService.deviceUUID );
			
			if ( !titlev.getText().toString().equals( sharedPrefs.getString( "title", "Apple Bloom Rewards" ) ) )
			{
				applyHeaderImage( sharedPrefs.getString( "img", null ) );
				titlev.setText( sharedPrefs.getString( "title", "Apple Bloom Rewards" ) );
				address.setText( sharedPrefs.getString( "address1", "" ) + ", " + sharedPrefs.getString( "address2", "" ) );
				
				Toast.makeText( LaunchActivity.context, "The UI has been updated! :D", Toast.LENGTH_LONG ).show();
			}
		}
		
		@Override
		protected Void doInBackground( Void... noparams )
		{
			do
			{
				publishProgress( null );
				SystemClock.sleep( 5000 );
			}
			while ( true );
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
}
