package io.mappedbus.sample.token;
import io.mappedbus.MemoryMappedFile;
import io.mappedbus.MappedBusMessage;


public class Token implements MappedBusMessage {
	
	public static final int TYPE = 0;

	private int from;
	
	private int to;
	
	public Token() {
	}

	public Token(int source, int target) {
		this.from = source;
		this.to = target;
	}
	
	public int type() {
		return TYPE;
	}
	
	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	@Override
	public String toString() {
		return "Token [from=" + from + ", to=" + to + "]";
	}
	
	public void write(MemoryMappedFile mem, long pos) {
		mem.putInt(pos, from);
		mem.putInt(pos + 4, to);
	}
	
	public void read(MemoryMappedFile mem, long pos) {
		from = mem.getInt(pos);
		to = mem.getInt(pos + 4);
	}

}