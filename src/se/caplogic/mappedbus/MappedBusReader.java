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

public class MappedBusReader {

	private final MemoryMappedFile mem;

	private final long size;
	
	private long limit = Layout.Data;
	
	public MappedBusReader(String file, long size) {
		this.size = size;
		try {
			mem = new MemoryMappedFile(file, size);			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasNext() {
		if(limit >= size) {
			throw new RuntimeException("End of file was reached");
		}
		long newLimit = mem.getLongVolatile(Layout.Limit);
		return newLimit > limit;
	}
	
	private void next() {
		while (true) {
			if (mem.getIntVolatile(limit) == Commit.Set) {
				break;
			}
		}
		limit += Length.Commit;	
	}

	public int readType() {
		next();
		int type = mem.getInt(limit);
		limit += Length.Metadata;
		return type;
	}
	
	public int readBuffer(byte[] dst, int offset) {
		next();
		int length = mem.getInt(limit);
		limit += Length.Metadata;
		mem.getBytes(limit, dst, offset, length);
		limit += length;
		return length;
	}
	
	public void readMessage(Message message) {
		message.read(mem, limit);
		limit += message.size();
	}
}
