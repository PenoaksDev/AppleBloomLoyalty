package co.applebloom.apps.rewards;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.chiorichan.android.MyLittleDB;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.pushlink.android.PushLink;

public class CommonUtils
{
	static final String TAG = "Rewards";
	static final String EXTRA_MESSAGE = "message";
	static final String DISPLAY_MESSAGE_ACTION = "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";
	
	public static void displayMessage( Context context, String message )
	{
		// TODO: Used for basic popup messages.
		Intent intent = new Intent( DISPLAY_MESSAGE_ACTION );
		intent.putExtra( EXTRA_MESSAGE, message );
		context.sendBroadcast( intent );
	}
	
	public static void restartDevice()
	{
		try
		{
			doCmds( "reboot" );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			PushLink.sendAsyncException( e );
		}
	}
	
	public static void doCmds( String cmd ) throws Exception
	{
		Process process = Runtime.getRuntime().exec( "su" );
		DataOutputStream os = new DataOutputStream( process.getOutputStream() );
		// DataInputStream is = new DataInputStream(process.getInputStream());
		
		BufferedReader is = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
		
		Log.v( TAG, "Sending Command to System" );
		
		/*
		 * for (String tmpCmd : cmds) { os.writeBytes(tmpCmd+"\n"); }
		 */
		
		os.writeBytes( cmd + "\n" );
		
		os.writeBytes( "exit\n" );
		
		String line;
		while ( ( line = is.readLine() ) != null )
		{
			System.out.println( line );
			Log.v( TAG, "Command Result: " + line );
		}
		
		os.flush();
		os.close();
		is.close();
		
		process.waitFor();
	}
	
	public static String formatPhoneNumber( String rawPhoneNumber )
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
	
	public static boolean isNumeric( String value )
	{
		try
		{
			double dd = Double.parseDouble( value );
		}
		catch ( NumberFormatException nfe )
		{
			return false;
		}
		
		return true;
	}
	
	public static boolean haveNetworkConnection()
	{
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;
		boolean haveConnectedEthernet = false;
		
		ConnectivityManager cm = (ConnectivityManager) LaunchActivity.getInstance().getSystemService( Context.CONNECTIVITY_SERVICE );
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for ( NetworkInfo ni : netInfo )
		{
			if ( ni.getTypeName().equalsIgnoreCase( "WIFI" ) )
				if ( ni.isConnected() )
					haveConnectedWifi = true;
			if ( ni.getTypeName().equalsIgnoreCase( "MOBILE" ) )
				if ( ni.isConnected() )
					haveConnectedMobile = true;
			if ( ni.getTypeName().equalsIgnoreCase( "ETHERNET" ) || ni.getTypeName().equalsIgnoreCase( "ETH" ) )
				if ( ni.isConnected() )
					haveConnectedEthernet = true;
			
			// if (ni.isConnected())
			// Log.d(TAG, "We found an active internet connection using: " +
			// ni.getTypeName());
		}
		
		return haveConnectedWifi || haveConnectedMobile || haveConnectedEthernet;
	}
}
