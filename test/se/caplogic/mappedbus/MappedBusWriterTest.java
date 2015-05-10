package se.caplogic.mappedbus;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import se.caplogic.mappedbus.MappedBus.Layout;
import se.caplogic.mappedbus.MappedBus.Length;

public class MappedBusWriterTest {
	
	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long LENGTH = 1000;
	
	@Before public void setup() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void testWriteBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter();
		writer.init(FILE_NAME, LENGTH, false);
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, LENGTH);
				
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		assertEquals(Layout.Data + Length.Commit + Length.Metadata + 4 , mem.getLongVolatile(Layout.Limit));
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);
		assertEquals(Layout.Data + 2 * (Length.Commit + Length.Metadata) + 4 + 3, mem.getLongVolatile(Layout.Limit));
	}

	@Test public void testWriteMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter();
		writer.init(FILE_NAME, LENGTH, false);
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, LENGTH);
				
		PriceUpdate priceUpdate = new PriceUpdate();
		writer.write(priceUpdate);
		assertEquals(Layout.Data + Length.Commit + Length.Metadata + PriceUpdate.SIZE , mem.getLongVolatile(Layout.Limit));
		
		writer.write(priceUpdate);
		assertEquals(Layout.Data + 2 * (Length.Commit + Length.Metadata + PriceUpdate.SIZE), mem.getLongVolatile(Layout.Limit));
	}

}