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
import java.io.IOException;

import se.caplogic.mappedbus.MappedBusConstants.Commit;
import se.caplogic.mappedbus.MappedBusConstants.Structure;
import se.caplogic.mappedbus.MappedBusConstants.Length;
import se.caplogic.mappedbus.MappedBusConstants.Rollback;

/**
 * Class for reading from the MappedBus.
 *
 */
public class MappedBusReader {

	public static final long MAX_TIMEOUT_COUNT = 100;

	private final String fileName;

	private final long fileSize;

	private final int recordSize;

	private MemoryMappedFile mem;

	private long limit = Structure.Data;

	private long initialLimit;

	private int maxTimeout = 2000;

	protected long timerStart;

	protected long timeoutCounter;

	private boolean typeRead;
	
	/**
	 * Creates a new reader.
	 *
	 * @param fileName the name of the memory mapped file
	 * @param fileSize the maximum size of the file
	 * @param recordSize the maximum size of a record (excluding status flags and meta data)
	 */
	public MappedBusReader(String fileName, long fileSize, int recordSize) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.recordSize = recordSize;
	}

	/**
	 * Opens the reader.
	 *
	 * @throws IOException if there was a problem opening the file
	 */
	public void open() throws IOException {
		try {
			mem = new MemoryMappedFile(fileName, fileSize);
		} catch(Exception e) {
			throw new IOException("Unable to open the file: " + fileName, e);
		}
		initialLimit = mem.getLongVolatile(Structure.Limit);
	}

	/**
	 * Sets the time for a reader to wait for a record to be committed.
	 *
	 * When the timeout occurs the reader will mark the record as "rolled back" and
	 * the record is ignored.
	 * 
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(int timeout) {
		this.maxTimeout = timeout;
	}

	/**
	 * Steps forward to the next record if there's one available.
	 * 
	 * The method has a timeout for how long it will wait for the commit field to be set. When the timeout is
	 * reached it will set the roll back field and skip over the record. 
	 *
	 * @return true, if there's a new record available, otherwise false
	 * @throws EOFException in case the end of the file was reached
	 */
	public boolean next() throws EOFException {
		if (limit >= fileSize) {
			throw new EOFException("End of file was reached");
		}
		if (mem.getLongVolatile(Structure.Limit) <= limit) {
			return false;
		}
		int commit = mem.getIntVolatile(limit);
		int rollback = mem.getIntVolatile(limit + Length.Commit);
		if (rollback == Rollback.Set) {
			limit += Length.RecordHeader + recordSize;
			timeoutCounter = 0;
			timerStart = 0;
			return false;
		}
		if (commit == Commit.Set) {
			timeoutCounter = 0;
			timerStart = 0;
			return true;
		}
		timeoutCounter++;
		if (timeoutCounter >= MAX_TIMEOUT_COUNT) {
			if (timerStart == 0) {
				timerStart = System.currentTimeMillis();
			} else {
				if (System.currentTimeMillis() - timerStart >= maxTimeout) {
					mem.putIntVolatile(limit + Length.Commit, Rollback.Set);
					limit += Length.RecordHeader + recordSize;
					timeoutCounter = 0;
					timerStart = 0;
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Reads the message type.
	 *
	 * @return the message type
	 */
	public int readType() {
		typeRead = true;
		limit += Length.StatusFlags;
		int type = mem.getInt(limit);
		limit += Length.Metadata;
		return type;
	}

	/**
	 * Reads the next message.
	 *
	 * @param message the message object to populate
	 * @return the message object
	 */
	public Message readMessage(Message message) {
		if (!typeRead) {
			readType();
		}
		typeRead = false;
		message.read(mem, limit);
		limit += recordSize;
		return message;
	}

	/**
	 * Reads the next buffer of data.
	 * 
	 * @param dst the input buffer
	 * @param offset the offset in the buffer of the first byte to read data into
	 * @return the length of the record that was read
	 */
	public int readBuffer(byte[] dst, int offset) {
		limit += Length.StatusFlags;
		int length = mem.getInt(limit);
		limit += Length.Metadata;
		mem.getBytes(limit, dst, offset, length);
		limit += recordSize;
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

	/**
	 * Closes the reader.
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