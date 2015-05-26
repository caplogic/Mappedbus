package io.mappedbus.sample.object;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusMessage;

public class ObjectReader {

	public static void main(String[] args) {
		ObjectReader reader = new ObjectReader();
		reader.run();	
	}

	public void run() {
		try {
			MappedBusReader reader = new MappedBusReader("/tmp/test-message", 2000000L, 12);
			reader.open();

			PriceUpdate priceUpdate = new PriceUpdate();
			
			MappedBusMessage message = null;

			while (true) {
				if (reader.next()) {
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