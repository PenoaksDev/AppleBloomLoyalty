package co.applebloom.apps.rewards;

import static co.applebloom.api.CommonUtils.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HeartBeat
{
	private static List<SystemTimerListener> clockListener = new ArrayList<SystemTimerListener>();
	private final Timer clockTimer;
	private final long FPS = 15;
	
	public HeartBeat()
	{
		clockTimer = new Timer();
		clockTimer.scheduleAtFixedRate( new Task() , 1000, 1000 * FPS );
		
		Log.v( TAG, "Application Heart Beat Started!" );
	}
	
	private class Task extends TimerTask
	{
		public void run()
		{
			timerHandler.sendEmptyMessage( 0 );
		}
	}
	
	private final Handler timerHandler = new Handler()
	{
		public void handleMessage( Message msg )
		{
			// runs in context of the main thread
			timerSignal();
		}
	};
	
	private void timerSignal()
	{
		for ( SystemTimerListener listener : clockListener )
		{
			Log.d( TAG, "Executing Listener: " + listener.getClass() );
			listener.onSystemTimeSignal();
		}
	}
	
	public void killTimer()
	{
		clockTimer.cancel();
	}
	
	public Boolean addListener( SystemTimerListener listener )
	{
		if ( clockListener.contains( listener ) )
			return false;
		
		clockListener.add( listener );
		return true;
	}
	
	public static Boolean removeListener( SystemTimerListener listener )
	{
		if ( clockListener.contains( listener ) )
		{
			clockListener.remove( listener );
			return true;
		}
		
		return false;
	}
}
