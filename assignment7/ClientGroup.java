package assignment7;

import java.util.HashMap;

public class ClientGroup {
	private int groupNumber;
	private HashMap<Integer, String> users = new HashMap<Integer, String>();
	
	public ClientGroup(int num, int[] userIDs, String[] userNames){
		groupNumber = num;
		
		for(int i = 0; i < userIDs.length; i++){
			users.put(userIDs[i], userNames[i]);
		}
		
	}
	
	// If you just want an empty group
	public ClientGroup(int num) {
		groupNumber = num;
	}
	
	public int[] getUserIDs(){
		int[] userIDs = new int[users.size()];
		
		int i = 0;
		for(int userID : users.keySet()) {
			userIDs[i] = userID;
			i++;
		}
		
		return userIDs;
	}
	
	public String[] getUsernames() {
		String[] usernames = new String[users.size()];
		
		int i = 0;
		for(String name : users.values()) {
			usernames[i] = name;
			i++;
		}
		
		return usernames;
	}
	
	public HashMap<String, Integer> getReverseUserMap(){
		HashMap<String, Integer> reverse = new HashMap<String, Integer>();
		
		for(Integer userID : users.keySet()) {
			reverse.put(users.get(userID), userID);
		}
		
		return reverse;
		
	}
	
	public void updateUserList(int[] userIDs, String[] userNames) {
		users.clear();
		for(int i = 0; i < userIDs.length; i++){
			users.put(userIDs[i], userNames[i]);
		}
		
		System.out.println("Userlist in Group <" + groupNumber + "> as Follows");
		for(Integer ID : users.keySet()) {
			System.out.println(ID + " : " + users.get(ID));
		}
	}
	
	
}
