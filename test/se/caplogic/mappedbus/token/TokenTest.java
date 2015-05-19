package se.caplogic.mappedbus.token;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class TokenTest {

	public static final String FILE_NAME = "/tmp/token-test";

	public static final int RECORD_SIZE = 8;

	public static final long FILE_SIZE = 20000000L;

	public static final int NUM_MESSAGES = 100000;

	public static final int NUM_NODES = 3;
	
	public static final int WAIT_TIME = 10000;

	@Test public void test() {
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
	}

}