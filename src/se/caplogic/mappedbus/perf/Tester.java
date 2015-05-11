package se.caplogic.mappedbus.perf;

import se.caplogic.mappedbus.MappedBusReader;
import se.caplogic.mappedbus.MemoryMappedFile;

public class Tester {

	public static void main(String[] args) throws Exception {
		Tester t = new Tester();
		for(int i = 0; i < 100; i++)
			t.run();
	}
	
	public void run() throws Exception {
		MemoryMappedFile mem = new MemoryMappedFile("/home/mikael/tmp/test", 1000000000);
		
		
		long start = System.nanoTime();
		for (long i = 0; i < 1000000000; i++) {
			if (mem.getIntVolatile(i) == 1) {
				break;
			}
			
		}
		long stop = System.nanoTime();
		
		System.out.println((stop - start)/((float)1000000) + " ms");
		
		System.out.println((stop - start)/((float)1000000000) + " ns/op");
	}

}
