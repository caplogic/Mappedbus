package io.mappedbus.perf;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusMessage;

public class MessageReader {

	public static void main(String[] args) {
		MessageReader reader = new MessageReader();
		reader.run(args[0]);	
	}

	public void run(String fileName) {
		try {
			MappedBusReader reader = new MappedBusReader(fileName, 20000000000L, 12);
			reader.open();

			PriceUpdate priceUpdate = new PriceUpdate();

			MappedBusMessage message = null;

			System.out.println("Waiting for first message");

			long start = 0;
			for (int i = 0; i < 80000000; i++) {
				while (true) {
					if (reader.next()) {
						if (start == 0) {
							start = System.nanoTime();
							System.out.println("Got first message");
						}
						int type = reader.readType();
						switch (type) {
						case PriceUpdate.TYPE:
							message = priceUpdate;
							break;
						default:
							throw new RuntimeException("Unknown type: " + type);
						}
						reader.readMessage(message);
						break;
					}
				}
			}
			long stop = System.nanoTime();
			System.out.println("Elapsed: " + ((stop - start) / 1000000 ) + " ms");
			System.out.println("Per op: " + ((stop - start) / 80000000 ) + " ns");
			System.out.println("Op/s: " + (long)(80000000/((stop-start)/(float)1000000000)));

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}