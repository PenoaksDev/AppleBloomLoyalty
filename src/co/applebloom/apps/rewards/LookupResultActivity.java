package co.applebloom.apps.rewards;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

import com.pushlink.android.PushLink;

public class LookupResultActivity extends TimedActivity implements OnClickListener, OnTouchListener
{
	private Button back;
	public static LookupResultActivity instance = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
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
		
        setContentView(R.layout.result);
        
        TableLayout parent = (TableLayout) findViewById(R.id.parent);
        parent.setOnTouchListener(this);
        
        Intent intent = getIntent();
        String message = intent.getStringExtra("json");
        JSONObj jObj = JSONObj.emptyObj() ;
        
        try
        {
			jObj = new JSONObj(message);
			
			TextView address1 = (TextView) findViewById(R.id.address1);
			TextView address2 = (TextView) findViewById(R.id.address2);
			TextView title = (TextView) findViewById(R.id.title);
			
			address1.setText( MainActivity.instance.deviceTitle );
			address2.setText( MainActivity.instance.deviceAddress1 );
			title.setText( MainActivity.instance.deviceAddress2 );
			
	        TextView balLabel = (TextView) findViewById(R.id.bal);
	        balLabel.setText( jObj.getString("pointBalance") );
	        
	        TextView nameLabel = (TextView) findViewById(R.id.name);
	        nameLabel.setText( jObj.getString("name") );
	        
	        TextView errLabel = (TextView) findViewById(R.id.err);
	        
	        if ( !jObj.getString("warn").isEmpty() )
	        {
	        	errLabel.setText(jObj.getString("warn"));
	        	errLabel.setVisibility(View.VISIBLE);
	        }
	        else
	        {
	        	errLabel.setVisibility(View.GONE);
	        }
	        
	        TextView msgLabel = (TextView) findViewById(R.id.msg);
	        
	        if ( !jObj.getString("msg").isEmpty() )
	        {
	        	msgLabel.setText(jObj.getString("msg"));
	        	msgLabel.setVisibility(View.VISIBLE);
	        }
	        else
	        {
	        	msgLabel.setVisibility(View.GONE);
	        }
		}
        catch (JSONException e)
        {
			e.printStackTrace();
			PushLink.sendAsyncException(e);
			finish();
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
        
        back = (Button) findViewById(R.id.back);
        back.setOnClickListener(this);
        
        final String user = jObj.getStringSafe("mobile_no");
        ListView lv = (ListView) findViewById( R.id.redemption );
        lv.setItemsCanFocus(false);
        
        ArrayList<HashMap<String, String>> listc = new ArrayList<HashMap<String, String>>();
        
        try
        {
        	JSONObj json = JSONObj.getFromUrl("http://api.applebloom.co/loyalty/redemption.json");
            
        	JSONArray list = json.getJSONArray("list");
        	
        	for(int i = 0; i < list.length(); i++){
                JSONObject c = list.getJSONObject(i);
 
                String redeemID = c.getString("redeemID");
                String title = c.getString("title");
                int cost = c.getInt("cost");
                
                HashMap<String, String> map = new HashMap<String, String>();
 
                map.put("redeemID", redeemID);
                map.put("title", title);
                map.put("required", "This reward requires " + cost + " points.");
                
                if ( jObj.getInt("pointBalance") >= cost )
                {
                	map.put("button", "Redeem This");
                }
                else
                {
                	map.put("button", ( cost - jObj.getInt("pointBalance") ) + " more needed");
                }
 
                listc.add(map);
            }
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	PushLink.sendAsyncException(e);
        }
        
        ListAdapter adapter = new SimpleAdapter(this, listc, R.layout.list_item, new String[] { "title" , "required", "button", "redeemID" }, new int[] { R.id.title, R.id.required, R.id.button, R.id.redeemID });
        
        lv.setAdapter(adapter);
        
        lv.setOnItemClickListener(new OnItemClickListener()
        {
			@Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
            {
            	String button = ((TextView) view.findViewById(R.id.button)).getText().toString();
            	
            	if ( button.equals("Redeem This") )
            	{
            		new AlertDialog.Builder(LookupResultActivity.this)
                    .setMessage("Are you sure you want to redeem this reward?\nOnce you do, the points will be deducted and a employeee will need to be notifieed ASAP.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	String redeemID = ((TextView) view.findViewById(R.id.redeemID)).getText().toString();
                        	
                        	instance.onTouch();
                        	instance.pauseIdleTimer();
                        	
                    		Intent in = new Intent(getApplicationContext(), ClaimReward.class);
                            in.putExtra("redeemID", redeemID);
                            in.putExtra("user", user);
                            startActivity(in);
                        }
                    }).setNegativeButton("No", null)
                    .show();
            	}
            	else
            	{
            		new AlertDialog.Builder(LookupResultActivity.this)
                	.setMessage("Sorry, You don't have enough points to claim this reward.")
                	.setPositiveButton("Ok", null)
                	.show();
            	}
            }
        });
        
        super.startIdleTimer();
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
		if ( back == v )
		{
			setResult(Activity.RESULT_OK);
			finish();
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