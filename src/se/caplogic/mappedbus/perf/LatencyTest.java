package se.caplogic.mappedbus.perf;

import org.HdrHistogram.Histogram;

import se.caplogic.mappedbus.MappedBusWriter;

public class LatencyTest {

	public static final String FILE_NAME = "/home/mikael/tmp/test";

	public static final long FILE_SIZE = 20000000000L;

	public static final int RECORD_SIZE = 12;

	public static final int NUM_MESSAGES =  1000000;

	public static final int WARMUP_ROUNDS = 10;

	public static final int RECOVERY_TIME = 2000;

	public static void main(String[] args) throws Exception {
		LatencyTest latencyTest = new LatencyTest();
		latencyTest.run();
	}

	public void run() throws Exception {
		MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE, false);

		PriceUpdate priceUpdate = new PriceUpdate();

		for (int r = 0; r < 1000; r++) {
			Histogram histogram = new Histogram(3);
			for (int i = 0; i < NUM_MESSAGES; i++) {
				long startTime = System.nanoTime();
				writer.write(priceUpdate);
				histogram.recordValue(System.nanoTime() - startTime);
			}

			if (r > WARMUP_ROUNDS) {
				histogram.outputPercentileDistribution(System.out, 1.0);
			}

			Thread.sleep(RECOVERY_TIME);
		}
	}
}