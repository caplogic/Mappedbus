package se.caplogic.mappedbus.sample.message;
import se.caplogic.mappedbus.MappedBusWriter;

public class MessageWriter {

	public static void main(String[] args) {
		MessageWriter writer = new MessageWriter();
		writer.run(Integer.valueOf(args[0]));
	}

	public void run(int source) {
		try {
			MappedBusWriter writer = new MappedBusWriter();
			writer.init("/tmp/test-message", 2000000L, true);

			PriceUpdate priceUpdate = new PriceUpdate();

			for (int i = 0; i < 1000; i++) {
				priceUpdate.setSource(source);
				priceUpdate.setPrice(i * 2);
				priceUpdate.setQuantity(i * 4);
				writer.write(priceUpdate);
				Thread.sleep(1000);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}