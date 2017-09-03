package assignment7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;

public class ChatGroup extends Observable{
	private int GroupID;
	private String cumMess = "";		//holds the cumulative message of the whole chat
	private ArrayList<Integer> clientIDs = new ArrayList<Integer>();		//for debugging purposes, do we maybe need a way to see who is this group here?
	private ArrayList<String> clientNames = new ArrayList<String>();
	private ArrayList<ClientObserver> clientObservers = new ArrayList<ClientObserver>();
	
	public ChatGroup(int GroupID, ArrayList<ClientObserver> watchers){	//If I ever refactor this, just pass the constructor the GroupID and the packet and let it do the rest
		this.GroupID = GroupID;
		if(watchers != null){
			String intro = "A new group has been created!\n";
			for(ClientObserver c : watchers){
				clientIDs.add(c.clientID);
				clientNames.add(c.clientName);
				clientObservers.add(c);
				intro += "New user " + c.clientName + " joined\n";
				addObserver(c);
			}
			
			cumMess += intro;
//			String[] allUserInfo = getAllUserInfo();
//			DataPacket newGroupPkt = new DataPacket("group", this.GroupID, cumMess, allUserInfo);
//			setChanged();
//			notifyObservers(newGroupPkt);	//Output packet for this
			System.out.println("Group " + GroupID + " initialized");
		}else{
			cumMess += "This is all-chat: all clients may interact here\n";
		}
	}
	
	/**Adds a single new client to this group
	 * @param watcher the ClientObserver responsible for relaying info to a client
	 */
	public void addNewClients(ClientObserver watcher){	//Add new clients
		synchronized(this){
			//get the noob in the system
			addObserver(watcher);
			clientIDs.add(watcher.clientID);
			clientNames.add(watcher.clientName);
			clientObservers.add(watcher);
			
			//We can try updating the noob first...
			watcher.catchUpNoob(cumMess);
			
			//And then send the new stuff to everyone (including the now-caught-up noobs)
			String intro = "New user " + watcher.clientName + " joined\n";
			cumMess += intro;
			
			// Now the packet to alert everyone to the new clients
			//We could still have the "message" line give the intro						//Think through the "new clients, old group" situatation carefully
			String[] allUserInfo = getAllUserInfo();
			DataPacket userUpdatePkt = new DataPacket("userlist", GroupID, intro, allUserInfo);
			setChanged();
			notifyObservers(userUpdatePkt);	//TODO make sure IAN handles (outputs) the welcomes on his side
			System.out.println("ChatGroup: User " + watcher.clientName + " added");
		}
	}
	
	/**Adds several new clients to the group via ArrayList
	 * @param watchers the ClientObservers for new clients to be added to the group
	 */
	public void addNewClients(ArrayList<ClientObserver> watchers){
		synchronized(this){
			String intro = "";
			for(ClientObserver c : watchers){
				addObserver(c);
				clientIDs.add(c.clientID);
				clientNames.add(c.clientName);
				clientObservers.add(c);
				
				//We could send the new members the previous convo messages
				c.catchUpNoob(cumMess);					//catch each one up on the conversation from before any of them were added
				intro += "New user " + c.clientName + " joined\n";		//but build the combined welcome message we'll have to append
				System.out.println("User " + c.clientName + " added");
				
			}
			//before we send everyone the new stuff
			cumMess += intro;	//document the additions in the cumulative message
			String[] allUserInfo = getAllUserInfo();
			DataPacket userUpdatePkt = new DataPacket("userlist", GroupID, intro, allUserInfo);
			setChanged();
			notifyObservers(userUpdatePkt);		//send everyone the new stuff
		}
	}
	
	
	/**Used because I let Ian convince me that sending our client IDs and names alternating in a String[] made sense 
	 * @return the String[] with evens are ID's and odds are names
	 */
	private String[] getAllUserInfo(){
		String[] allUserInfo = new String[2*clientIDs.size()];
		int IDcount =  0; int nameCount = 0;
		for(int i=0; i<allUserInfo.length; i++){
			if(i%2 == 0){
				allUserInfo[i] = clientIDs.get(IDcount).toString();
				IDcount++;
			}else{
				allUserInfo[i] = clientNames.get(nameCount);		//I hate us so much for this
				nameCount++;
			}
		}
		return allUserInfo;
	}
	
	private void removeClient(int ID){
		synchronized(this){
			//get the index for this client
			int index = 0;
			for(int i=0; i<clientIDs.size(); i++){
				if(clientIDs.get(i) == ID){
					index = i;
				}
			}
				clientIDs.remove(index);
				String name = clientNames.remove(index);
				this.deleteObserver(clientObservers.get(index));

				//Inform those who remain
				String goodbye = "User " + name + " has left";
				String[] allUserInfo = getAllUserInfo();
				DataPacket userUpdatePkt = new DataPacket("userlist", GroupID, goodbye, allUserInfo);
				setChanged();
				notifyObservers(userUpdatePkt);		//send everyone the new stuff
				System.out.println("User " + name + " removed");
		}
	}
	
	/**The means by which the ChatGroup accepts information
	 * @param pkt message or other data for use by the Group or distribution to the users
	 */
	public void givePacket(DataPacket pkt){
		//Work with metaData here to ensure we process this correctly
		//What would the group receive besides just more messages?
		//notifyObservers: whatever changes occurred, we need to notify them
		String context = pkt.getMeta().toLowerCase();
		synchronized(this){
			switch(context){
			case "message":
				cumMess += pkt.getMessage();
				System.out.println("Packet received");
				setChanged();
				this.notifyObservers(pkt);
				break;
			case "quit":
				int ID = Integer.parseInt(pkt.getMessage());
				removeClient(ID);
				break;
			}
		}
	}
	
	public void finalizeGroupCreation(){
			synchronized(this){
			String[] allUserInfo = getAllUserInfo();
			DataPacket newGroupPkt = new DataPacket("group", this.GroupID, cumMess, allUserInfo);
			setChanged();
			notifyObservers(newGroupPkt);	//Output packet for this
		}
	}
}
