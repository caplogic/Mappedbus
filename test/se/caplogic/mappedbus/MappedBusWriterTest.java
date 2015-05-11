package se.caplogic.mappedbus;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import se.caplogic.mappedbus.MappedBus.FileStructure;
import se.caplogic.mappedbus.MappedBus.Length;

public class MappedBusWriterTest {
	
	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long FILE_SIZE = 1000;
	
	public static final int RECORD_SIZE = 12;
	
	@Before public void setup() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void testWriteBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		assertEquals(FileStructure.Data + Length.Commit + Length.Rollback + Length.Metadata + RECORD_SIZE , mem.getLongVolatile(FileStructure.Limit));
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);
		assertEquals(FileStructure.Data + 2 * (Length.Commit + Length.Rollback + Length.Metadata + RECORD_SIZE), mem.getLongVolatile(FileStructure.Limit));
	}

	@Test public void testWriteMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
	
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		PriceUpdate priceUpdate = new PriceUpdate();
		writer.write(priceUpdate);
		assertEquals(FileStructure.Data + Length.Commit + Length.Rollback + Length.Metadata + PriceUpdate.SIZE , mem.getLongVolatile(FileStructure.Limit));
		
		writer.write(priceUpdate);
		assertEquals(FileStructure.Data + 2 * (Length.Commit + Length.Rollback + Length.Metadata + PriceUpdate.SIZE), mem.getLongVolatile(FileStructure.Limit));
	}

}