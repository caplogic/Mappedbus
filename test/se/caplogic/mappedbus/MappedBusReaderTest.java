package se.caplogic.mappedbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class MappedBusReaderTest {

	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long LENGTH = 1000;
	
	@Before public void setup() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void testReadBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter();
		writer.init(FILE_NAME, LENGTH, false);
		
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);

		MappedBusReader reader = new MappedBusReader(FILE_NAME, LENGTH);
		
		byte[] buffer = new byte[4];
		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(4, reader.readBuffer(buffer, 0));
		assertArrayEquals(data1, buffer);
		
		buffer = new byte[3];
		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(3, reader.readBuffer(buffer, 0));
		assertArrayEquals(data2, buffer);
		
		assertEquals(false, reader.hasNext());
		assertEquals(true, reader.hasRecovered());
	}
	
	@Test public void testReadMessage() {
		MappedBusWriter writer = new MappedBusWriter();
		writer.init(FILE_NAME, LENGTH, false);
		
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, LENGTH);

		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(0, priceUpdate.getSource());
		assertEquals(1, priceUpdate.getPrice());
		assertEquals(2, priceUpdate.getQuantity());
		
		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		assertEquals(false, reader.hasNext());
		assertEquals(true, reader.hasRecovered());
	}
	
}