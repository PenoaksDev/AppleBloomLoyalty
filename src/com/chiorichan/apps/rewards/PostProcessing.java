package com.chiorichan.apps.rewards;

public class PostProcessing
{
	public ActionList action;
	public String payload;
	
	public PostProcessing( ActionList _action, String _payload )
	{
		action = _action;
		payload = _payload;
	}
	
	public enum ActionList
	{
		TOAST(),
		UPDATEUI();
	}
}
