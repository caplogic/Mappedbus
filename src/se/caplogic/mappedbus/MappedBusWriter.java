/* 
* Copyright 2015 Caplogic AB. 
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/
package se.caplogic.mappedbus;
import java.io.File;

import se.caplogic.mappedbus.MappedBus.Commit;
import se.caplogic.mappedbus.MappedBus.Layout;
import se.caplogic.mappedbus.MappedBus.Length;

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
