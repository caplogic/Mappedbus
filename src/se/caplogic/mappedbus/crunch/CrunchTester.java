package se.caplogic.mappedbus.crunch;

import java.io.File;
import java.util.Arrays;

import se.caplogic.mappedbus.MappedBusReader;
import se.caplogic.mappedbus.MappedBusWriter;

public class CrunchTester {
	
	public static final String FILE_NAME = "/tmp/crunch-test";
	
	public static final long FILE_SIZE = 2000000L;
	
	public static final int NUM_WRITERS = 10;
	
	public static final int RECORD_LENGTH = 10;
	
	public static final int NUM_RECORDS = 10000;
	
	public static void main(String[] args) throws Exception {
		CrunchTester tester = new CrunchTester();
		tester.run();
	}
	
	public void run() throws Exception {
		new File(FILE_NAME).delete();

		Writer[] writers = new Writer[NUM_WRITERS];
		for(int i = 0; i < writers.length; i++) {
			writers[i] = new Writer(i);
		}
		for(int i = 0; i < writers.length; i++) {
			writers[i].start();
		}
		for(int i = 0; i < writers.length; i++) {
			writers[i].join();
		}
		
		MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE);
		byte[] data = new byte[RECORD_LENGTH];
		while(reader.hasNext()) {
			reader.readBuffer(data, 0);
			for(int i=0; i < data.length; i++) {
				System.out.println("Read: " + Arrays.toString(data));
				if(data[0] != data[i]) {
					System.out.println("---");
					System.out.println("TEST FAILED!");
					return;
				}
			}
		}
		System.out.println("---");
		System.out.println("Test passed.");
	}

}

class Writer extends Thread {
	
	private final int id;
	
	public Writer(int id) {
		this.id = id;
	}
	
	public void run() {
		try {
			MappedBusWriter writer = new MappedBusWriter();
			writer.init(CrunchTester.FILE_NAME, CrunchTester.FILE_SIZE, false);

			byte[] data = new byte[CrunchTester.RECORD_LENGTH];
			Arrays.fill(data, (byte)id);
			
			for(int i=0; i < CrunchTester.NUM_RECORDS; i++) {
				writer.write(data, 0, data.length);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}