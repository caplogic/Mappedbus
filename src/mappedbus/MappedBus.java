package mappedbus;

public class MappedBus {

	public static class Layout {
		
		public static final int Limit = 0;
		
		public static final int Data = 8;
		
	}
	
	public static class Length {
		
		public static final int Commit = 4;

		public static final int Metadata = 4;
		
	}
	
	public static class Commit {
		
		public static final int Set = 1;
		
	}
}
