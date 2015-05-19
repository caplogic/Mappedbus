package se.caplogic.mappedbus;

import static org.junit.Assert.assertEquals;

import java.io.EOFException;
import java.io.File;

import org.junit.Before;
import org.junit.Test;

import se.caplogic.mappedbus.MappedBusConstants.Structure;
import se.caplogic.mappedbus.MappedBusConstants.Length;

/**
 * This class tests MappedBusWriter.
 *
 */
public class MappedBusWriterTest {
	
	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long FILE_SIZE = 1000;
	
	public static final int RECORD_SIZE = 12;
	
	@Before public void setup() {
		new File(FILE_NAME).delete();
	}
	
	@Test(expected=EOFException.class) public void testWriteEOF() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + RECORD_SIZE - 4;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE, false);
		writer.open();
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, RECORD_SIZE); // throws EOFException
	}
	
	@Test(expected=EOFException.class) public void testWriteEOF2() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + (2 * RECORD_SIZE) - 4;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE, false);
		writer.open();
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, RECORD_SIZE);
		writer.write(data, 0, RECORD_SIZE); // throws EOFException
	}
	
	@Test public void testWriteBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		assertEquals(Structure.Data + Length.Commit + Length.Rollback + Length.Metadata + RECORD_SIZE , mem.getLongVolatile(Structure.Limit));
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);
		assertEquals(Structure.Data + 2 * (Length.Commit + Length.Rollback + Length.Metadata + RECORD_SIZE), mem.getLongVolatile(Structure.Limit));
	}

	@Test public void testWriteMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();
	
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		PriceUpdate priceUpdate = new PriceUpdate();
		writer.write(priceUpdate);
		assertEquals(Structure.Data + Length.Commit + Length.Rollback + Length.Metadata + PriceUpdate.SIZE , mem.getLongVolatile(Structure.Limit));
		
		writer.write(priceUpdate);
		assertEquals(Structure.Data + 2 * (Length.Commit + Length.Rollback + Length.Metadata + PriceUpdate.SIZE), mem.getLongVolatile(Structure.Limit));
	}

}