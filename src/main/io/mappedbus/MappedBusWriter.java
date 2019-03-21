/* 
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
package io.mappedbus;
import io.mappedbus.MappedBusConstants.StatusFlag;
import io.mappedbus.MappedBusConstants.Length;
import io.mappedbus.MappedBusConstants.Structure;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for writing messages to the bus.
 * <p>
 * Messages can either be message based or byte array based.
 * <p>
 * The typical usage is as follows:
 * <pre>
 * {@code
 * // Construct a writer
 * MappedBusWriter writer = new MappedBusWriter("/tmp/test", 100000L, 32);
 * writer.open();
 * 
 * // A: write an object based message
 * PriceUpdate priceUpdate = new PriceUpdate();
 *
 * writer.write(priceUpdate);
 * 
 * // B: write a byte array based message
 * byte[] buffer = new byte[32];
 *
 * writer.write(buffer, 0, buffer.length);
 *
 * // Close the writer
 * writer.close();
 * }
 * </pre>
 */
public class MappedBusWriter {

	private MemoryMappedFile mem;
	
	private final String fileName;
	
	private final long fileSize;

	private final int entrySize;

	/**
	 * Constructs a new writer.
	 * 
	 * @param fileName the name of the memory mapped file
	 * @param fileSize the maximum size of the file
	 * @param recordSize the maximum size of a record (excluding status flags and meta data)
	 */
	public MappedBusWriter(String fileName, long fileSize, int recordSize) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.entrySize = recordSize + Length.RecordHeader;
	}
	
	/**
	 * Opens the writer.
	 *
	 * @throws IOException if there was an error opening the file
	 */
	public void open() throws IOException {
		try {
			mem = new MemoryMappedFile(fileName, fileSize);
		} catch(Exception e) {
			throw new IOException("Unable to open the file: " + fileName, e);
		}
		mem.compareAndSwapLong(Structure.Limit, 0, Structure.Data);
	}

	/**
	 * Writes a message.
	 *
	 * @param message the message object to write
	 * @return returns true if the message could be written, or otherwise false
	 * @throws EOFException in case the end of the file was reached
	 */
	public boolean write(MappedBusMessage message) throws EOFException {
		long commitPos = writeRecord(message);
		return commit(commitPos);
	}

	protected long writeRecord(MappedBusMessage message) throws EOFException {
		long limit = allocate();
		long commitPos = limit;
		limit += Length.StatusFlag;
		mem.putInt(limit, message.type());
		limit += Length.Metadata;
		message.write(mem, limit);
		return commitPos;
	}

	/**
	 * Writes a buffer of data.
	 *
	 * @param src the output buffer
	 * @param offset the offset in the buffer of the first byte to write
	 * @param length the length of the data
	 * @return returns true if the message could be written, or otherwise false
	 * @throws EOFException in case the end of the file was reached
	 */
	public boolean write(byte[] src, int offset, int length) throws EOFException {
		long commitPos = writeRecord(src, offset, length);
		return commit(commitPos);
	}

	protected long writeRecord(byte[] src, int offset, int length) throws EOFException {
		long limit = allocate();
		long commitPos = limit;
		limit += Length.StatusFlag;
		mem.putInt(limit, length);
		limit += Length.Metadata;
		mem.setBytes(limit, src, offset, length);
		return commitPos;
	}

	private long allocate() throws EOFException {
		long limit = mem.getAndAddLong(Structure.Limit, entrySize);
		if (limit + entrySize > fileSize) {
			throw new EOFException("End of file was reached");
		}
		return limit;
	}

	protected boolean commit(long commitPos) {
		return mem.compareAndSwapInt(commitPos, StatusFlag.NotSet, StatusFlag.Commit);
	}

	/**
	 * Closes the writer.
	 *
	 * @throws IOException if there was an error closing the file
	 */
	public void close() throws IOException {
		try {
			mem.unmap();
		} catch(Exception e) {
			throw new IOException("Unable to close the file", e);
		}
	}
}