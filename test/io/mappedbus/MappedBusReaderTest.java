package io.mappedbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.mappedbus.MappedBusConstants.StatusFlag;
import io.mappedbus.MappedBusConstants.Length;
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
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE);
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
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
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
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
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
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
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
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		writer.open();

		// write first record
		PriceUpdate priceUpdate = new PriceUpdate(0, 1, 2);
		writer.write(priceUpdate);
		
		// write second record
		priceUpdate = new PriceUpdate(3, 4, 5);
		writer.write(priceUpdate);
		
		// set commit flag to false for the first record
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);		
		mem.putByteVolatile(Structure.Data, StatusFlag.NotSet);
		
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
		mem.putByteVolatile(Structure.Data, StatusFlag.Rollback);
		
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
	
	class PriceUpdate implements MappedBusMessage {
		
		public static final int TYPE = 0;

		private int source;
		
		private int price;
		
		private int quantity;
		
		public PriceUpdate() {
		}

		public PriceUpdate(int source, int price, int quantity) {
			this.source = source;
			this.price = price;
			this.quantity = quantity;
		}
		
		public int type() {
			return TYPE;
		}
		
		public int getSource() {
			return source;
		}

		public void setSource(int source) {
			this.source = source;
		}

		public int getPrice() {
			return price;
		}

		public void setPrice(int price) {
			this.price = price;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		@Override
		public String toString() {
			return "PriceUpdate [source=" + source + ", price=" + price + ", quantity=" + quantity + "]";
		}
		
		public void write(MemoryMappedFile mem, long pos) {
			mem.putInt(pos, source);
			mem.putInt(pos + 4, price);
			mem.putInt(pos + 8, quantity);
		}
		
		public void read(MemoryMappedFile mem, long pos) {
			source = mem.getInt(pos);
			price = mem.getInt(pos + 4);
			quantity = mem.getInt(pos + 8);
		}
	}	
}