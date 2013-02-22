package co.applebloom.apps.rewards;

import co.applebloom.api.WebSocketService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent )
	{
		WebSocketService.send( "SYS " + intent.getAction() );
	}
}
