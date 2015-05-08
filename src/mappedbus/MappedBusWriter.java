package mappedbus;
import java.io.File;

import mappedbus.MappedBus.Commit;
import mappedbus.MappedBus.Layout;
import mappedbus.MappedBus.Length;

public class MappedBusWriter {

	private MemoryMappedFile mem;
	
	private long size;

	public void init(String file, long size, boolean append) {
		this.size = size;
		if (!append) {
			new File(file).delete();
		}
		try {
			mem = new MemoryMappedFile(file, size);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		if (append) {
			long limit = mem.getLongVolatile(Layout.Limit);
			if (limit == 0) {
				mem.putLongVolatile(Layout.Limit, Layout.Data);
			}
		} else {
			mem.putLongVolatile(Layout.Limit, Layout.Data);
		}
	}

	public void add(Message message) {
		long limit = addRecord(message.size());
		long commitPos = limit;
		limit += Length.Commit;
		mem.putInt(limit, message.type());
		limit += Length.Metadata;
		message.write(mem, limit);
		limit += message.size();
		commitRecord(commitPos);
	}
	
	public void add(byte[] buffer, int offset, int length) {
		long limit = addRecord(length);
		long commitPos = limit;
		limit += Length.Commit;
		mem.putInt(limit, length);
		limit += Length.Metadata;
		mem.setBytes(limit, buffer, offset, length);
		limit += length;
		commitRecord(commitPos);		
	}
	
	private long addRecord(int recordSize) {
		int entrySize = Length.Commit + Length.Metadata + recordSize;
		long limit;
		while (true) {
			limit = mem.getLongVolatile(Layout.Limit);
			if (limit + entrySize > size) {
				throw new RuntimeException("End of file was reached");
			}
			long oldLimit = limit;
			if (mem.compareAndSwapLong(Layout.Limit, oldLimit, limit + entrySize)) {
				break;
			}
		}
		return limit;
	}

	private void commitRecord(long commitPos) {
		mem.putIntVolatile(commitPos, Commit.Set);
	}
}