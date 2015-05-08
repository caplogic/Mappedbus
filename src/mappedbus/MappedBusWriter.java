package mappedbus;
import java.io.File;

public class MappedBusWriter {

	private MemoryMappedFile mem;
	
	private long size;

	public void init(String file, long size, boolean append) {
		this.size = size;
		if(!append) {
			new File(file).delete();
		}
		try {
			mem = new MemoryMappedFile(file, size);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		if(append) {
			long limit = mem.getLongVolatile(0);
			if(limit == 0) {
				mem.putLongVolatile(0, 8);
			}
		} else {
			mem.putLongVolatile(0, 8);
		}
	}

	public void add(Message message) {
		long limit = addRecord(message.size());
		long commitPos = limit;
		limit += 4;
		mem.putInt(limit, message.type());
		limit += 4;
		message.write(mem, limit);
		limit += message.size();
		commitRecord(commitPos);
	}
	
	public void add(byte[] buffer, int offset, int length) {
		long limit = addRecord(length);
		long commitPos = limit;
		limit += 4;
		mem.putInt(limit, length);
		limit += 4;
		mem.setBytes(limit, buffer, offset, length);
		limit += length;
		commitRecord(commitPos);		
	}
	
	private long addRecord(int recordSize) {
		int entrySize = 4 + 4 + recordSize;
		long limit;
		while(true) {
			limit = mem.getLongVolatile(0);
			long oldLimit = limit;
			if(limit > size) {
				limit = 8;
			}
			if(mem.compareAndSwapLong(0, oldLimit, limit + entrySize)) {
				break;
			}
		}
		return limit;
	}

	private void commitRecord(long commitPos) {
		mem.putIntVolatile(commitPos, 1);
	}
}