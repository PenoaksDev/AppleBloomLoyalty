package com.chiorichan.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class MyLittleDB extends SQLiteOpenHelper
{
	public MyLittleDB(Context context)
	{
		super(context, "RewardsData", null, 2);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		try
		{
			db.execSQL("CREATE TABLE pending (id, time, msg, expire);");
			db.execSQL("CREATE TABLE redeemables (id, title, cost);");
			db.execSQL("CREATE TABLE trans (id, time, n, p, action, comment);");
			db.execSQL("CREATE TABLE users (id, name, email, first_added, balance, last_instore_check);");
		}
		catch ( SQLiteException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		
	}
}
