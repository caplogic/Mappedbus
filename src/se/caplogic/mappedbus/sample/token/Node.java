package se.caplogic.mappedbus.sample.token;
import se.caplogic.mappedbus.MappedBusReader;
import se.caplogic.mappedbus.MappedBusWriter;
import se.caplogic.mappedbus.Message;

public class Node {

	private MappedBusReader reader;
	
	private MappedBusWriter writer;
	
	public static void main(String[] args) {
		Node reader = new Node();
		reader.init();
		reader.run(Integer.valueOf(args[0]), Integer.valueOf(args[1]));	
	}

	public void init() {
		reader = new MappedBusReader("/tmp/token-test", 2000000L);

		writer = new MappedBusWriter();
		writer.init("/tmp/token-test", 2000000L, true);		
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
				if (reader.hasNext()) {
					System.out.println("Read: " + reader.readMessage(token));

					if(token.getTo() == id) {
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