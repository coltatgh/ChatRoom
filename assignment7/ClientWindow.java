package assignment7;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/*
 * Ask About
 * 1) Working on two computers
 * 2) How to handle a client exiting [server-side question]
 * 		* ActionOnClose
 */

public class ClientWindow extends Application{
	private String desiredIP = "localhost";
	
	private int myID;
	private String myUsername;
	private boolean amIInstantiated = false;
	
	// IO Streams
	DataOutputStream toServer = null;
	DataInputStream fromServer = null;
	
	private ObjectOutputStream objOutStream;
	private ObjectInputStream objInStream;
	
	private HashMap<Integer, ClientGroup> groupMap = new HashMap<Integer, ClientGroup>();
	private ArrayList<Integer> groupsMemberOf = new ArrayList<Integer>();
	
	//private TextArea ta = new TextArea();
	private TabPane tabPane;
	private Tab newGroupTab;
	private VBox newGroupUsernameRegion;
	private ArrayList<CheckBox> usernameChecksNewGroup;
	private HashMap<Integer, Tab> tabMap = new HashMap<Integer, Tab>();
	private HashMap<Integer, TextArea> textAreaMap = new HashMap<Integer, TextArea>();
	private HashMap<Integer, TextField> textFieldMap = new HashMap<Integer, TextField>();
	private HashMap<Integer, VBox> usernameRegionMap = new HashMap<Integer, VBox>();
	
	/*
	 * (non-Javadoc)
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 * 
	 * Metadata
	 * group - create new group
	 * 		message - <userID>::<userID>:: .....
	 * 
	 * Colton: Anytime a new client is created
	 * 	1st tab == all chat
	 * 	* message = <userID>::<userID> 
	 */
	
