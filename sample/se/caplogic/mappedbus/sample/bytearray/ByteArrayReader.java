package se.caplogic.mappedbus.sample.bytearray;
import java.util.Arrays;

import se.caplogic.mappedbus.MappedBusReader;

public class ByteArrayReader {

	public static void main(String[] args) {
		ByteArrayReader reader = new ByteArrayReader();
		reader.run();	
	}

	public void run() {
		try {
			MappedBusReader reader = new MappedBusReader("/tmp/test-bytearray", 2000000L, 10);

			byte[] buffer = new byte[10];

			while (true) {
				if (reader.hasNext()) {
					if (!reader.next()) {
						continue; // the record was abandoned, skip it
					}
					int length = reader.readBuffer(buffer, 0);
					System.out.println("Read: length = " + length + ", data= "+ Arrays.toString(buffer));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}