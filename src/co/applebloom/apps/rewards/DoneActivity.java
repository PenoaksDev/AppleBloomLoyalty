package co.applebloom.apps.rewards;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.pushlink.android.PushLink;

public class DoneActivity extends TimedActivity implements OnClickListener, OnTouchListener
{
	private Button back;
	public static DoneActivity instance = null;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		if ( instance != null )
		{
			instance.setResult( RESULT_CANCELED );
			instance.finish();
		}
		
		instance = this;
		
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN ); // Clean FLAG
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );
		
		setContentView( R.layout.result );
		
		TableLayout parent = (TableLayout) findViewById( R.id.parent );
		parent.setOnTouchListener( this );
		
		Intent intent = getIntent();
		String phoneNumber = intent.getStringExtra( "com.applebloom.apps.phoneNumber" );
		String warn = intent.getStringExtra( "com.applebloom.apps.warning" );
		String msg = intent.getStringExtra( "com.applebloom.apps.message" );
		
		TextView address1 = (TextView) findViewById( R.id.address1 );
		TextView address2 = (TextView) findViewById( R.id.address2 );
		TextView title = (TextView) findViewById( R.id.title );
		
		SharedPreferences sharedPrefs = getSharedPreferences( "AppleBloomRewards", 0 );
		
		title.setText( sharedPrefs.getString( "title", "Apple Bloom Rewards" ) );
		address1.setText( sharedPrefs.getString( "address1", "" ) );
		address2.setText( sharedPrefs.getString( "address2", "" ) );
		
		SQLiteDatabase db = LaunchActivity.myLittleDB.getReadableDatabase();
		
		Cursor cursor = db.query( "users", null, "`id` = '" + phoneNumber + "'", null, null, null, null );
		
		cursor.moveToFirst();
		String name = cursor.getString( 1 );
		String email = cursor.getString( 2 );
		int balance = cursor.getInt( 4 );
		
		TextView balLabel = (TextView) findViewById( R.id.bal );
		TextView nameLabel = (TextView) findViewById( R.id.name );
		
		if ( name == null || name.equals( "" ) || name.equalsIgnoreCase( "null" ) )
			name = formatPhoneNumber( phoneNumber );
		
		balLabel.setText( Integer.toString( balance ) );
		nameLabel.setText( "Welcome, " + name );
		
		TextView errLabel = (TextView) findViewById( R.id.err );
		
		if ( warn == null || warn.isEmpty() )
		{
			errLabel.setVisibility( View.GONE );
			
		}
		else
		{
			errLabel.setText( warn );
			errLabel.setVisibility( View.VISIBLE );
		}
		
		TextView msgLabel = (TextView) findViewById( R.id.msg );
		
		if ( msg == null || msg.isEmpty() )
		{
			msgLabel.setVisibility( View.GONE );
		}
		else
		{
			msgLabel.setText( msg );
			msgLabel.setVisibility( View.VISIBLE );
		}
		
		back = (Button) findViewById( R.id.back );
		back.setOnClickListener( this );
		
		ListView lv = (ListView) findViewById( R.id.redemption );
		lv.setItemsCanFocus( false );
		
		ArrayList<HashMap<String, String>> listc = new ArrayList<HashMap<String, String>>();
		
		try
		{
			cursor = db.query( "redeemables", null, null, null, null, null, null );
			
			for ( cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext() )
			{
				String redeemID = cursor.getString( 0 );
				String title1 = cursor.getString( 1 );
				int cost = cursor.getInt( 2 );
				
				HashMap<String, String> map = new HashMap<String, String>();
				
				map.put( "redeemID", redeemID );
				map.put( "title", title1 );
				map.put( "required", "This reward requires " + cost + " points." );
				
				if ( balance >= cost )
				{
					map.put( "button", "Redeem This" );
				}
				else
				{
					map.put( "button", ( cost - balance ) + " more needed" );
				}
				
				listc.add( map );
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			PushLink.sendAsyncException( e );
		}
		
		ListAdapter adapter = new SimpleAdapter( this, listc, R.layout.list_item, new String[] { "title", "required", "button", "redeemID" }, new int[] { R.id.title, R.id.required, R.id.button, R.id.redeemID } );
		
		lv.setAdapter( adapter );
		
		final String phoneNumberF = phoneNumber;
		
		lv.setOnItemClickListener( new OnItemClickListener()
		{
			@Override
			public void onItemClick( AdapterView<?> parent, final View view, int position, long id )
			{
				String button = ( (TextView) view.findViewById( R.id.button ) ).getText().toString();
				
				if ( button.equals( "Redeem This" ) )
				{
					onTouch();
					
					new AlertDialog.Builder( DoneActivity.this ).setMessage( "Are you sure you want to redeem this reward?\nOnce you do, the points will be deducted and a employeee will need to be notifieed ASAP." ).setPositiveButton( "Yes", new DialogInterface.OnClickListener()
					{
						public void onClick( DialogInterface dialog, int whichButton )
						{
							String redeemID = ( (TextView) view.findViewById( R.id.redeemID ) ).getText().toString();
							
							Intent in = new Intent( LaunchActivity.getAppContext(), ClaimReward.class );
							in.putExtra( "redeemID", redeemID );
							in.putExtra( "user", phoneNumberF );
							startActivity( in );
							finish();
						}
					} ).setNegativeButton( "No", null ).show();
				}
				else
				{
					onTouch();
					
					new AlertDialog.Builder( DoneActivity.this ).setMessage( "Sorry, You don't have enough points to claim this reward." ).setPositiveButton( "Ok", null ).show();
				}
			}
		} );
		
		super.startIdleTimer();
	}
	
	public void onTouch()
	{
		super.onTouch();
	}
	
	public String formatPhoneNumber( String rawPhoneNumber )
	{
		PhoneNumberUtil putil = PhoneNumberUtil.getInstance();
		try
		{
			PhoneNumber num = putil.parse( rawPhoneNumber, "US" );
			return putil.format( num, PhoneNumberFormat.NATIONAL );
		}
		catch ( NumberParseException e )
		{
			return rawPhoneNumber;
		}
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
	public void onClick( View v )
	{
		if ( back == v )
		{
			setResult( Activity.RESULT_OK );
			finish();
		}
	}
	
	@Override
	public boolean onTouch( View v, MotionEvent event )
	{
		final int actionPerformed = event.getAction();
		
		if ( actionPerformed == MotionEvent.ACTION_DOWN )
			super.onTouch();
		
		return false;
	}
}
