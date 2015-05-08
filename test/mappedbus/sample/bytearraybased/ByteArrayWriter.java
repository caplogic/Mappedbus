package mappedbus.sample.bytearraybased;
import java.util.Arrays;

import mappedbus.MappedBusWriter;

public class ByteArrayWriter {

	public static void main(String[] args) {
		ByteArrayWriter writer = new ByteArrayWriter();
		writer.run(Integer.valueOf(args[0]));
	}

	public void run(int source) {
		try {
			MappedBusWriter writer = new MappedBusWriter();
			writer.init("/tmp/test-bytearray", 2000000L, true);

			byte[] buffer = new byte[10];

			for(int i = 0; i < 1000; i++) {
				Arrays.fill(buffer, (byte)source);
				writer.add(buffer, 0, buffer.length);
				Thread.sleep(1000);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}