package io.mappedbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MemoryMappedFile;
import io.mappedbus.MappedBusConstants.Commit;
import io.mappedbus.MappedBusConstants.Length;
import io.mappedbus.MappedBusConstants.Rollback;
import io.mappedbus.MappedBusConstants.Structure;

import java.io.EOFException;
import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests MappedBusReader.
 *
 */
public class MappedBusReaderTest {

	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long FILE_SIZE = 1000;
	
	public static final int RECORD_SIZE = 12;
	
	@Before public void before() {
		new File(FILE_NAME).delete();
	}
	
	@After public void after() {
		new File(FILE_NAME).delete();
	}
	
	@Test public void testReadEmptyFile() throws Exception {
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		reader.open();
		assertEquals(false, reader.next());
	}
	
	@Test(expected=EOFException.class) public void testReadEOF() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + RECORD_SIZE;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE, false);
		writer.open();
		MappedBusReader reader = new MappedBusReader(FILE_NAME, fileSize, RECORD_SIZE);
		reader.open();
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, data.length);
		assertEquals(true, reader.next());
		assertEquals(true, reader.hasRecovered());
		assertEquals(RECORD_SIZE, reader.readBuffer(data, 0));		
		reader.next(); // throws EOFException
	}
	
	@Test public void testReadBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();
		
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);

		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		reader.open();
		
		byte[] buffer = new byte[4];
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(4, reader.readBuffer(buffer, 0));
		assertArrayEquals(data1, buffer);
		
		buffer = new byte[3];
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(3, reader.readBuffer(buffer, 0));
		assertArrayEquals(data2, buffer);
		
		assertEquals(false, reader.next());
		assertEquals(true, reader.hasRecovered());
	}
	
	@Test public void testReadMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();
	
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		reader.open();

		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(0, priceUpdate.getSource());
		assertEquals(1, priceUpdate.getPrice());
		assertEquals(2, priceUpdate.getQuantity());
		
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		assertEquals(false, reader.next());
		assertEquals(true, reader.hasRecovered());
	}
	
	@Test public void testCrashBeforeCommitRollbackBySameReader() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();

		// write first record
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		// write second record
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		// set commit flag to false for the first record
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
		mem.putIntVolatile(Structure.Data, 0);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		reader.setTimeout(0);
		reader.open();

		assertEquals(0, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);
		for (int i = 0; i < MappedBusReader.MAX_TIMEOUT_COUNT - 1; i++) {
			assertEquals(false, reader.next());	
		}
		assertEquals(99, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);
		
		// the reader starts the timer
		assertEquals(false, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(100, reader.timeoutCounter);
		assertTrue(reader.timerStart > 0);
		
		// the reader sets the roll back flag and skips the record
		assertEquals(false, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);
		
		// the reader reads the second record
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		// no more records available
		assertEquals(false, reader.next());
		assertEquals(true, reader.hasRecovered());		
	}

	@Test public void testCrashBeforeCommitRollbackByDifferentReaderBefore() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);
		writer.open();

		// write first record
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		// write second record
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		// set commit flag to false for the first record
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);		
		mem.putIntVolatile(Structure.Data, Commit.NotSet);
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		reader.setTimeout(0);
		reader.open();

		assertEquals(0, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);
		for (int i = 0; i < MappedBusReader.MAX_TIMEOUT_COUNT - 10; i++) {
			assertEquals(false, reader.next());	
		}
		assertEquals(MappedBusReader.MAX_TIMEOUT_COUNT - 10, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);

		// another reader sets the rollback flag
		mem.putIntVolatile(Structure.Data + Length.Commit, Rollback.Set);
		
		// the reader skips the record
		assertEquals(false, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.timeoutCounter);
		assertEquals(0, reader.timerStart);
	
		// the reader reads the second record
		assertEquals(true, reader.next());
		assertEquals(false, reader.hasRecovered());
		assertEquals(0, reader.readType());
		reader.readMessage(priceUpdate);
		assertEquals(3, priceUpdate.getSource());
		assertEquals(4, priceUpdate.getPrice());
		assertEquals(5, priceUpdate.getQuantity());
		
		// no more records available
		assertEquals(false, reader.next());
		assertEquals(true, reader.hasRecovered());
	}
	
}