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

import android.util.Log;
import co.applebloom.apps.rewards.LaunchActivity;

public class HTTPParser
{
    static String result = "";
    static Exception exception = null;
    static String TAG = "HTTPParser";
    static CookieStore cookieStore = new BasicCookieStore();
    
    public static String getFromUrl(String url) throws Exception
    {
    	return getFromUrl(url, null);
    }
    
	public static String getFromUrl(String url, String def) throws Exception
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
						
						// TODO: Renable.
						//arguments += "uuid=" + LaunchActivity.uuid;
						
						Log.v(TAG, "Getting DATA from URL \"" + httpURL + "\" with params \"" + arguments + "\"");
						
						DefaultHttpClient httpclient = new DefaultHttpClient();
						HttpPost post = new HttpPost(httpURL);
						
						StringEntity params = new StringEntity(arguments);
						post.setHeader("Content-Type", "application/x-www-form-urlencoded");
						post.setEntity(params);
						
						String response = EntityUtils.toString( httpclient.execute( post, localContext ).getEntity(), "UTF-8" );
						
						HTTPParser.result = response;
			        }
					catch (Exception e)
					{
			            HTTPParser.exception = e;
			        }	
				}
		    });
			
			result = null;
			exception = null;
			trd.start();
			
			trd.join(5000);
			
			if ( exception != null )
				throw exception;
			
			if ( result != null )
				Log.v(TAG, "Got Data: " + result);
			
			if ( (result == null || result.isEmpty()) && def != null )
				return def;
			
			return result;
		}
		catch (Exception e)
		{
			if ( def != null )
				return def;
			
			throw e;
		}
    }
}