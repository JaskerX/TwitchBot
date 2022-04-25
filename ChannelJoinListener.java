package de.jaskerx;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.twitch4j.chat.events.channel.ChannelJoinEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

public class ChannelJoinListener {

	public static void checkUser (ChannelJoinEvent event) {
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
		
		if (!isUserInList(userId)) {
			addUserToList(user, userId);
			System.out.println(user + "added");
		}
		
	}
	
	private static boolean isUserInList (String userId) {
		try {
			Reader fileReader = new FileReader("users.csv");
			CSVReaderBuilder builder = new CSVReaderBuilder(fileReader);
			CSVReader reader = builder.build();
			
			List<String[]> all = reader.readAll();
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i)[0].equals(userId)) {
					return true;
				}
			}
			reader.close();
			
		} catch (IOException | CsvException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static void addUserToList (EventUser user, String userId) {
		try {
			String datum = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now());
	    	String uhrzeit = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now());
			Writer fileWriter = new FileWriter("users.csv", true);
			CSVWriter writer = new CSVWriter(fileWriter);
			
			String[] lineText = {userId, user.getName(), "false", datum + " - " + uhrzeit};
			writer.writeNext(lineText);
			
			writer.flush();
			writer.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
