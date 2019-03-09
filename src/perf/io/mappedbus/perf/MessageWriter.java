package io.mappedbus.perf;
import io.mappedbus.MappedBusWriter;

import java.io.File;

public class MessageWriter {

	public static void main(String[] args) {
		MessageWriter writer = new MessageWriter();
		writer.run(args[0]);
	}

	public void run(String fileName) {
		try {
			MappedBusWriter writer = new MappedBusWriter(fileName, 20000000000L, 12);
			writer.open();
			
			PriceUpdate priceUpdate = new PriceUpdate();
			
			for (int i = 0; i < 80000000; i++) {
				writer.write(priceUpdate);
			}
			
			System.out.println("Done");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}