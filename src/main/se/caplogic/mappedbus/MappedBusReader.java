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

/**
 * Interface for reading from the MappedBus.
 *
 */
public interface MappedBusReader {

	/**
	 * Opens the reader.
	 *
	 * @throws Exception if there was a problem opening the file
	 */
	public void open() throws IOException;

	/**
	 * Sets the time for a reader to wait for a record to be committed.
	 *
	 * When the timeout occurs the reader will mark the record as "rolled back" and
	 * the record is ignored.
	 * 
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(int timeout);

	/**
	 * Indicates whether there's a new record available.
	 *
	 * @return true, if there's a new record available, otherwise false
	 */
	public boolean hasNext() throws EOFException;
	
	/**
	 * Reads the header of the next record.
	 * 
	 * This method waits (busy-wait) until either the rollback, or the commit field is set.
	 * 
	 * The method has a timeout for how long it will wait for the commit field to be set. When the timeout is
	 * reached it will set the rollback field and skip over the record. 
	 *
	 * @return true, if the record is committed and can be read, otherwise false and if so it should be skipped
	 */
	public boolean next();

	/**
	 * Reads the message type.
	 *
	 * @return the message type
	 */
	public int readType();

	/**
	 * Reads the next message.
	 *
	 * @param message the message object to populate
	 * @return the message object
	 */
	public Message readMessage(Message message);

	/**
	 * Reads the next buffer of data.
	 * 
	 * @param dst the input buffer
	 * @param offset the offset in the buffer of the first byte to read data into
	 * @return the length of the record that was read
	 */
	public int readBuffer(byte[] dst, int offset);

	/**
	 * Indicates whether all records available when the reader was created have been read.
	 *
	 * @return true, if all records available from the start was read, otherwise false
	 */
	public boolean hasRecovered();

	/**
	 * Closes the reader.
	 *
	 * @throws IOException if there was an error closing the file
	 */
	public void close() throws IOException;	
}
