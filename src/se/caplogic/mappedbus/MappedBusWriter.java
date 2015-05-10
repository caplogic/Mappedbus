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

/**
 * Class for writing to the MappedBus.
 *
 */
public class MappedBusWriter {

	private MemoryMappedFile mem;
	
	private long size;

	/**
	 * Creates a new writer.
	 * 
	 * @param file the name of the memory mapped file
	 * @param size the maximum size of the file
	 * @param append whether to append to the file (will create a new file if false)
	 */
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

	/**
	 * Writes a message.
	 *
	 * @param message the message to be written
	 */
	public void write(Message message) {
		long limit = allocate(message.size());
		long commitPos = limit;
		limit += Length.Commit;
		mem.putInt(limit, message.type());
		limit += Length.Metadata;
		message.write(mem, limit);
		limit += message.size();
		commit(commitPos);
	}
	
	/**
	 * Writes a buffer of data.
	 *
	 * @param buffer the buffer containing data to be written
	 * @param offset where in the buffer to start
	 * @param length the size of the data
	 */
	public void write(byte[] buffer, int offset, int length) {
		long limit = allocate(length);
		long commitPos = limit;
		limit += Length.Commit;
		mem.putInt(limit, length);
		limit += Length.Metadata;
		mem.setBytes(limit, buffer, offset, length);
		limit += length;
		commit(commitPos);		
	}
	
	private long allocate(int recordSize) {
		int entrySize = Length.Commit + Length.Metadata + recordSize;
		long limit;
		while (true) {
			limit = mem.getLongVolatile(Layout.Limit);
			if (limit + entrySize > size) {
				throw new RuntimeException("End of file was reached");
			}
			if (mem.compareAndSwapLong(Layout.Limit, limit, limit + entrySize)) {
				break;
			}
		}
		return limit;
	}

	private void commit(long commitPos) {
		mem.putIntVolatile(commitPos, Commit.Set);
	}
}
