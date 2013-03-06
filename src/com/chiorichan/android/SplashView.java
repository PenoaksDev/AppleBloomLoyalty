package com.chiorichan.android;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import co.applebloom.apps.rewards.R;

public class SplashView extends LinearLayout
{
	public interface SplashEvents
	{
		// This event is signaled after the splash and all of its foal views,
		// if any, have been drawn.
		// As currently implemented, it will trigger BEFORE any scrollbars are drawn.
		// We are assuming that there will BE no scrollbars on a SplashView.
		public void onSplashDrawComplete();
	}
	
	private SplashEvents splashEventHandler = null;
	
	public void setSplashEventHandler( SplashEvents splashEventHandler )
	{
		this.splashEventHandler = splashEventHandler;
	}
	
	private void fireSplashDrawCompleteEvent()
	{
		if ( this.splashEventHandler != null )
		{
			this.splashEventHandler.onSplashDrawComplete();
		}
	}
	
	public SplashView(Context context)
	{
		super( context );
		
		// This is set by default for a LinearLayout, but just making sure!
		this.setWillNotDraw( true );
		
		// If the cache is not enabled, then I think that helps to ensure that
		// dispatchDraw override WILL
		// get called. Because if caching were enabled, then the
		// drawing might not occur.
		this.setDrawingCacheEnabled( false );
		
		setGravity( Gravity.CENTER );
		
		// This splices in your XML definition (see below) to the SplashView layout
		LayoutInflater.from( context ).inflate( R.layout.splashhome, this, true );
	}
	
	@Override
	protected void dispatchDraw( Canvas canvas )
	{
		// Draw any foal views
		super.dispatchDraw( canvas );
		
		// Signal client objects (in this case, your mane activity) that
		// we have finished initializing and drawing the view.
		fireSplashDrawCompleteEvent();
	}
}
