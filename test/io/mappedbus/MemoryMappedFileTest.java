package io.mappedbus;

import static org.junit.Assert.assertEquals;

import java.io.File;

import io.mappedbus.MemoryMappedFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MemoryMappedFileTest {

	public static final String FILE_NAME = "/tmp/memorymappedfile-test";

	public static final long FILE_SIZE = 1000L;
	
	@Before public void before() {
		new File(FILE_NAME).delete();
	}
	
	@After public void after() {
		new File(FILE_NAME).delete();
	}	

	@Test public void testVolatility() {
		final int LIMIT = 0;
		final int COMMIT = 8;
		final int DATA = 16;

		Thread writer = new Thread() {
			public void run() {
				try {
					MemoryMappedFile m = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
					Thread.sleep(500);
					m.putLongVolatile(LIMIT, 1);
					Thread.sleep(500);
					m.putLong(DATA, 2);
					m.putLongVolatile(COMMIT, 1);
					m.unmap();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
		writer.start();

		try {
			MemoryMappedFile m = new MemoryMappedFile(FILE_NAME, FILE_SIZE);
			long limit = m.getLong(LIMIT);
			assertEquals(0, limit);
			while (true) {
				limit = m.getLongVolatile(LIMIT);
				if (limit != 0) {
					assertEquals(1, limit);
					break;
				}
			}
			long commit = m.getLongVolatile(COMMIT);
			long data = m.getLong(DATA);
			assertEquals(0, commit);
			assertEquals(0, data);
			while (true) {
				commit = m.getLongVolatile(COMMIT);
				if (commit != 0) {
					assertEquals(1, commit);
					break;
				}
			}
			data = m.getLong(DATA);
			assertEquals(2, data);
			m.unmap();
			writer.join();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}