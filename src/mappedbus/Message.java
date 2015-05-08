package mappedbus;

public interface Message {

	public void write(MemoryMappedFile mem, long pos);
	
	public void read(MemoryMappedFile mem, long pos);
	
	public int type();
	
	public int size();
	
}
