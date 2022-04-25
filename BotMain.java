package de.jaskerx;



import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelJoinEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BotMain {

	static String channel = "jaskerx";
	static TwitchClient client;
	static String prefix = "!";
	static boolean wasLive = false;
	static boolean verifiedOnlyChat = false;
	
	public static void main(String[] args)
	{
	OAuth2Credential oAuth2Credential = new OAuth2Credential("twitch", "");
	
		client = TwitchClientBuilder.builder()
							.withEnableHelix(true)
							.withEnableChat(true)
							.withChatAccount(oAuth2Credential)
							.build();

		/*client.getClientHelper().enableStreamEventListener(channel);

		
		client.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> {
			if (!BotMain.wasLive) {
	    		BotMain.client.getChat().connect();
	    		BotMain.client.getChat().joinChannel(BotMain.channel);
	    		System.out.println("connected");
	    		BotMain.wasLive = true;
    		}
		});
		client.getEventManager().onEvent(ChannelGoOfflineEvent.class, event -> {
			if (BotMain.wasLive) {
	    		BotMain.client.getChat().disconnect();
	    		System.out.println("disconnected");
	    		BotMain.wasLive = false;
			}
		});*/
		client.getEventManager().onEvent(ChannelMessageEvent.class, event -> MessageListener.checkMessage(event));
		client.getEventManager().onEvent(ChannelJoinEvent.class, event -> ChannelJoinListener.checkUser(event));
		
	/*		client.getChat().connect();
			client.getChat().joinChannel(channel);
			System.out.println("connected");*/
		
		
		Timer liveTimer = new Timer();
		
		liveTimer.schedule(new CheckLiveTask(), 0, 30000);
	}
	
}

class MakeLiveRequest {
	
	OkHttpClient client = new OkHttpClient();

    public String run(String url) throws IOException {
        Request request = new Request.Builder()
        		.url(url)
        		.addHeader("client-id", "")
        		.addHeader("Authorization", "Bearer ")
        		.build();

        try (Response response = client.newCall(request).execute()) {
        	return response.body().string();
        }
    }
	
}

class CheckLiveTask extends TimerTask {

	OkHttpClient client = new OkHttpClient();
	public String run(String url) throws IOException {
        Request request = new Request.Builder()
        		.url(url)
        		.addHeader("client-id", "")
        		.addHeader("Authorization", "Bearer ")
        		.build();

        try (Response response = client.newCall(request).execute()) {
        	return response.body().string();
        }
    }
	
	@Override
	public void run() {

		try {
			
			JSONObject response = new JSONObject(new MakeLiveRequest().run("https://api.twitch.tv/helix/search/channels?query=" + BotMain.channel));
			
			String result = "Ein Fehler ist aufgetreten!";
			for (int i = 0; i < response.getJSONArray("data").length(); i++) {
				String broadcaster_login = 	response.getJSONArray("data").getJSONObject(i).get("broadcaster_login").toString();
				boolean is_live = Boolean.valueOf(response.getJSONArray("data").getJSONObject(i).get("is_live").toString());
				
				if (broadcaster_login.equals(BotMain.channel)) {

			    	if (is_live) {
			    		result = "[" + LocalTime.now().getHour() + ":" + LocalTime.now().getMinute() + ":" + LocalTime.now().getSecond() + "] Der gesuchte Streamer ist online!";
			    		if (!BotMain.wasLive) {
				    		BotMain.client.getChat().connect();
				    		BotMain.client.getChat().joinChannel(BotMain.channel);
				    		System.out.println("connected");
				    		BotMain.wasLive = true;
			    		}
			    	} else {
			    		result = "[" + LocalTime.now().getHour() + ":" + LocalTime.now().getMinute() + ":" + LocalTime.now().getSecond() + "] Der gesuchte Streamer ist offline!";
			    		if (BotMain.wasLive) {
				    		BotMain.client.getChat().disconnect();
				    		System.out.println("disconnected");
				    		BotMain.wasLive = false;
			    		}
			    	}
				}
			}
			System.out.println(result);
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
