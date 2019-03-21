package io.mappedbus;

import static org.junit.Assert.assertEquals;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MemoryMappedFile;
import io.mappedbus.MappedBusConstants.Length;
import io.mappedbus.MappedBusConstants.Structure;

import java.io.EOFException;
import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests MappedBusWriter.
 *
 */
public class MappedBusWriterTest {
	
	public static final String FILE_NAME = "/tmp/MappedBusWriterTest";
	
	public static final long FILE_SIZE = 1000;
	
	public static final int RECORD_SIZE = 12;
	
	@Before public void before() {
		new File(FILE_NAME).delete();
	}
	
	@After public void after() {
		new File(FILE_NAME).delete();
	}
	
	@Test(expected=EOFException.class) public void testWriteEOF() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + RECORD_SIZE - 4;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE);
		writer.open();
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, RECORD_SIZE); // throws EOFException
	}
	
	@Test(expected=EOFException.class) public void testWriteEOF2() throws Exception {
		int fileSize = Length.Limit + Length.RecordHeader + (2 * RECORD_SIZE) - 4;
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, fileSize, RECORD_SIZE);
		writer.open();
		byte[] data = new byte[RECORD_SIZE];
		writer.write(data, 0, RECORD_SIZE);
		writer.write(data, 0, RECORD_SIZE); // throws EOFException
	}
	
	@Test public void testWriteBuffer() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		writer.open();
		
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		byte[] data1 = {0, 1, 2, 3};
		writer.write(data1, 0, data1.length);
		assertEquals(Structure.Data + Length.StatusFlag + Length.Metadata + RECORD_SIZE , mem.getLongVolatile(Structure.Limit));
		
		byte[] data2 = {4, 5, 6};
		writer.write(data2, 0, data2.length);
		assertEquals(Structure.Data + 2 * (Length.StatusFlag + Length.Metadata + RECORD_SIZE), mem.getLongVolatile(Structure.Limit));
	}

	@Test public void testWriteMessage() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
		writer.open();
	
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
				
		PriceUpdate priceUpdate = new PriceUpdate();
		writer.write(priceUpdate);
		assertEquals(Structure.Data + Length.StatusFlag + Length.Metadata + RECORD_SIZE , mem.getLongVolatile(Structure.Limit));
		
		writer.write(priceUpdate);
		assertEquals(Structure.Data + 2 * (Length.StatusFlag + Length.Metadata + RECORD_SIZE), mem.getLongVolatile(Structure.Limit));
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