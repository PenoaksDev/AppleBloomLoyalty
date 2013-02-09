package co.applebloom.apps.rewards;

public class SystemTimerListener
{
	public void onSystemTimeSignal ()
	{
		//Override Me!
	}
	
	public void destroyListener ()
	{
		HeartBeat.removeListener( this );
	}
}
