package co.applebloom.apps.rewards;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.chiorichan.android.JSONObj;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.pushlink.android.PushLink;

public class homeActivity extends Activity implements OnClickListener, OnLongClickListener
{
	private TextView phone;
	private Button num0, num1, num2, num3, num4, num5, num6, num7, num8, num9, go;
	private ImageButton scan, back;
	private boolean continueAllowed = false;
    private static TextView titlev;
    private static TextView address;
	public static homeActivity instance = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if ( instance != null )
		{
			instance.setResult(RESULT_CANCELED);
			instance.finish();
		}
		
		instance = this;
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
        setContentView(R.layout.home);
        
        TextView version = (TextView) findViewById( R.id.version );
    	version.setText( "Apple Bloom Loyalty Version " +  MainActivity.appVersion );
        
    	TextView uuid = (TextView) findViewById( R.id.uuid );
    	uuid.setText( "Device UUID: " + MainActivity.uuid );
    	
        address = (TextView) findViewById(R.id.address);
        titlev = (TextView) findViewById(R.id.title);
        
        scan = (ImageButton) findViewById(R.id.button_scan);
        back = (ImageButton) findViewById(R.id.button_back);
        
        phone = (TextView) findViewById(R.id.phone);
        num0 = (Button) findViewById(R.id.button0);
        num1 = (Button) findViewById(R.id.button1);
        num2 = (Button) findViewById(R.id.button2);
        num3 = (Button) findViewById(R.id.button3);
        num4 = (Button) findViewById(R.id.button4);
        num5 = (Button) findViewById(R.id.button5);
        num6 = (Button) findViewById(R.id.button6);
        num7 = (Button) findViewById(R.id.button7);
        num8 = (Button) findViewById(R.id.button8);
        num9 = (Button) findViewById(R.id.button9);
        go = (Button) findViewById(R.id.gobutt);
        
        num0.setOnClickListener(this);
        num1.setOnClickListener(this);
        num2.setOnClickListener(this);
        num3.setOnClickListener(this);
        num4.setOnClickListener(this);
        num5.setOnClickListener(this);
        num6.setOnClickListener(this);
        num7.setOnClickListener(this);
        num8.setOnClickListener(this);
        num9.setOnClickListener(this);
        scan.setOnClickListener(this);
        back.setOnClickListener(this);
        back.setOnLongClickListener(this);
        go.setOnClickListener(this);
        phone.setCursorVisible(false);
        
        phone.addTextChangedListener(new TextWatcher()
        {
            public void afterTextChanged(Editable s)
            {
            	testLength();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        
        Intent intent = getIntent();
        JSONObj json = JSONObj.newObj( intent.getStringExtra("json") );
        
        if ( json.getBooleanSafe("locationAssigned") )
		{
			String jTitle = json.getStringSafe("title");
			String jAddress1 = json.getStringSafe("address1");
			String jAddress2 = json.getStringSafe("address2");
			
			titlev.setText( jTitle );
			address.setText( jAddress1 + ", " + jAddress2 );
			
			String imgUrl = json.getStringSafe( "img" );
			ImageView iv = (ImageView) findViewById( R.id.headerImage );
			
			UrlImageViewHelper.setUrlDrawable(iv, imgUrl);
		}
        else
        {
        	setResult(RESULT_CANCELED);
        	finish();
        }
        
        PushLink.setCurrentPopUpTarget(this);
	}
	
	public void onClick (View v)
	{    
		String s = phone.getText().toString();
		
		if ( num0 == v || num1 == v || num2 == v || num3 == v || num4 == v || num5 == v || num6 == v || num7 == v || num8 == v || num9 == v )
		{
			Button b = (Button) v;
			phone.setText( phone.getText().toString() + b.getText().toString() );
		}
		else if ( scan == v )
		{
			Intent intent = new Intent( getIntent().getAction() );
	        intent.putExtra("ACTION", "scan");
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
		else if ( back == v )
		{
			if ( s.length() > 0 )
				phone.setText( s.substring(0, s.length() - 1) );
		}
		else if ( go == v )
		{
			if ( continueAllowed )
			{
				Intent intent = new Intent( getIntent().getAction() );
		        intent.putExtra("MOBILE_NO", s);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
			else
			{
				new AlertDialog.Builder(this)
            	.setMessage("You must enter the full 10 digit mobile #\n w/o country code (i.e. 7085296564)")
            	.setPositiveButton("Ok", null)
            	.show();
			}
		}
		
		testLength();
	}
	
	public void testLength ()
	{
		String s = phone.getText().toString();
		
		if ( s.length() == 10 )
		{
			if ( s.substring(0, 1).equals("1") )
			{
				phone.setText( s.substring(1, s.length()) );
			}
			else
			{
				continueAllowed = true;
				
				go.setBackgroundResource(R.drawable.go_button_green);
				// Enable GO!
				
				return;
			}
		}
		
		continueAllowed = false;
		
		go.setBackgroundResource(R.drawable.go_button_gray);
	}

	public boolean onLongClick(View v)
	{
		if ( back == v )
		{
			phone.setText("");
		}
		
		return false;
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
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
}

/*
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, MENU_ADMIN, 0, "Administration")
    	.setIcon(android.R.drawable.ic_menu_edit);
    menu.add(0, MENU_REBOOT, 0, "Restart Device")
		.setIcon(android.R.drawable.ic_lock_power_off);

    return true;
    //super.onCreateOptionsMenu(menu);
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
	final EditText input = new EditText(this);
	
    switch (item.getItemId()) {
        case MENU_ADMIN:
        	new AlertDialog.Builder(this)
            .setTitle("Authorized Users Only")
            .setMessage("Please enter the password?")
            .setView(input)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	String value = input.getText().toString().trim();
                	if(value.matches("admin1"))
                    {
                		Intent intent = new Intent( android.provider.Settings.ACTION_SETTINGS);
                    	startActivity(intent);
                    }
                	else
                	{
                		new AlertDialog.Builder(homeActivity.this)
                    	.setMessage("Incorrect Login")
                    	.setPositiveButton("Ok", null)
                    	.show();
                	}
                }
            }).setNegativeButton("Cancel", null)
            .show();
        	
        	/*
        	Intent intent = new Intent( this, SettingsActivity.class );
        	startActivity( intent );
        	
            return true;
        case MENU_REBOOT: // Currently Removed
        	new AlertDialog.Builder(this)
            .setTitle("Authorized Users Only")
            .setMessage("Please enter the password?")
            .setView(input)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	String value = input.getText().toString().trim();
                	if(value.matches("admin1"))
                    {
                		//getParent().finish();
                		restartDevice();
                    }
                	else
                	{
                		new AlertDialog.Builder(homeActivity.this)
                    	.setMessage("Incorrect Login")
                    	.setPositiveButton("Ok", null)
                    	.show();
                	}
                }
            }).setNegativeButton("Cancel", null)
            .show();
        	
        	return true;
    }

    return super.onOptionsItemSelected(item);
}
*/