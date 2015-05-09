package se.caplogic.mappedbus.perf;
import java.io.File;

import se.caplogic.mappedbus.MappedBusWriter;

public class MessageWriter {

	public static void main(String[] args) {
		MessageWriter writer = new MessageWriter();
		writer.run(args[0]);
	}

	public void run(String fileName) {
		try {
			new File(fileName).delete();
			
			MappedBusWriter writer = new MappedBusWriter();
			writer.init(fileName, 20000000000L, false);

			PriceUpdate priceUpdate = new PriceUpdate();
			
			for(int i = 0; i < 80000000; i++) {
				writer.add(priceUpdate);
			}
			
			System.out.println("Done");
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}