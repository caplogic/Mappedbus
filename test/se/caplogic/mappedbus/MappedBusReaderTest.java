package se.caplogic.mappedbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.EOFException;
import java.io.File;

import org.junit.Before;
import org.junit.Test;

import se.caplogic.mappedbus.MappedBusConstants.Length;
import se.caplogic.mappedbus.MappedBusConstants.Structure;

/**
 * This class tests MappedBusReader.
 *
 */
public class MappedBusReaderTest {

	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long FILE_SIZE = 1000;
	
	public static final int RECORD_SIZE = 12;
	
	@Before public void setup() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void testReadEmptyFile() throws Exception {
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		assertEquals(false, reader.hasNext());
		reader.next();
	}
	
	@Test(expected=EOFException.class) public void testReadEOF() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + RECORD_SIZE;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE, false);
		MappedBusReader reader = new MappedBusReader(FILE_NAME, fileSize, RECORD_SIZE);
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, data.length);
		assertEquals(true, reader.hasNext());
		assertEquals(true, reader.next());
		assertEquals(true, reader.hasRecovered());
		assertEquals(RECORD_SIZE, reader.readBuffer(data, 0));		
		reader.hasNext(); // throws EOFException
	}
	
	@Test public void testReadBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
	
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);

		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		
		byte[] buffer = new byte[4];
		assertEquals(true, reader.hasNext());
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(4, reader.readBuffer(buffer, 0));
		assertArrayEquals(data1, buffer);
		
		buffer = new byte[3];
		assertEquals(true, reader.hasNext());
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(3, reader.readBuffer(buffer, 0));
		assertArrayEquals(data2, buffer);
		
		assertEquals(false, reader.hasNext());
		assertEquals(true, reader.hasRecovered());
	}
	
	@Test public void testReadMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
	
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);

		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(true, reader.next());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(0, priceUpdate.getSource());
		assertEquals(1, priceUpdate.getPrice());
		assertEquals(2, priceUpdate.getQuantity());
		
		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(true, reader.next());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		assertEquals(false, reader.hasNext());
		assertEquals(true, reader.hasRecovered());
	}
	
	@Test public void testCrashBeforeCommit() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
		mem.putIntVolatile(Structure.Data, 0);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);

		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(false, reader.next());
		
		assertEquals(true, reader.hasNext());
		assertEquals(false, reader.hasRecovered());
		assertEquals(true, reader.next());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		assertEquals(false, reader.hasNext());
		assertEquals(true, reader.hasRecovered());		
	}
	
}