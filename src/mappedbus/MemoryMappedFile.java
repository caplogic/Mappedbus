package mappedbus;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
 
import sun.nio.ch.FileChannelImpl;
import sun.misc.Unsafe;
 
@SuppressWarnings("restriction")
public class MemoryMappedFile {
 
	private static final Unsafe unsafe;
	private static final Method mmap;
	private static final Method unmmap;
	private static final int BYTE_ARRAY_OFFSET;
 
	private long addr, size;
	private final String loc;
 
	static {
		try {
			Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			unsafe = (Unsafe) singleoneInstanceField.get(null);
			mmap = getMethod(FileChannelImpl.class, "map0", int.class, long.class, long.class);
			unmmap = getMethod(FileChannelImpl.class, "unmap0", long.class, long.class);
			BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	private static Method getMethod(Class<?> cls, String name, Class<?>... params) throws Exception {
		Method m = cls.getDeclaredMethod(name, params);
		m.setAccessible(true);
		return m;
	}
 
	private static long roundTo4096(long i) {
		return (i + 0xfffL) & ~0xfffL;
	}
 
	private void mapAndSetOffset() throws Exception{
		final RandomAccessFile backingFile = new RandomAccessFile(this.loc, "rw");
		backingFile.setLength(this.size);
		final FileChannel ch = backingFile.getChannel();
		this.addr = (long) mmap.invoke(ch, 1, 0L, this.size);
		ch.close();
		backingFile.close();
	}
 
	public MemoryMappedFile(final String loc, long len) throws Exception {
		this.loc = loc;
		this.size = roundTo4096(len);
		mapAndSetOffset();
	}
 
	public int getInt(long pos){
		return unsafe.getInt(pos + addr);
	}

	public int getIntVolatile(long pos){
		return unsafe.getIntVolatile(null, pos + addr);
	}
	
	public long getLong(long pos){
		return unsafe.getLong(pos + addr);
	}
	
	public long getLongVolatile(long pos){
		return unsafe.getLongVolatile(null, pos + addr);
	}
 
	public void putInt(long pos, int val){
		unsafe.putInt(pos + addr, val);
	}
	
	public void putIntVolatile(long pos, int val){
		unsafe.putIntVolatile(null, pos + addr, val);
	}
 
	public void putLong(long pos, long val){
		unsafe.putLong(pos + addr, val);
	}
	
	public void putLongVolatile(long pos, long val){
		unsafe.putLongVolatile(null, pos + addr, val);
	}
	
	public boolean compareAndSwapInt(long pos, int expected, int value) {
		return unsafe.compareAndSwapInt(null, pos + addr, expected, value);
	}
		
	public boolean compareAndSwapLong(long pos, long expected, long value) {
		return unsafe.compareAndSwapLong(null, pos + addr, expected, value);
	}
 
	public void getBytes(long pos, byte[] data, int offset, int length){
		unsafe.copyMemory(null, pos + addr, data, BYTE_ARRAY_OFFSET + offset, length);
	}
 
	public void setBytes(long pos, byte[] data, int offset, int length){
		unsafe.copyMemory(data, BYTE_ARRAY_OFFSET + offset, null, pos + addr, length);
	}
}