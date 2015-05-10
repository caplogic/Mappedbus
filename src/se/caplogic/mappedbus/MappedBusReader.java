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

import se.caplogic.mappedbus.MappedBus.Commit;
import se.caplogic.mappedbus.MappedBus.Layout;
import se.caplogic.mappedbus.MappedBus.Length;

/**
 * Class for reading from the MappedBus.
 *
 */
public class MappedBusReader {

	private final MemoryMappedFile mem;

	private final long size;
	
	private long limit = Layout.Data;
	
	private long initialLimit;
	
	private boolean typeRead;
	
	/**
	 * Creates a new reader.
	 *
	 * @param file the name of the memory mapped file
	 * @param size the maximum size of the file
	 */
	public MappedBusReader(String file, long size) {
		this.size = size;
		try {
			mem = new MemoryMappedFile(file, size);
			initialLimit = mem.getLongVolatile(Layout.Limit);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Indicates whether there's a new record available.
	 *
	 * @return true, if there's a new record available, otherwise false
	 */
	public boolean hasNext() {
		if (limit >= size) {
			throw new RuntimeException("End of file was reached");
		}
		return mem.getLongVolatile(Layout.Limit) > limit;
	}
	
	private void next() {
		while (true) {
			if (mem.getIntVolatile(limit) == Commit.Set) {
				break;
			}
		}
		limit += Length.Commit;
	}

	/**
	 * Reads the message type.
	 *
	 * @return an integer specifying the message type
	 */
	public int readType() {
		typeRead = true;	
		next();
		int type = mem.getInt(limit);
		limit += Length.Metadata;
		return type;
	}

	/**
	 * Reads the next message.
	 *
	 * @param message an object which the message will be read into
	 * @return the message
	 */
	public Message readMessage(Message message) {
		if (!typeRead) {
			readType();
		}
		typeRead = false;
		message.read(mem, limit);
		limit += message.size();
		return message;
	}

	/**
	 * Reads the next buffer of data.
	 *
	 * @param dst a buffer which the data will be copied to
	 * @param offset where in the buffer to copy the data
	 * @return the length of the buffer
	 */
	public int readBuffer(byte[] dst, int offset) {
		next();
		int length = mem.getInt(limit);
		limit += Length.Metadata;
		mem.getBytes(limit, dst, offset, length);
		limit += length;
		return length;
	}
	
	/**
	 * Indicates whether all records available when the reader was created have been read.
	 *
	 * @return true, if all records available from the start was read, otherwise false
	 */
	public boolean hasRecovered() {
		return limit >= initialLimit;
	}
}