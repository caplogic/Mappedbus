package io.mappedbus.token;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MappedBusMessage;

public class Node extends Thread {
	
	private final int id;
	
	private final int numberOfNodes;
	
	private final int numberOfMessages;
	
	private int messagesReceived;

	private MappedBusReader reader;
	
	private MappedBusWriter writer;
	
	public Node(int id, int numberOfNodes, int numberOfMessages, String fileName, long fileSize, int recordSize) throws Exception {
		this.id = id;
		this.numberOfNodes = numberOfNodes;
		this.numberOfMessages = numberOfMessages;
		writer = new MappedBusWriter(fileName, fileSize, recordSize, true);
		writer.open();
		reader = new MappedBusReader(fileName, fileSize, recordSize);
		reader.open();
	}

	public void run() {
		try {
			
			Token token = new Token();

			if (id == 0) {
				token.setFrom(id);
				token.setTo(1);
				writer.write(token);
			}

			while (true) {
				if (reader.next()) {				
					//System.out.println(id + " Read: " + reader.readMessage(token));
					reader.readMessage(token);
					messagesReceived++;
					if (messagesReceived >= numberOfMessages) {
						break;
					}
					
					if(token.getTo() == id) {
						//Thread.sleep(1000);
						token.setFrom(id);
						token.setTo((id + 1) % numberOfNodes);
						writer.write(token);
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws Exception {
		writer.close();
		reader.close();
	}
	
	public int getMessagesReceived() {
		return messagesReceived;
	}
}