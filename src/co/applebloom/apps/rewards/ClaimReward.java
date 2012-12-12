package co.applebloom.apps.rewards;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.pushlink.android.PushLink;

public class ClaimReward extends Activity implements OnClickListener
{
	Button finish = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); //Clean FLAG
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
        setContentView(R.layout.claim);
        
        finish = (Button) findViewById(R.id.finish);
        finish.setOnClickListener(this);
        
        Intent intent = getIntent();
        String redeemID = intent.getStringExtra("redeemID");
        String user = intent.getStringExtra("user");
        
        try
        {
        	JSONObj json = JSONObj.getFromUrl("http://api.applebloom.co/loyalty/claim.json?mobile_no=" + user + "&redeemID=" + redeemID);
        	
        	TextView phone = (TextView) findViewById(R.id.phone);
        	TextView points = (TextView) findViewById(R.id.points);
        	TextView reward = (TextView) findViewById(R.id.reward);
        	
        	phone.setText( "Phone Number: " + json.getString("formattedPhone") );
        	points.setText( "Remaining Points: " + json.getString("balance") );
        	reward.setText( "Redeemed Reward: " + json.getString("title") );
        }
        catch ( Exception e )
        {
        	PushLink.sendAsyncException(e);
        	e.printStackTrace();
        	finish();
        	overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
	}

	@Override
	public void onClick(View v)
	{
		if ( v == finish )
		{
			finish();
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
	}
}
