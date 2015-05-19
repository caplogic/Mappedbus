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
import java.io.EOFException;
import java.io.File;

import se.caplogic.mappedbus.MappedBusConstants.Commit;
import se.caplogic.mappedbus.MappedBusConstants.Structure;
import se.caplogic.mappedbus.MappedBusConstants.Length;

/**
 * Class for writing to the MappedBus.
 *
 */
public class MappedBusWriter {

	private final MemoryMappedFile mem;
	
	private final long fileSize;
	
	private final int recordSize;

	/**
	 * Creates a new writer.
	 * 
	 * @param fileName the name of the memory mapped file
	 * @param size the maximum size of the file
	 * @param recordSize the maximum size of a record (excluding status flags and meta data)
	 * @param append whether to append to the file (will create a new file if false)
	 */
	public MappedBusWriter(String fileName, long fileSize, int recordSize, boolean append) {
		this.fileSize = fileSize;
		this.recordSize = recordSize;
		if (!append) {
			new File(fileName).delete();
		}
		try {
			mem = new MemoryMappedFile(fileName, fileSize);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		if (append) {
			/*long limit = mem.getLongVolatile(Structure.Limit);
			if (limit == 0) {
				mem.putLongVolatile(Structure.Limit, Structure.Data);
			}*/
			mem.compareAndSwapLong(Structure.Limit, 0, Structure.Data);
		} else {
			mem.putLongVolatile(Structure.Limit, Structure.Data);
		}
	}

	/**
	 * Writes a message.
	 *
	 * @param message the message object to write
	 */
	public void write(Message message) throws EOFException {
		long limit = allocate();
		long commitPos = limit;
		limit += Length.StatusFlags;
		mem.putInt(limit, message.type());
		limit += Length.Metadata;
		message.write(mem, limit);
		commit(commitPos);
	}

	/**
	 * Writes a buffer of data.
	 *
	 * @param src the output buffer
	 * @param offset the offset in the buffer of the first byte to write
	 * @param length the length of the data
	 */
	public void write(byte[] src, int offset, int length) throws EOFException {
		long limit = allocate();
		long commitPos = limit;
		limit += Length.StatusFlags;
		mem.putInt(limit, length);
		limit += Length.Metadata;
		mem.setBytes(limit, src, offset, length);
		commit(commitPos);		
	}
	
	private long allocate() throws EOFException {
		int entrySize = Length.RecordHeader + recordSize;
		long limit;
		while (true) {
			limit = mem.getLongVolatile(Structure.Limit);
			if (limit + entrySize > fileSize) {
				throw new EOFException("End of file was reached");
			}
			if (mem.compareAndSwapLong(Structure.Limit, limit, limit + entrySize)) {
				break;
			}
		}
		return limit;
	}

	private void commit(long commitPos) {
		mem.putIntVolatile(commitPos, Commit.Set);
	}
	
	/**
	 * Closes the writer.
	 *
	 * @throws Exception if there was an error closing the file
	 */
	public void close() throws Exception {
		mem.unmap();
	}
}