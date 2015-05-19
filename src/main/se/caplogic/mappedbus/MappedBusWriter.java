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
 * Interface for writing to the MappedBus.
 *
 */
public interface MappedBusWriter {

	/**
	 * Opens the writer.
	 *
	 * @throws IOException if there was an error opening the file
	 */
	public void open() throws IOException;

	/**
	 * Writes a message.
	 *
	 * @param message the message object to write
	 */
	public void write(Message message) throws EOFException;

	/**
	 * Writes a buffer of data.
	 *
	 * @param src the output buffer
	 * @param offset the offset in the buffer of the first byte to write
	 * @param length the length of the data
	 */
	public void write(byte[] src, int offset, int length) throws EOFException;

	/**
	 * Closes the writer.
	 *
	 * @throws IOException if there was an error closing the file
	 */
	public void close() throws IOException;
}