package com.chiorichan.android;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import co.applebloom.apps.rewards.LaunchActivity;

import com.pushlink.android.PushLink;

public class JSONObj extends JSONObject
{
    static String result = "";
    static Exception exception = null;
    static String TAG = "JSONParser";
    static CookieStore cookieStore = new BasicCookieStore();
	
	public JSONObj (String str) throws JSONException
	{
		super( str );
	}
	
	public JSONObj (JSONObject json) throws JSONException
	{
		super( json.toString() );
	}
	
	public JSONObj () throws JSONException
	{
		super();
	}
	
	public static JSONObj convertObj ( JSONObject json )
	{
		try
		{
			return new JSONObj( json.toString() );
		}
		catch ( Exception e )
		{
			PushLink.sendAsyncException(e);
			e.printStackTrace();
			return null;
		}
	}
	
	public static JSONObj emptyObj ()
	{
		try
		{
			return new JSONObj();
		}
		catch ( Exception e )
		{
			PushLink.sendAsyncException(e);
			e.printStackTrace();
			return null;
		}
	}
	
	public static JSONObj newObj(String json)
	{
		try
		{
			return new JSONObj(json);
		}
		catch ( Exception e )
		{
			return emptyObj();
		}
	}
	
	public boolean getBoolean (String keyName, Boolean def) throws Exception
	{
		try
		{
			return super.getBoolean(keyName);
		}
		catch ( JSONException e )
		{
			return def;
		}
		catch ( Exception e )
		{
			throw e;
		}
	}
	
	public boolean getBooleanSafe(String keyName)
	{
		try
		{
			return getBoolean(keyName);
		}
		catch (Exception e) 
		{
			return false;
		}
	}
	
	public String getString(String keyName, String def)
	{
		try
		{
			return super.getString(keyName);
		}
		catch ( JSONException e )
		{
			return def;
		}
	}
	
	public Long getLong(String keyName, Long def)
	{
		try
		{
			return super.getLong(keyName);
		}
		catch ( JSONException e )
		{
			return def;
		}
	}
	
	public String getStringSafe(String keyName)
	{
		try
		{
			return super.getString(keyName);
		}
		catch ( Exception e )
		{
			return "";
		}
	}
    
	public static JSONObj getFromUrlSafe(String url)
	{
		try
		{
			return getFromUrl(url);
		}
		catch (Exception e)
		{
			PushLink.sendAsyncException(e);
			try
			{
				return new JSONObj();
			}
			catch (JSONException e1)
			{
				e1.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static JSONObj getFromUrl(String url) throws Exception
    {
		try
		{
			final String urlPath = url;
			
			Thread trd = new Thread(new Runnable()
		    {
				@Override
				public void run()
				{
					try
					{
						HttpContext localContext = new BasicHttpContext();
						localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
						
						String arguments = "";
						String httpURL = urlPath.trim();
						
						if( httpURL.contains("?") )
						{
							arguments = httpURL.substring( httpURL.indexOf("?") + 1 );
							httpURL = httpURL.substring( 0, httpURL.indexOf("?") );
						}
						
						if ( !arguments.isEmpty() )
							arguments += "&";
						
						// TODO: Renable
						// arguments += "uuid=" + LaunchActivity.uuid;
						
						Log.v(TAG, "Getting JSON from URL \"" + httpURL + "\" with params \"" + arguments + "\"");
						
						DefaultHttpClient httpclient = new DefaultHttpClient();
						HttpPost post = new HttpPost(httpURL);
						
						StringEntity params = new StringEntity(arguments);
						post.setHeader("Content-Type", "application/x-www-form-urlencoded");
						post.setEntity(params);
						
						String response = EntityUtils.toString( httpclient.execute( post, localContext ).getEntity(), "UTF-8" );
						
						JSONObj.result = response;
			        }
					catch (Exception e)
					{
			            JSONObj.exception = e;
			        }	
				}
		    });
			
			result = null;
			exception = null;
			trd.start();
			
			trd.join(5000);
			
			if ( exception != null )
				throw exception;
			
			if ( result == null )
			{
				return new JSONObj();
			}
			else
			{
				Log.v(TAG, "Got Data: " + result);
				return new JSONObj(result);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
    }
}