	@Override
	public void start(Stage primaryStage){
		
		/*
		 * Initial Window : Username Sign-In
		 */
		BorderPane signInBP = new BorderPane();
		signInBP.setPadding(new Insets(5,5,5,5));
		signInBP.setStyle("-fx-border-color: green");
		signInBP.setTop(new Label("Enter Desired Username"));
		
		TextField userNameTF = new TextField();
		userNameTF.setAlignment(Pos.BOTTOM_RIGHT);
		userNameTF.selectPositionCaret(0);
		signInBP.setCenter(userNameTF);
		
		// Overall TabPane -----------
		tabPane = new TabPane();
		
		Tab allTab = makeGroupTab(0);
		
		tabMap.put(0, allTab);
		tabPane.getTabs().add(allTab);
		
		
		// ---------- NEW GROUP TAB FORMATION ----------
		newGroupTab = new Tab();
		VBox newGroupTabVBox = new VBox(15);
		
		//newGroupTabVBox.getChildren().add(new TextArea("This will be the make a group window\n To be completed"));
		
		
		Button btNewGroupMake = new Button();
		btNewGroupMake.setOnAction(e -> {
			
			
		});
		
		//newGroupTabVBox.getChildren().addAll(usernameRegionMap.get(0), btNewGroupMake);
		newGroupTab.setText("New Group");
		newGroupTab.setContent(newGroupTabVBox);
		tabPane.getTabs().add(newGroupTab);
		
		// ---------- Create Group 0 : the ALL group
		groupMap.put(0, new ClientGroup(0)); // Empty except myself for starters
		
		
		// ---------- Create scene and place on stage
		Scene scene = new Scene(signInBP, 300, 150);
		primaryStage.setTitle("Client");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		
		userNameTF.setOnAction(e -> {
			DataPacket packet = new DataPacket("message", 0, userNameTF.getText());
			
			System.out.println("Username Init Packet msg: " + packet.getMessage());
			
			myUsername = userNameTF.getText();
			
			try {
				objOutStream.writeObject(packet);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			primaryStage.setTitle(userNameTF.getText());
			primaryStage.setScene(new Scene(tabPane, 600, 300));
		});
		
		textFieldMap.get(0).setOnAction(e -> {
			DataPacket packet = new DataPacket("message", 0, textFieldMap.get(0).getText());
			try {
				objOutStream.writeObject(packet);
				objOutStream.flush();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			textFieldMap.get(0).setText("");
			textFieldMap.get(0).requestFocus();
			
			
		});
		
		try {
			// Create socket connection
			Socket socket = new Socket(desiredIP, 7000);
			
			//InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
			
			objOutStream = new ObjectOutputStream(socket.getOutputStream());
			objInStream = new ObjectInputStream(socket.getInputStream());
			
			Thread th = new Thread(new IncomingReader());
			th.start();
			
			
		} catch (IOException ex) {
			textAreaMap.get(0).appendText(ex.toString() + '\n');
		}
		
	}
	
	
	
	class IncomingReader implements Runnable {
		@Override
		public void run(){
			while(true) {
				try{
					
					DataPacket pack = (DataPacket)objInStream.readObject();
					
					System.out.println("Processing <" + pack.getMeta() + "> packet");
					
					switch(pack.getMeta()) {
						case "userlist": 
							updateUserList(pack);
							break;
						case "group": 
							makeNewGroup(pack);
							break;	
					}
					
					Platform.runLater(() -> {
						textAreaMap.get(pack.getGroup()).appendText(pack.getMessage() + '\n');
					});
					
					
				} catch (IOException ex) {
					System.err.println(ex);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void updateUserList(DataPacket pack) {
		System.out.println("Entered update userlist");
		
		// Meta = userList
		// String[] = userID : userName ::
		// groupID
		
		int[] userIDs = new int[pack.getUsernames().length / 2];
		String[] userNames = new String[pack.getUsernames().length / 2];
		
		// BACKEND - Update userlist data
		try{
			// Parsing for relevant info ("split up")
			for(int i = 0; i < pack.getUsernames().length - 1; i = i + 2) {
				userIDs[i/2] = Integer.parseInt((pack.getUsernames()[i]));
				userNames[i/2] = pack.getUsernames()[i + 1];
			}
			
			if(!amIInstantiated){
				for(int i = 0; i < userIDs.length; i++){
					if(userNames[i].compareTo(myUsername) == 0){
						myID = userIDs[i];
						amIInstantiated = true;
						break;
					}
				}
			}
			
			if(groupMap.containsKey(pack.getGroup())){
				groupMap.get(pack.getGroup()).updateUserList(userIDs, userNames);
			}
		} catch (Exception e) {
			System.out.println("Username info in incorrect format");
			System.err.println(e);
			e.printStackTrace();
		}
		
		System.out.println("Usernames parsed");
		for(String name : userNames) {
			System.out.println(name);
		}
		
		System.out.println("Group: " + pack.getGroup());
		
		if(groupMap.containsKey(pack.getGroup())){
			System.out.println("Adding Usernames to Group: " + pack.getGroup());
			// GUI - update userlist region
			String[] allUsernames = groupMap.get(pack.getGroup()).getUsernames();
			ArrayList<CheckBox> usernameChecksGroup = new ArrayList<CheckBox>();
			ArrayList<CheckBox> usernameChecksNewGroup = new ArrayList<CheckBox>();
			//CheckBox[] usernameChecks = new CheckBox[groupMap.get(pack.getGroup()).getUserIDs().length - 1];
			for(int i = 0; i < (groupMap.get(pack.getGroup()).getUserIDs().length); i++){
				System.out.println("Boutta check username: " + allUsernames[i]);
				if(allUsernames[i].compareTo(myUsername) != 0) {
					//final CheckBox cb = usernameChecks[i] = new CheckBox(allUsernames[i]);
					usernameChecksGroup.add(new CheckBox(allUsernames[i]));
					usernameChecksNewGroup.add(new CheckBox(allUsernames[i]));
				} 
			}
			
			Platform.runLater(() -> {
				// Update UserList of Group Tab
				VBox usernameRegion = usernameRegionMap.get(pack.getGroup());
				usernameRegion.setSpacing(1);
				usernameRegion.setPadding(new Insets(2));
				usernameRegion.getChildren().clear();
				if(!usernameChecksGroup.isEmpty()) { 
					usernameRegion.getChildren().addAll(usernameChecksGroup);
				}
				
				
			});
			
			// If its group 0, we need to update the "masterlist"
			if(pack.getGroup() == 0) {
				this.usernameChecksNewGroup = usernameChecksNewGroup;
				
				Platform.runLater(() -> {
					// Update New Group Tab
					VBox newGroupVBox = new VBox();
					Button btMakeGroup = new Button("Make Group");
					btMakeGroup.setOnAction(e -> {
						/**
						 * Make Data Packet with
						 * Meta = "group"
						 * String[] = memberIDs
						 */
						
						// Get the checked usernames
						ArrayList<String> desiredNames = new ArrayList<String>();
						for(CheckBox cb : this.usernameChecksNewGroup) {
							if(cb.isSelected()) {
								desiredNames.add(cb.getText());
							}
						}
						
						
						if(!desiredNames.isEmpty()) {
							// Get IDs from checked usernames
							String[] desiredInfo = new String[(desiredNames.size() * 2) + 2];
							HashMap<String, Integer> revHM = groupMap.get(0).getReverseUserMap();
							desiredInfo[0] = Integer.toString(myID);
							desiredInfo[1] = myUsername;
							int i = 2;
							for(String name : desiredNames) {
								desiredInfo[i] = Integer.toString(revHM.get(name));
								desiredInfo[i + 1] = name;
								i += 2;
							}
							
							
							// Send the right data packet for what I want
							DataPacket newGroupPack = new DataPacket("group", -1, desiredInfo);
							try {
								//System.out.println("Sent packet with " + newGroupPack.getMeta());
								objOutStream.writeObject(newGroupPack);
								objOutStream.flush();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							
							
							System.out.println("New group as follows");
							for(String s : desiredInfo){
								System.out.println(s);
							}
						}
					});
					
					
					
					newGroupVBox.getChildren().addAll(this.usernameChecksNewGroup);
					newGroupVBox.getChildren().add(btMakeGroup);
					newGroupTab.setContent(newGroupVBox);
				});
			}
			
		}
		
	}
	
	
	
	public void makeNewGroup(DataPacket pack) {
		System.out.println("Entered make new group");
		
		if(pack.getUsernames().length % 2 != 0){
			System.out.println("Username info not correct length");
			return;
		}
		
		
		int[] userIDs = new int[(pack.getUsernames().length / 2) + 1];	// +1 for my own username and ID
		String[] userNames = new String[(pack.getUsernames().length / 2) + 1];
		userIDs[0] = myID;
		userNames[0] = myUsername;
		
		
		
		try{
			for(int i = 0; i < pack.getUsernames().length; i = i + 2) {
				userIDs[(i/2) + 1] = Integer.parseInt((pack.getUsernames()[i]));
				userNames[(i/2) + 1] = pack.getUsernames()[i + 1];
			}
			
			groupMap.put(pack.getGroup(), new ClientGroup(pack.getGroup(), userIDs, userNames));
			
		} catch (Exception e) {
			System.out.println("Username info in incorrect format");
		}
		
		// Make a new tab for this group
		Tab newTab = makeGroupTab(pack.getGroup());
		
		Platform.runLater(() -> {
			tabPane.getTabs().remove(newGroupTab);	// Removed and added so its at the end
			tabPane.getTabs().add(newTab);
			tabPane.getTabs().add(newGroupTab);
		});
		
		groupsMemberOf.add(pack.getGroup());
	}
	
	public Tab makeGroupTab(int groupNo){
		Tab newTab = new Tab();
		
		// Panel formation
		BorderPane tfPane = new BorderPane();
		tfPane.setPadding(new Insets(5,5,5,5));
		tfPane.setStyle("-fx-border-color: green");
		tfPane.setLeft(new Label("Enter text: "));
		
		TextField tfChatBox = new TextField();
		textFieldMap.put(groupNo, tfChatBox);
		tfChatBox.setAlignment(Pos.BOTTOM_RIGHT);
		tfChatBox.selectPositionCaret(0);
		tfChatBox.setText("");
		tfPane.setCenter(tfChatBox);
		
		BorderPane mainPane = new BorderPane();
		
		// Text area for contents
		TextArea ta = new TextArea();
		textAreaMap.put(groupNo, ta);
		ta.setEditable(false);
		mainPane.setCenter(new ScrollPane(ta));
		mainPane.setBottom(tfPane);
		
		// Area for usernames
		
		
		if(groupMap.containsKey(groupNo)){
			String[] allUsernames = groupMap.get(groupNo).getUsernames();
			CheckBox[] usernameChecks = new CheckBox[groupMap.get(groupNo).getUserIDs().length];
			for(int i = 0; i < usernameChecks.length; i++){
				final CheckBox cb = usernameChecks[i] = new CheckBox(allUsernames[i]);
			}
			
			VBox usernameRegion = new VBox();
			usernameRegion.setSpacing(1);
			usernameRegion.setPadding(new Insets(2));
			usernameRegion.getChildren().addAll(usernameChecks);
			usernameRegionMap.put(groupNo, usernameRegion);
			
			
			
			//Label pH = new Label("Usernames");
			//usernameRegion.getChildren().add(pH);
			mainPane.setRight(usernameRegion);
		} else {
			VBox usernameRegion = new VBox();
			usernameRegion.setSpacing(1);
			usernameRegion.setPadding(new Insets(2));
			usernameRegion.getChildren().add(new Label("EMPTY"));
			usernameRegionMap.put(groupNo, usernameRegion);
			mainPane.setRight(usernameRegion);
		}
		
		
		
		newTab.setContent(mainPane);
		if(groupNo == 0) {
			newTab.setText("Group: All");
		} else
		newTab.setText("Group: " + groupNo);
		
		return newTab;
	}
	
	public void stop(){
		System.out.println("Closing");
		
		String[] memberOf = new String[groupsMemberOf.size() + 1];
		memberOf[0] = "0";
		
		int i = 1;
		for(int groupNo : groupsMemberOf) {
			memberOf[i] = "" + groupNo;
			i++;
		}
		DataPacket closePack = new DataPacket("quit", 0, memberOf);
		try {
			objOutStream.writeObject(closePack);
			DataPacket packIn = (DataPacket)objInStream.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
