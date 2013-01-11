package co.applebloom.apps.rewards;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TextView;

import com.pushlink.android.PushLink;

public class FirsttimeCollection extends TimedActivity implements OnClickListener, OnTouchListener
{
	private Button finish;
	private Button cancel;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
        setContentView(R.layout.firsttime);
		
        TableLayout parent = (TableLayout) findViewById(R.id.parent);
        parent.setOnTouchListener(this);
        
        finish = (Button) findViewById(R.id.finish);
        cancel = (Button) findViewById(R.id.cancel);
        finish.setOnClickListener(this);
        cancel.setOnClickListener(this);
        
        Intent intent = getIntent();
        String message = intent.getStringExtra("json");
        JSONObject jObj = null;
        
        try {
			jObj = new JSONObject(message);
			
	        TextView nameLabel = (TextView) findViewById(R.id.name);
	        nameLabel.setText( jObj.getString("name") );
	        
		} catch (JSONException e) {
			e.printStackTrace();
			PushLink.sendAsyncException(e);
			finish();
		}
        
        startIdleTimer();
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_FOCUS
    			|| keyCode == KeyEvent.KEYCODE_CAMERA
    			|| keyCode == KeyEvent.KEYCODE_BACK
    			|| keyCode == KeyEvent.KEYCODE_POWER
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
    			|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
    		) {
    		
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);    		  
    	}
    }
	
	@Override
	public void onClick(View v)
	{
		if ( v == finish )
		{
			CheckBox a = (CheckBox) findViewById(R.id.agreement);
			
			if ( !a.isChecked() )
			{
				new AlertDialog.Builder(this)
            	.setMessage("You agree to the terms of service.")
            	.setPositiveButton("Ok", null)
            	.show();
				return;
			}
			
			CheckBox c = (CheckBox) findViewById(R.id.text);
			
			Intent intent = new Intent(getIntent().getAction());
	        intent.putExtra("TEXTING", c.isChecked());
			setResult(Activity.RESULT_OK, intent);
			finish();
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
		else if ( v == cancel )
		{
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
        final int actionPerformed = event.getAction();
        
        if (actionPerformed == MotionEvent.ACTION_DOWN)
            super.onTouch();
		
		return false;
	}
}