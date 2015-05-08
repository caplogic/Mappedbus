package mappedbus;

public class MappedBusReader {

	private final MemoryMappedFile mem;

	private long limit = 8;
	
	public MappedBusReader(String file, long size) {
		try {
			mem = new MemoryMappedFile(file, size);			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasNext() {
		long newLimit = mem.getLongVolatile(0);
		if(limit > newLimit) {
			limit = 8;
			return true;
		}
		return newLimit > limit;
	}
	
	private void next() {
		while(true) {
			if(mem.getIntVolatile(limit) == 1) {
				break;
			}
		}
		limit += 4;	
	}

	public int readType() {
		next();
		int type = mem.getInt(limit);
		limit += 4;
		return type;
	}
	
	public int readBuffer(byte[] dst, int offset) {
		next();
		int length = mem.getInt(limit);
		limit += 4;
		mem.getBytes(limit, dst, offset, length);
		limit += length;
		return length;
	}
	
	public void readMessage(Message message) {
		message.read(mem, limit);
		limit += message.size();
	}
}