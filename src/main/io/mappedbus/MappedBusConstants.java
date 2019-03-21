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

/**
 * Class with constants.
 *
 */
public class MappedBusConstants {

	public static class Structure {
		
		public static final int Limit = 0;
		
		public static final int Data = Length.Limit;
		
	}

	public static class Length {
		
		public static final int Limit = 8;
		
		public static final int StatusFlag = 4;
		
		public static final int Metadata = 4;

		public static final int RecordHeader = StatusFlag + Metadata;

	}

	public static class StatusFlag {
		
		public static final byte NotSet = 0;
		
		public static final byte Commit = 1;

		public static final byte Rollback = 2;

	}
}