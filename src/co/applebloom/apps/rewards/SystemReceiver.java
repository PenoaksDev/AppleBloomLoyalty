package co.applebloom.apps.rewards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.chiorichan.net.SocketService;

public class SystemReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent )
	{
		SocketService.send( "SYS", intent.getAction() );
	}
}
