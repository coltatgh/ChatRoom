package assignment7;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;


public class ServerMain extends Application {
	private TextArea ta = new TextArea();
	
	private int clientNo = 0;
	public int getCurrentClientNo() { return clientNo; }
	private int groupNo = 0;
	
	private HashMap<Integer, String> clientMap = new HashMap<Integer, String>();	//map ID's to names
	private HashMap<Integer, ObjectOutputStream> clientOutputStreams = new HashMap<Integer, ObjectOutputStream>();	//should these maybe be maps or something in case...
	private HashMap<Integer, ChatGroup> groupList = new HashMap<Integer, ChatGroup>();								//the group# =/= index (so the order of addition gets fucky)
	private HashMap<Integer, ClientObserver> observerList = new HashMap<Integer, ClientObserver>();

	//Thread-Safe methods for writing to these:
	//client Names and ID's	////////////////////////////////////////////////////
	Object clientKey = new Object();
	
	/**Puts a client's preferred name in the Map, keyed to their ID
	 * @param ID the ID number assigned to the client
	 * @param name the name the client has chosen for his/herself
	 */
	public void putClientMap(int ID, String name){
		synchronized(clientKey){
			clientMap.put(ID, name);
			}
		}
	
	/**Retrieves the name associated with the given ID
	 * @param ID the ID number assigned to the client
	 * @return the name the client has chosen for his/herself
	 */
	public String getClientMap(int ID){
		synchronized(clientKey){
			return clientMap.get(ID);
			}
		}
	
	//clientOutputStreams	/////////////////////////////////////////////////////
	Object streamKey = new Object();
	
	
	/**Saves a clients output Stream in the ArrayList above; the client's ID should be the index
	 * @param s output stream associated with the client whose ID = the index to this stream
	 */
	public void putStreamList(int ID, ObjectOutputStream stream){
		synchronized(streamKey){
			clientOutputStreams.put(ID, stream);
			}
		}
	
	/**Retrieves the output Stream associated with the given client ID (which should index to that client's stream)
	 * @param index the ID of the client whose stream is being retrieved. Used as the index to the array
	 * @return the ObjectOutputStream associated with the client ID given
	 */
	public ObjectOutputStream getStreamList(int ID){
		synchronized(streamKey){
			return clientOutputStreams.get(ID);
			}
		}
	
	//groupList	///////////////////////////////////////////////////////////////////
	Object groupKey = new Object();
	
	/**Save a GroupChat in the ArrayList above (again, the groupNo should match the index)
	 * @param g ChatGroup. Ideally the ID given to each Group will stay such that they are also its index
	 */
	public void putGroupList(int groupNumber, ChatGroup group){
		synchronized(groupKey){
			groupList.put(groupNumber, group);
			}
		}
	
	/**Retrieve a group using the groupNo as an index
	 * @param index
	 * @return the ChatGroup with the given index
	 */
	public ChatGroup getGroupList(int groupNumber){
		synchronized(groupKey){
			return groupList.get(groupNumber);
		}
	}
	
	//observerList	//////////////////////////////////////////////////////////////
	Object observerKey = new Object();
	
	/**Stores the given ClientObserver in the ArrayList above 
	 * @param o the ClientObserver who will be (hopefully) placed such that index = clientID
	 */
	public synchronized void putObserverList(int ID, ClientObserver observer){
		synchronized(observerKey){
			observerList.put(ID, observer);
			}
		}
	
	/**Retrieve the ClientObserver associated with the given clientID
	 * @param index the clientID for the client whose observer we need to retrieve
	 * @return ClientObserver for the clientID given
	 */
	public ClientObserver getObserverList(int ID){
		synchronized(observerKey){
			return observerList.get(ID);
		}
	}	
	// end Thread-safe shit
	
	public static void main(String[] args){
		launch(args);
	}
	@Override 
	public void start(Stage primaryStage){
		Scene scene = new Scene(new ScrollPane(ta), 450, 200);
		primaryStage.setTitle("Server");
		primaryStage.setScene(scene);
		primaryStage.show();
		//the chat everyone is in and everyone gets notified for
		ChatGroup generalChat = new ChatGroup(groupNo, null);	
		groupList.put(groupNo, generalChat);
		groupNo++;
		
		new Thread(() -> {
			try{
				// Create server socket
				ServerSocket serverSocket = new ServerSocket(7000);
				//Update UI
				Platform.runLater(() -> {
					ta.appendText("Server started at " + new Date() + '\n');
				});
				//Main Server Work
				while(true){
					// Listen for connection req
					Socket socket = serverSocket.accept();
					
					clientNo++;
					
					new Thread(new HandleAClient(socket, clientNo)).start();
					
				}
			} catch (IOException ex){
				System.err.println(ex);
			}
		}).start();
	}
	
	/**Processes all incoming messages from a given client
	 *
	 */
	class HandleAClient implements Runnable {
		int personalNo;
		//private BufferedReader reader;
		
		private ObjectOutputStream objOutStream;
		private ObjectInputStream objInStream;
		private boolean isClosed = false;
		
