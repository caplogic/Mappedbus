package io.mappedbus;

import static org.junit.Assert.assertEquals;
import io.mappedbus.MappedBusMessage;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MemoryMappedFile;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests that no messages are lost by passing a token around between a number of nodes.
 *
 * At the end there's a check that each node received the expected number of messages. 
 *
 * For more exhaustive testing NUM_RUNS can be increased.
 * 
 */
public class TokenTest {

	public static final String FILE_NAME = "/tmp/token-test";

	public static final int RECORD_SIZE = 8;

	public static final long FILE_SIZE = 20000000L;

	public static final int NUM_MESSAGES = 100000;

	public static final int NUM_NODES = 3;
	
	public static final int WAIT_TIME = 10000;
	
	public static final int NUM_RUNS = 100;

	@Before public void before() {
		new File(FILE_NAME).delete();
	}
	
	@After public void after() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void test() throws Exception {
		for (int i=0; i < NUM_RUNS; i++) {
			runTest();
		}
	}
	
	private void runTest() throws Exception {
		new File(FILE_NAME).delete();

		Node[] nodes = new Node[NUM_NODES];

		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new Node(i, NUM_NODES, NUM_MESSAGES, FILE_NAME, FILE_SIZE, RECORD_SIZE);
		}

		for (int i = 0; i < nodes.length; i++) {
			nodes[i].start();
		}

		try {
			for (int i = 0; i < nodes.length; i++) {
				nodes[i].join(WAIT_TIME);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < nodes.length; i++) {
			assertEquals(NUM_MESSAGES, nodes[i].getMessagesReceived());
		}
		
		try {
			for (int i = 0; i < nodes.length; i++) {
				nodes[i].close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	class Node extends Thread {
		
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
			writer = new MappedBusWriter(fileName, fileSize, recordSize);
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
						
						if (token.getTo() == id) {
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
	
	class Token implements MappedBusMessage {
		
		public static final int TYPE = 0;

		private int from;
		
		private int to;
		
		public Token() {
		}

		public Token(int source, int target) {
			this.from = source;
			this.to = target;
		}
		
		public int type() {
			return TYPE;
		}
		
		public int getFrom() {
			return from;
		}

		public void setFrom(int from) {
			this.from = from;
		}

		public int getTo() {
			return to;
		}

		public void setTo(int to) {
			this.to = to;
		}

		@Override
		public String toString() {
			return "Token [from=" + from + ", to=" + to + "]";
		}
		
		public void write(MemoryMappedFile mem, long pos) {
			mem.putInt(pos, from);
			mem.putInt(pos + 4, to);
		}
		
		public void read(MemoryMappedFile mem, long pos) {
			from = mem.getInt(pos);
			to = mem.getInt(pos + 4);
		}
	}
}