package io.mappedbus;

import static org.junit.Assert.assertEquals;
import io.mappedbus.MappedBusMessage;
import io.mappedbus.MappedBusReader;
import io.mappedbus.MappedBusWriter;
import io.mappedbus.MemoryMappedFile;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests that records written by multiple concurrent writers are stored correctly.
 *
 * For more exhaustive testing NUM_RUNS can be increased.
 *
 */
public class ObjectBasedIntegrityTest {

	public static final String FILE_NAME = "/tmp/objectbased-integrity-test";

	public static final long FILE_SIZE = 40000000;

	public static final int NUM_READERS = 8;

	public static final int NUM_WRITERS = 4;

	public static final int RECORD_LENGTH = 16;

	public static final int NUM_RECORDS_PER_WRITER = 300000;

	public static final int NUM_RECORDS = NUM_RECORDS_PER_WRITER * NUM_WRITERS;

	public static final int NUM_RUNS = 10;
	
	@Before public void before() {
		new File(FILE_NAME).delete();
	}
	
	@After public void after() {
		new File(FILE_NAME).delete();
	}	

	@Test public void test() throws Exception {
		for (int i=0; i < NUM_RUNS; i++) {
			runTest();
		}
	}

	private void runTest() throws Exception {
		new File(FILE_NAME).delete();

		Writer[] writers = new Writer[NUM_WRITERS];
		for (int i = 0; i < writers.length; i++) {
			writers[i] = new Writer(i + 1);
		}
		for (int i = 0; i < writers.length; i++) {
			writers[i].start();
		}
		Reader[] readers = new Reader[NUM_READERS];
		for (int i=0; i < readers.length; i++) {
			readers[i] = new Reader();
		}
		for (int i=0; i < readers.length; i++) {
			readers[i].start();
		}
		for (int i = 0; i < writers.length; i++) {
			writers[i].join();
		}
		for (int i = 0; i < readers.length; i++) {
			readers[i].join();
		}
		for (int i = 0; i < readers.length; i++) {
			assertEquals(false, readers[i].hasFailed());
			assertEquals(NUM_RECORDS, readers[i].getRecordsReceived());
		}
	}

	class Writer extends Thread {

		private final int id;

		public Writer(int id) {
			this.id = id;
		}

		public void run() {
			try {
				MappedBusWriter writer = new MappedBusWriter(ObjectBasedIntegrityTest.FILE_NAME, ObjectBasedIntegrityTest.FILE_SIZE, ObjectBasedIntegrityTest.RECORD_LENGTH);
				writer.open();
				Record record = new Record();
				record.setKey(id);
				long value = id;
				for (int i=0; i < ObjectBasedIntegrityTest.NUM_RECORDS_PER_WRITER; i++) {
					record.setKey(id);
					record.setValue(value);
					value += NUM_WRITERS;
					writer.write(record);
				}
				writer.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	class Reader extends Thread {

		private int recordsReceived;

		private boolean failed;

		public void run() {
			try {
				MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_LENGTH);
				reader.open();
				long[] counters = new long[NUM_WRITERS];
				for (int i=0; i < counters.length; i++) {
					counters[i] = i+1;
				}
				Record record = new Record();
				while (true) {
					if (reader.next()) {
						int type = reader.readType();
						if (type == Record.TYPE) {
							reader.readMessage(record);
							long key = record.getKey();
							long value = record.getValue();
							long expected = counters[(int)(key-1)];
							if (expected != value) {
								System.out.println("Expected: " + counters[(int)(key-1)] + ", actual: " + value);
								failed = true;				
								return;
							}
							counters[(int)(key-1)] += NUM_WRITERS;
						}
						recordsReceived++;
						if (recordsReceived >= NUM_RECORDS) {
							break;
						}
					}
				}
				reader.close();				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		public boolean hasFailed() {
			return failed;
		}

		public int getRecordsReceived() {
			return recordsReceived;
		}
	}

	class Record implements MappedBusMessage {

		public static final int TYPE = 0;

		private long key;

		private long value;

		public int type() {
			return TYPE;
		}

		public long getKey() {
			return key;
		}

		public void setKey(long key) {
			this.key = key;
		}

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}

		public void write(MemoryMappedFile mem, long pos) {
			mem.putLong(pos, key);
			mem.putLong(pos + 8, value);
		}

		public void read(MemoryMappedFile mem, long pos) {
			key = mem.getLong(pos);
			value = mem.getLong(pos + 8);
		}
	}
}