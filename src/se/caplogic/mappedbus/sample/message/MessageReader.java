package se.caplogic.mappedbus.sample.message;
import se.caplogic.mappedbus.MappedBusReader;
import se.caplogic.mappedbus.Message;

public class MessageReader {

	public static void main(String[] args) {
		MessageReader reader = new MessageReader();
		reader.run();	
	}

	public void run() {
		try {
			MappedBusReader reader = new MappedBusReader("/tmp/test-message", 2000000L);

			PriceUpdate priceUpdate = new PriceUpdate();
			
			Message message = null;

			while (true) {
				if (reader.hasNext()) {
					boolean recovered = reader.hasRecovered();
					int type = reader.readType();
					switch (type) {
					case PriceUpdate.TYPE:
						message = priceUpdate;
						break;
					default:
						throw new RuntimeException("Unknown type: " + type);
					}
					reader.readMessage(message);					
					System.out.println("Read: " + message + ", hasRecovered=" + recovered);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}