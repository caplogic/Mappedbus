package io.mappedbus.sample.token;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MappedBusMessage;

public class Node {

	private MappedBusReader reader;
	
	private MappedBusWriter writer;
	
	public static void main(String[] args) throws Exception {
		Node reader = new Node();
		reader.init();
		reader.run(Integer.valueOf(args[0]), Integer.valueOf(args[1]));	
	}

	public void init() throws Exception {
		writer = new MappedBusWriter("/tmp/token-test", 2000000L, 8);
		writer.open();
		
		reader = new MappedBusReader("/tmp/token-test", 2000000L, 8);
		reader.open();
	}

	public void run(int id, int numberOfNodes) {
		try {
			Token token = new Token();

			if (id == 0) {
				token.setFrom(id);
				token.setTo(1);
				writer.write(token);
			}

			while (true) {
				if (reader.next()) {
					System.out.println("Read: " + reader.readMessage(token));

					if (token.getTo() == id) {
						Thread.sleep(1000);
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
}