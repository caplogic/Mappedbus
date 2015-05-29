package io.mappedbus;

import java.io.File;

/**
 * Test program to ensure the ordering between non-volatile and volatile read/writes across
 * processes and CPUs.
 * 
 * Writer:
 * - waits 3 seconds (for the reader to be started)
 * - writes a non-volatile value
 * - writes a volatile value
 *
 * Reader:
 * - reads a non-volatile value (should be zero at this point)
 * - reads a volatile value (should be zero at this point)
 * - in loop reads the volatile value and breaks the loop when the value is no longer zero
 * - reads the non-volatile value and makes sure it's updated
 *
 * To run:
 * in one terminal:
 * > taskset -c 0 java io.mappedbus.MemoryTester writer
 * 
 * in another terminal:
 * > taskset -c 1 java io.mappedbus.MemoryTester reader
 * ..
 * Non Volatile Field was 0 as expected.
 * Volatile Field was 0 as expected.
 * Non Volatile Field was 1 as expected.
 * Volatile Field was 2 as expected.
 *
 */
public class MemoryTester {

	public static long NON_VOLATILE_FIELD = 0;

	public static long VOLATILE_FIELD = 512;

	public static final String FILE_NAME = "/tmp/memory-tester";
	
	public static final long FILE_SIZE = 1000L;

	public static void main(String[] args) throws Exception {
			if (args.length == 1 && args[0].equals("reader")) {
				new MemoryTester().runReader();
			} else if (args.length == 1 && args[0].equals("writer")) {
				new MemoryTester().runWriter();
			} else {
				System.out.println("reader|writer");
			}
	}

	public void runWriter() throws Exception {
		new File(FILE_NAME).delete();
		System.out.println("Waiting 3s, now start the reader");
		Thread.sleep(3000);
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
		mem.putLong(NON_VOLATILE_FIELD, 1);
		mem.putLongVolatile(VOLATILE_FIELD, 2);
		mem.unmap();
	}
	
	public void runReader() throws Exception {
		MemoryMappedFile mem = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
		long nonVolatileField = mem.getLong(NON_VOLATILE_FIELD);
		assertEquals(0, nonVolatileField, "Non Volatile Field");
		long volatileField = mem.getLongVolatile(VOLATILE_FIELD);
		assertEquals(0, volatileField, "Volatile Field");
		long start = System.currentTimeMillis();
		while (true) {
			volatileField = mem.getLongVolatile(VOLATILE_FIELD);
			if (volatileField != 0) {
				break;
			}
			if (System.currentTimeMillis() - start > 3000) {
				System.out.println("TEST FAILED: timeout waiting for the volatile field to change");
				return;
			}
		}
		nonVolatileField = mem.getLong(NON_VOLATILE_FIELD);
		assertEquals(1, nonVolatileField, "Non Volatile Field");
		assertEquals(2, volatileField, "Volatile Field");
		mem.unmap();
	}

	public void assertEquals(long expected, long actual, String name) {
		if(expected == actual) {
			System.out.println(name + " was " + actual + " as expected.");			
		} else {
			System.out.println("TEST FAILED: " + name + " was " + actual + ", but expected " + expected + ".");			
			System.exit(0);
		}
	}
}