package se.caplogic.mappedbus;
import se.caplogic.mappedbus.MemoryMappedFile;
import se.caplogic.mappedbus.Message;


public class PriceUpdate implements Message {
	
	public static final int TYPE = 0;
	
	public static final int SIZE = 12;

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
	
	public int size() {
		return SIZE;
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