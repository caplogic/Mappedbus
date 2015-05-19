package se.caplogic.mappedbus.perf;
import se.caplogic.mappedbus.MappedBusReaderImpl;
import se.caplogic.mappedbus.Message;

public class MessageReader {

	public static void main(String[] args) {
		MessageReader reader = new MessageReader();
		reader.run(args[0]);	
	}

	public void run(String fileName) {
		try {
			MappedBusReaderImpl reader = new MappedBusReaderImpl(fileName, 20000000000L, 12);
			reader.open();

			PriceUpdate priceUpdate = new PriceUpdate();

			Message message = null;

			long start = System.nanoTime();
			for(int i = 0; i < 80000000; i++) {
				while(true) {
					if (reader.hasNext()) {
						if(reader.next()) {
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