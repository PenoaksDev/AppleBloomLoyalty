package co.applebloom.apps.rewards;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class TimedActivity extends Activity
{
    //member variables
    private Timer mTimerSeconds;
    private int mIntIdleSeconds;
    private boolean mBoolInitialized = false;
    private boolean mBoolRunning = false;
    private int MAX_IDLE_TIME_SECONDS = 45;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        if (mTimerSeconds != null)
        {
            mTimerSeconds.cancel();
        }
        super.onDestroy();
    }

    public void onTouch()
    {
        mIntIdleSeconds=0;
    }

    public void pauseIdleTimer ()
    {
    	mBoolRunning = false;
    }
    
    public void resumeIdleTimer ()
    {
    	mBoolRunning = true;
    }
    
    public void startIdleTimer ()
    {
        if (mBoolInitialized == false)
        {
            mBoolInitialized = true;

            //initialize idle counter
            mIntIdleSeconds=0;

            //create timer to tick every second
            mTimerSeconds = new Timer();
            mTimerSeconds.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    timerSecondsCounter();
                }
            }, 0, 1000);
        }
    }

    private void timerSecondsCounter()
    {
    	if ( mBoolRunning )
    	{
            mIntIdleSeconds++;
            
            if ( (MAX_IDLE_TIME_SECONDS - mIntIdleSeconds) < 10 )
            	Log.v("AppleBloomRewards", "This activity will finish in " + (MAX_IDLE_TIME_SECONDS - mIntIdleSeconds) +  " seconds.");

            if (mIntIdleSeconds >= MAX_IDLE_TIME_SECONDS)
            {
            	setResult(Activity.RESULT_CANCELED);
            	finish();
            }
    	}
    }
}