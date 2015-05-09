package se.caplogic.mappedbus.sample.token;
import se.caplogic.mappedbus.MemoryMappedFile;
import se.caplogic.mappedbus.Message;


public class Token implements Message {
	
	public static final int TYPE = 0;
	
	public static final int SIZE = 8;

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
	
	public int size() {
		return SIZE;
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