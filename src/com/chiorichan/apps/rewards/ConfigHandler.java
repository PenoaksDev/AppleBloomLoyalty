package com.chiorichan.apps.rewards;

import static com.chiorichan.net.NetworkHandler.TAG;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import android.os.Environment;
import android.util.Log;
import co.applebloom.apps.rewards.LaunchActivity;

import com.chiorichan.configuration.MemorySection;
import com.chiorichan.configuration.file.YamlConfiguration;

public class ConfigHandler
{
	private File configFile;
	private YamlConfiguration config;
	
	public ConfigHandler()
	{
		if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) )
		{
			configFile = new File( Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ), "RewardsData.yaml" );
		}
		else
		{
			configFile = new File( LaunchActivity.getAppContext().getFilesDir(), "RewardsData.yaml" );
		}
		
		if ( configFile.exists() )
			config = YamlConfiguration.loadConfiguration( configFile );
		else
		{
			config = new YamlConfiguration();
			try
			{
				configFile.createNewFile();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		
		Log.i( TAG, "Accessing configuration file from location '" + configFile.getAbsolutePath() + "'" );
	}
	
	public YamlConfiguration getConfig()
	{
		return config;
	}
	
	public void pushChanges( YamlConfiguration conf ) throws IOException
	{
		for ( Entry<String, Object> l : conf.getValues( true ).entrySet() )
		{
			if ( !( l.getValue() instanceof MemorySection ) )
			{
				Log.i( TAG, l.getKey() + " --> " + l.getValue() );
				config.set( l.getKey(), l.getValue() );
			}
		}
		
		config.save( configFile );
	}

	public void saveConfig() throws IOException
	{
		config.save( configFile );
	}
}
