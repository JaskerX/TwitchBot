package de.jaskerx;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvException;

public class MessageListener {

	public static boolean checkMessage(ChannelMessageEvent event)
	{
	String prefix = BotMain.prefix;	
	String channel = event.getChannel().getName();
	String command = "";
	if (event.getMessage().startsWith(prefix)) {
		command = event.getMessage().toLowerCase().split(prefix)[1].split(" ")[0];
	}
	EventUser user = event.getUser();
	String userId = "";
	try {
		List<String> users = new ArrayList<String>();
		users.add(user.getName());
		HystrixCommand<UserList> resUsers = BotMain.client.getHelix().getUsers("", null, users);
		String usersList = resUsers.queue().get().toString();
		userId = usersList.split("id=")[1].split(",")[0];
	} catch (InterruptedException | ExecutionException e) {
		e.printStackTrace();
	}
	
		if (BotMain.verifiedOnlyChat && !isUserVerified(userId)) {
			Duration timeoutDuration = Duration.ofMinutes(5);
			System.out.println("timeout: " + user.getName() + event.getTwitchChat().timeout(channel, user.getName(), timeoutDuration, "Aktuell dürfen nur verifizierte Nutzer schreiben. Es wird aber vermutlich nur wenige Minuten dauern, bis dieser Modus deaktiviert wird (alle, die das erste Mal bei einem meiner Streams dabei sind, gelten als nicht verifiziert; nach dem Stream sollte normalerweise jeder automatisch verifiziert werden, im nächsten Stream wirst du also nicht mehr getimeouted werden)"));
			return false;
		}
	
		System.out.println("* " + user.getName() + " issued " + command + " (" + event.getMessage() + ")");
	
		switch (command) {
			
			case "youtube": //reply(channel, "Meinen YouTube-Kanal findest du unter https://www.youtube.com/channel/UCUZCUva4ecvFDEB9FPpHsow. Er ist aktuell aber inaktiv."); break;
			case "yt": reply(channel, "Meinen YouTube-Kanal findest du unter https://www.youtube.com/channel/UCUZCUva4ecvFDEB9FPpHsow. Er ist aktuell aber inaktiv.");
				break;
			case "discord": //reply(channel, "Meinen Discord-Server findest du unter https://discord.gg/HzQGp6w"); break;
			case "dc": reply(channel, "Meinen Discord-Server findest du unter https://discord.gg/HzQGp6w");
				break;
			case "website": reply(channel, "Meine Website findest du unter https://jaskerx.github.io. Sie ist allerdings noch in Arbeit");
				break;
			case "help": //reply(channel, "Ihr könnt momentan folgende Commands nutzen: '!discord/!dc', '!youtube/!yt', '!website'"); break;
			case "commands": //reply(channel, "Ihr könnt momentan folgende Commands nutzen: '!discord/!dc', '!youtube/!yt', '!website'"); break;
			case "cmnds": //reply(channel, "Ihr könnt momentan folgende Commands nutzen: '!discord/!dc', '!youtube/!yt', '!website'"); break;
			case "hilfe": //reply(channel, "Ihr könnt momentan folgende Commands nutzen: '!discord/!dc', '!youtube/!yt', '!website'"); break;
			case "hilf": reply(channel, "Ihr könnt momentan folgende Commands nutzen: '!discord/!dc', '!youtube/!yt', '!website'");
				break;
			case "verifyall": if (userId.equals("478400355")) {
									verifyAll();
								}
							reply(channel, "Alle nicht verifizierten Nutzer wurden verifiziert.");
				break;
			case "verifiedonlychat": if (userId.equals("478400355")) {
										if (BotMain.verifiedOnlyChat) {
											reply(channel, "Es können nun wieder alle Nutzer schreiben.");
											BotMain.verifiedOnlyChat = false;
										} else {
											reply(channel, "Es können nur noch verifizierte Nutzer schreiben (also alle, die das erste Mal bei einem meiner Streams dabei sind; nach dem Stream sollte normalerweise jeder automatisch verifiziert werden).");
											BotMain.verifiedOnlyChat = true;
										}
									}
				break;

		}
		
		return true;
	}
	
	
	private static void reply (String channel, String message) {
		BotMain.client.getChat().sendMessage(channel, message);
	}
	
	private static boolean isUserVerified (String userId) {
		try {
			Reader fileReader = new FileReader("users.csv");
			CSVReaderBuilder builder = new CSVReaderBuilder(fileReader);
			CSVReader reader = builder.build();
			
			List<String[]> all = reader.readAll();
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i)[0].equals(userId) && all.get(i)[2].equals("true")) {
					return true;
				}
			}
			
		} catch (IOException | CsvException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private static void verifyAll () {
		try {
			Reader fileReader = new FileReader("users.csv");
			CSVReaderBuilder builder = new CSVReaderBuilder(fileReader);
			CSVReader reader = builder.build();
			
			List<String[]> all = reader.readAll();
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i)[2].equals("false")) {
					all.get(i)[2] = "true";
				}
			}
			reader.close();
			Writer fileWriter = new FileWriter("users.csv", false);
			CSVWriter writer = new CSVWriter(fileWriter);
			writer.writeAll(all);
			writer.flush();
			writer.close();
			
		} catch (IOException | CsvException e) {
			e.printStackTrace();
		}
	}
}
