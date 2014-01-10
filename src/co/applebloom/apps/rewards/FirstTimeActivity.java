package co.applebloom.apps.rewards;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class FirstTimeActivity extends TimedActivity implements OnClickListener, OnTouchListener
{
	private static final int TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 0x0000020;
	private Button finish;
	private Button cancel;
	private String phoneNumber;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN ); // Clean FLAG
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );
		
		setContentView( R.layout.firsttime );
		
		TableLayout parent = (TableLayout) findViewById( R.id.parent );
		parent.setOnTouchListener( this );
		
		finish = (Button) findViewById( R.id.finish );
		cancel = (Button) findViewById( R.id.cancel );
		finish.setOnClickListener( this );
		cancel.setOnClickListener( this );
		
		Intent intent = getIntent();
		phoneNumber = intent.getStringExtra( "com.applebloom.apps.phoneNumber" );
		
		if ( phoneNumber == null )
			finish();
		
		TextView nameLabel = (TextView) findViewById( R.id.name );
		String name = formatPhoneNumber( phoneNumber );
		nameLabel.setText( "Welcome, " + name );
		
		CheckBox sms_check = (CheckBox) findViewById( R.id.text );
		
		if ( LaunchActivity.getPrefBoolean( "sms_disabled" ) )
			sms_check.setVisibility( 0 );
		
		startIdleTimer();
		
		EditText email = (EditText) findViewById( R.id.email );
		email.setInputType( TYPE_TEXT_VARIATION_EMAIL_ADDRESS );
		email.addTextChangedListener( new TextWatcher()
		{
			public void afterTextChanged( Editable s )
			{
				onTouch();
				
				if ( s.length() > 0 )
				{
					finish.setText( "Continue" );
				}
				else
				{
					finish.setText( "Skip E-Mail" );
				}
			}
			
			public void beforeTextChanged( CharSequence s, int start, int count, int after )
			{
			}
			
			public void onTextChanged( CharSequence s, int start, int before, int count )
			{
			}
		} );
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
		if ( v == finish )
		{
			CheckBox a = (CheckBox) findViewById( R.id.agreement );
			
			if ( !a.isChecked() )
			{
				new AlertDialog.Builder( this ).setMessage( "You must agree to the terms of service." ).setPositiveButton( "Ok", null ).show();
				return;
			}
			
			CheckBox b = (CheckBox) findViewById( R.id.text );
			
			if ( b.isChecked() )
			{
				//LaunchActivity.getInstance().s.sendMessageSync( "TXT " + phoneNumber );
			}
			
			SQLiteDatabase db = LaunchActivity.myLittleDB.getWritableDatabase();
			
			String email = ( (EditText) findViewById( R.id.email ) ).getText().toString();
			
			if ( email == null )
				email = "";
			
			if ( email.length() > 0 && !android.util.Patterns.EMAIL_ADDRESS.matcher( email ).matches() )
			{
				new AlertDialog.Builder( this ).setMessage( "It seems the email address you entered in invalid. Please double check it and try again." ).setPositiveButton( "Ok", null ).show();
				return;
			}
			
			String name = formatPhoneNumber( phoneNumber );
			ContentValues insert = new ContentValues();
			
			insert.put( "id", phoneNumber );
			insert.put( "name", name );
			insert.put( "email", email );
			insert.put( "first_added", System.currentTimeMillis() );
			insert.put( "balance", 0 );
			insert.put( "last_instore_check", 0 );
			
			Log.e( "REWARDS", insert.toString() );
			
			db.insert( "users", "id", insert );
			db.close();
			
			//LaunchActivity.getInstance().processNumber( phoneNumber );
			finish();
		}
		else if ( v == cancel )
		{
			// setResult(Activity.RESULT_CANCELED);
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
