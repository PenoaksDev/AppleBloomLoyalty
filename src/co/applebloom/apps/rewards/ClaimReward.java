package co.applebloom.apps.rewards;

import org.json.JSONException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.chiorichan.android.JSONObj;
import com.chiorichan.net.CommonUtils;
import com.pushlink.android.PushLink;

public class ClaimReward extends Activity implements OnClickListener
{
	Button finish = null;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN ); // Clean FLAG
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );
		
		setContentView( R.layout.claim );
		
		finish = (Button) findViewById( R.id.finish );
		finish.setOnClickListener( this );
		
		try
		{
			int balance = 0;
			// String msg = "Congrats, You have just claimed a reward!";
			String err = "Sorry, It seems that something is wrong with the reward claiming. Try again later!";
			
			Intent intent = getIntent();
			String redeemId = intent.getStringExtra( "redeemID" );
			String phoneNumber = intent.getStringExtra( "user" );
			String rtitle = "";
			int required = 0;
			
			Log.d( "APRewards", "Now Attempting To Claim Reward Id " + redeemId + " for Mobile No " + phoneNumber );
			
			SQLiteDatabase db = LaunchActivity.myLittleDB.getReadableDatabase();
			Cursor cursor = db.query( "redeemables", null, "`id` = '" + redeemId + "'", null, null, null, null );
			
			if ( cursor.getCount() > 0 )
			{
				cursor.moveToFirst();
				
				rtitle = cursor.getString( 1 );
				required = cursor.getInt( 2 );
			}
			else
			{
				// Something is not right and we must just end it now.
				finish();
				return;
			}
			
			cursor = db.query( "users", null, "`id` = '" + phoneNumber + "'", null, null, null, null );
			
			if ( cursor.getCount() > 0 )
			{
				cursor.moveToFirst();
				balance = cursor.getInt( 4 );
				
				if ( balance >= required )
				{
					balance -= required;
					err = null;
				}
				
				ContentValues update = new ContentValues();
				update.put( "balance", balance );
				update.put( "last_instore_check", System.currentTimeMillis() );
				
				db.update( "users", update, "`id` = '" + phoneNumber + "'", null );
			}
			else
			{
				// Something is not right and we must just end it now.
				finish();
				return;
			}
			
			if ( err == null )
			{
				ContentValues trans = new ContentValues();
				trans.put( "id", phoneNumber );
				trans.put( "time", System.currentTimeMillis() );
				trans.put( "n", required );
				trans.put( "p", 0 );
				trans.put( "action", "Claimed Points" );
				trans.put( "comment", "Balance: " + balance );
				SQLiteDatabase tdb = LaunchActivity.myLittleDB.getWritableDatabase();
				tdb.insert( "trans", null, trans );
				tdb.close();
				
				JSONObj jsn = null;
				try
				{
					jsn = new JSONObj("{}");
					
					jsn.put( "id", phoneNumber );
					jsn.put( "name", "" );
					jsn.put( "email", "" );
					jsn.put( "first_added", 0L );
					jsn.put( "balance", balance );
					jsn.put( "last_instore_check", 0L );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				finally
				{
					//if ( LaunchActivity.getSocketService() != null )
						//LaunchActivity.getSocketService().sendMessageSync( "ACCT " + jsn.toString() );
				}
			}
			else
			{
				Intent intent1 = new Intent( getApplicationContext(), DoneActivity.class );
				intent1.putExtra( "com.applebloom.apps.warning", err );
				intent1.putExtra( "com.applebloom.apps.phoneNumber", phoneNumber );
				startActivity( intent1 );
			}
			
			TextView phone = (TextView) findViewById( R.id.phone );
			TextView points = (TextView) findViewById( R.id.points );
			TextView reward = (TextView) findViewById( R.id.reward );
			
			phone.setText( "Phone Number: " + CommonUtils.formatPhoneNumber( phoneNumber ) );
			points.setText( "Remaining Points: " + balance );
			points.setText( "Remaining Points: " + balance );
			reward.setText( "Redeemed Reward: " + rtitle );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			//LaunchActivity.sendException( e );
		}
	}
	
	@Override
	public void onClick( View v )
	{
		if ( v == finish )
		{
			// TODO: Return to balance
			finish();
		}
	}
}
