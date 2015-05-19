package se.caplogic.mappedbus.sample.token;
import se.caplogic.mappedbus.MappedBusReaderImpl;
import se.caplogic.mappedbus.MappedBusWriterImpl;
import se.caplogic.mappedbus.Message;

public class Node {

	private MappedBusReaderImpl reader;
	
	private MappedBusWriterImpl writer;
	
	public static void main(String[] args) throws Exception {
		Node reader = new Node();
		reader.init();
		reader.run(Integer.valueOf(args[0]), Integer.valueOf(args[1]));	
	}

	public void init() throws Exception {
		writer = new MappedBusWriterImpl("/tmp/token-test", 2000000L, 8, true);
		writer.open();
		
		reader = new MappedBusReaderImpl("/tmp/token-test", 2000000L, 8);
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
				if (reader.hasNext()) {
					if(!reader.next()) {
						continue; // the record was abandoned, skip it
					}
					
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