package assignment7;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Observable;
import java.util.Observer;

import javafx.application.Platform;

public class ClientObserver implements Observer{
	
	//Output Stream or something here, associated with a single client
	Integer clientID;
	String clientName;
	ObjectOutputStream toClient;
	
	public ClientObserver(Integer personalNo, String name, ObjectOutputStream stream){
		clientID = personalNo;
		clientName = name;
		toClient = stream;
		System.out.println("ClientObserver initialized for " + name);
	}
	
	
	/**Used to synchronize access to this ClientObserver's Output stream
	 * @param pkt the object to be sent
	 */
	public synchronized void useSharedStream(Object pkt){	//This is needed only if we try to catch new users up on previous conversation

			Platform.runLater(() -> {
				System.out.println("Inside update");
	//			Platform.runLater(() -> {
					try {
						toClient.writeObject(pkt);
						toClient.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	//			});
				System.out.println("Presumably sent message");
			});
		
	}
	
	
	/**Used to update new users on the previous conversation in a group without having to annoy all other members of the group
	 * @param prevConvo String holding the entire conversation preceding this client's addition to the group
	 */
	public synchronized void catchUpNoob(String prevConvo){	//TODO add a conversation number or something
		useSharedStream(new DataPacket("message", 0, prevConvo));	//As is this, obviously
	}
	
	@Override
	public void update(Observable group, Object pkt) {
		//This will always do the same thing: pass message along to the client
		useSharedStream(pkt);
	}

}
