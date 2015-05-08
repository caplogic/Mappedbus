package mappedbus;

import mappedbus.MappedBus.Commit;
import mappedbus.MappedBus.Layout;
import mappedbus.MappedBus.Length;

public class MappedBusReader {

	private final MemoryMappedFile mem;

	private long limit = Layout.Data;
	
	public MappedBusReader(String file, long size) {
		try {
			mem = new MemoryMappedFile(file, size);			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasNext() {
		long newLimit = mem.getLongVolatile(Layout.Limit);
		if (limit > newLimit) {
			limit = Layout.Data;
			return true;
		}
		return newLimit > limit;
	}
	
	private void next() {
		while (true) {
			if (mem.getIntVolatile(limit) == Commit.Set) {
				break;
			}
		}
		limit += Length.Commit;	
	}

	public int readType() {
		next();
		int type = mem.getInt(limit);
		limit += Length.Metadata;
		return type;
	}
	
	public int readBuffer(byte[] dst, int offset) {
		next();
		int length = mem.getInt(limit);
		limit += Length.Metadata;
		mem.getBytes(limit, dst, offset, length);
		limit += length;
		return length;
	}
	
	public void readMessage(Message message) {
		message.read(mem, limit);
		limit += message.size();
	}
}