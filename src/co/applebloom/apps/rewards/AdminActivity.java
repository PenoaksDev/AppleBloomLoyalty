package co.applebloom.apps.rewards;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import co.applebloom.api.WebSocketService;

import com.chiorichan.utils.Reversed;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class AdminActivity extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );
		
		setContentView( R.layout.admin );
		
		Button settings = (Button) findViewById( R.id.settings );
		settings.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
				Intent intent = new Intent( android.provider.Settings.ACTION_SETTINGS );
				intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
				startActivity( intent );
			}
		} );
		
		Button reboot = (Button) findViewById( R.id.reboot );
		reboot.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
				CommonUtils.restartDevice();
			}
		} );
		
		Button done = (Button) findViewById( R.id.done );
		done.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
				finish();
			}
		} );
		
		// App Information
		TextView version = (TextView) findViewById( R.id.version );
		version.setText( "Apple Bloom Rewards Version " + LaunchActivity.appVersion );
		
		new logCatReader().execute();
	}
	
	public void forceRedeemClick( View v )
	{
		WebSocketService.requestRedeemables( true );
	}
	
	public void forceAcctClick( View v )
	{
		WebSocketService.syncAccounts();
	}
	
	public void sayHelloClick( View v )
	{
		WebSocketService.register( true );
	}
	
	public void requestNewClick( View v )
	{
		SharedPreferences.Editor editor = LaunchActivity.sharedPrefs.edit();
		editor.putString( "uuid", null );
		editor.commit();
		
		WebSocketService.deviceUUID = null;
		WebSocketService.register( true );
	}
	
	public class logCatReader extends AsyncTask<Void, String, Void>
	{
		@Override
		protected void onProgressUpdate( String... values )
		{
			TextView tv = (TextView) findViewById( R.id.logcat );
			String str = tv.getText().toString();
			
			for ( String v : values )
			{
				str += "\n" + v;
			}
			
			if ( str.length() > 2000 )
				str = str.substring( str.length() - 2000 );
			
			tv.setText( str );
			
			String uuid = WebSocketService.deviceUUID;
			String state = WebSocketService.deviceState;
			
			try
			{
				( (TextView) findViewById( R.id.uuid ) ).setText( "Device UUID: " + ( ( uuid == null || uuid == "" ) ? "{NULL?}" : uuid ) );
				( (TextView) findViewById( R.id.msg ) ).setText( "Device State: " + ( ( state == null || state == "" ) ? "{NULL?}" : state ) );
			}
			catch ( NullPointerException e )
			{
				LaunchActivity.sendException( e );
			}
		}
		
		@Override
		protected Void doInBackground( Void... noparams )
		{
			try
			{
				Process process = Runtime.getRuntime().exec( "logcat" );
				BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
				
				String line;
				while ( ( line = bufferedReader.readLine() ) != null )
				{
					publishProgress( line );
				}
			}
			catch ( IOException e )
			{
				LaunchActivity.sendException( e );
			}
			
			return null;
		}
	}
	
}