		/** Construct a thread */
		public HandleAClient(Socket socket, int personalNo){
			this.personalNo = personalNo;
			try {
				objOutStream = new ObjectOutputStream(socket.getOutputStream());
				objInStream = new ObjectInputStream(socket.getInputStream());
				
				//save this output stream in the server
				putStreamList(personalNo, objOutStream);	
				
				//Get client name
				objOutStream.writeObject(new DataPacket("message", 0, "You are client Number: " + Integer.toString(clientNo) + "\nSend me your name now"));
				objOutStream.flush();
				String name = ((DataPacket)objInStream.readObject()).getMessage();
				
				//Save the client name under the associated ID number
				putClientMap(personalNo, name);
				
				//Make an observer responsible of relaying all relevant messages to this client
				ClientObserver observer = new ClientObserver(personalNo, name, objOutStream);
				putObserverList(personalNo, observer);
				getGroupList(0).addNewClients(observer);	//all new clients are in the main chat
				
				System.out.println("Client no: " + personalNo + " with name " + name);
				Platform.runLater(() -> {
					ta.appendText("Starting client: " + clientNo + " at " + new Date() + '\n');
					
					InetAddress inetAddress = socket.getInetAddress();
					ta.appendText("Client " + clientNo + "'s host name is " + inetAddress.getHostName() + '\n');
					ta.appendText("Client " + clientNo + "'s IP address is " + inetAddress.getHostAddress() + '\n');
				});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		/** Run a thread */
		public void run() {
				
			// Continuously serve client
			
				while(!isClosed) {
					System.out.println("Entered run of client");
					
					try{
						DataPacket packet = (DataPacket)objInStream.readObject();

						String context = packet.getMeta().toLowerCase();
						int toGroup = packet.getGroup();
						
						switch(context){
						case "message":
							String message = packet.getMessage();
							String output = clientMap.get(personalNo) + ": " + message;
							packet.setMessage(output);
							ChatGroup recipient = getGroupList(toGroup);	//should probably use a map instead of this fragile indexing
							recipient.givePacket(packet);
							System.out.println("Received message " + "<" + output + ">");
							break;
						case "group":
							//Pull apart our stupid structure to get ID's and Names
							ArrayList<ClientObserver> groupMembers = getAllObservers(packet);
//							String[] groupMembers = packet.getUsernames();
//							String[] clientID = new String[groupMembers.length/2];
//							String[] clientNames = new String[groupMembers.length/2];
//							int counter = 0;
//							for(int i = 0; i<groupMembers.length; i+=2){
//								clientID[counter] = groupMembers[i];
//								clientNames[counter] = groupMembers[i+1];
//								counter++;
//							}
//							
//							//Get the ClientObservers associated with the clients (IDs) in the pkt
//							ArrayList<ClientObserver> newGroupies = new ArrayList<ClientObserver>();
//							int currentID;
//							ClientObserver currentObserver;
//							try{
//								for(String s: clientID){
//									currentID = Integer.parseInt(s);
//									currentObserver = getObserverList(currentID);	//because as it is right now, ID's are simply indices
//									newGroupies.add(currentObserver);
//								}
//								//newGroupies.add(getObserverList(personalNo));	//also remember to add the client whose handler this is! (Ian should've put him in the list)
//							}catch(NumberFormatException nfe){
//								nfe.printStackTrace();
//							}
							ChatGroup newGroup = new ChatGroup(groupNo, groupMembers);
							newGroup.finalizeGroupCreation();
							putGroupList(groupNo, newGroup);
							System.out.println("Created new group " + "<" + groupNo + ">");
							groupNo++;
							break;
						case "userlist":
							ChatGroup recipient2 = getGroupList(toGroup);
							ArrayList<ClientObserver> newPeeps = getAllObservers(packet);
							recipient2.addNewClients(newPeeps);
							System.out.println("Invited new users to " + "<Group: " + groupNo + ">");
							break;
						case "quit":
							String[] clientsGroups = packet.getUsernames();	//this will hold the group numbers
							int byeGroup;
							DataPacket goodbye;
							ChatGroup recipient3;
							for(String s : clientsGroups){
								byeGroup = Integer.parseInt(s);
								goodbye = new DataPacket("quit", byeGroup, ""+personalNo);
								recipient3 = getGroupList(byeGroup);
								recipient3.givePacket(goodbye);
							}
							isClosed = true;
							break;
						}
						
					} catch (IOException e) {
						System.err.println(e);
						
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		
				}
			}
	private ArrayList<ClientObserver> getAllObservers(DataPacket packet){
		//Pull apart our stupid structure to get ID's and Names
		String[] groupMembers = packet.getUsernames();
		String[] clientID = new String[groupMembers.length/2];
		String[] clientNames = new String[groupMembers.length/2];
		int counter = 0;
		for(int i = 0; i<groupMembers.length; i+=2){
			clientID[counter] = groupMembers[i];
			clientNames[counter] = groupMembers[i+1];
			counter++;
		}
		
		//Get the ClientObservers associated with the clients (IDs) in the pkt
		ArrayList<ClientObserver> newGroupies = new ArrayList<ClientObserver>();
		int currentID;
		ClientObserver currentObserver;
		try{
			for(String s: clientID){
				currentID = Integer.parseInt(s);
				currentObserver = getObserverList(currentID);	//because as it is right now, ID's are simply indices
				newGroupies.add(currentObserver);
			}
			//newGroupies.add(getObserverList(personalNo));	//also remember to add the client whose handler this is! (Ian should've put him in the list)
		}catch(NumberFormatException nfe){
			nfe.printStackTrace();
		}
		
		return newGroupies;
	}
		}
	}