package io.mappedbus;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class RollbackTest {

    public static final String FILE_NAME = "/tmp/rollback-tester";

    public static final int RECORD_SIZE = 8 * 8;

    public static final int RECORDS = 20;

    public static final long FILE_SIZE = (RECORD_SIZE + MappedBusConstants.Length.RecordHeader) * RECORDS + MappedBusConstants.Length.Limit;

    public static final long INITIAL_VALUE = 1111111111111111111L;

    @Test
    public void test() {
        new File(FILE_NAME).delete();

        Writer writer = new Writer();
        Reader reader = new Reader();

        writer.start();
        reader.start();

        try {
            writer.join();
            reader.join();
        } catch (InterruptedException e) {
        }
    }

    class Writer extends Thread {
        public void run() {
            try {
                MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
                reader.open();

                MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
                writer.open();

                Message message = new Message();

                for (long i = 0; i < RECORDS; i++) {
                    message.setValue(INITIAL_VALUE + i);
                    if(i == 5 || i == 15) {
                        long commitPos = writer.writeRecord(message);
                        Thread.sleep(1000);
                        boolean result = writer.commit(commitPos);
                        assertEquals(false, result);
                    } else {
                        writer.write(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class Reader extends Thread {
        public void run() {
            try {
                MappedBusReader reader = new MappedBusReader(FILE_NAME, FILE_SIZE, RECORD_SIZE);
                reader.setTimeout(500);
                reader.open();

                MappedBusWriter writer = new MappedBusWriter(FILE_NAME, FILE_SIZE, RECORD_SIZE);
                writer.open();

                Message message = new Message();

                for (int i = 0; i < RECORDS - 1; i++) {
                    if(i == 5 || i == 15) {
                        continue;
                    }
                    message.setValue(INITIAL_VALUE + i);
                    while(true) {
                        if (reader.next()) {
                            reader.readMessage(message);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class Message implements MappedBusMessage {

        long value;

        public void setValue(long value) {
            this.value = value;
        }

        @Override
        public void write(MemoryMappedFile mem, long pos) {
            for(int i = 0; i < 8; i++) {
                mem.putLong(pos + i * 8, value);
            }
        }

        @Override
        public void read(MemoryMappedFile mem, long pos) {
            for (int i = 0; i < 8; i++) {
                long valueRead;
                if ((valueRead = mem.getLong(pos + i * 8)) != value) {
                   throw new RuntimeException("Excepted: " + value + " but read " + valueRead);
                }
            }
        }

        @Override
        public int type() {
            return 0;
        }
    }
}
