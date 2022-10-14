## Mappedbus is a Java based high throughput, low latency message bus, using a memory mapped file or shared memory as transport

Mappedbus was inspired by [Java Chronicle](https://github.com/OpenHFT/Chronicle-Queue) with the main difference that it's designed to efficiently support multiple writers – enabling use cases where the order of messages produced by multiple processes are important.

The throughput (on a laptop, i7-4558U @ 2.8 GHz) between a single producer writing at full speed and a single consumer is around 14 million messages per second (a small message consisting of three integer fields), and the average read/write latency is around 70 ns per message.

Mappedbus is a lock-free data structure.

Mappedbus does not create any objects after startup and therefore has Zero GC impact.

#### Features:
* IPC between multiple processes by message passing.
* Support for a memory mapped file, or shared memory as transport.
* Support for object or byte array (raw data) based messages.

### Getting Started

Download mappedbus.jar from the release tab (or clone the project and build it from source by running "ant") and try out any of the samples described below.

### Usage

Setup a reader and a writer:
```java
// Setup a reader
MappedBusReader reader = new MappedBusReader("/tmp/test", 100000L, 32);
reader.open();

// Setup a writer
MappedBusWriter writer = new MappedBusWriter("/tmp/test", 100000L, 32);
writer.open();
```

In the code above the file "/tmp/test" is on disk and thus is memory mapped by the library. To use the library with shared memory, instead point to a file in "/dev/shm", for example, "/dev/shm/test".

When using a memory mapped file the messages will be lazily persisted to disk. With shared memory the messages will be stored in the RAM.
<br><br>
Read/write messages using objects:
```java
PriceUpdate priceUpdate = new PriceUpdate();

// write a message
writer.write(priceUpdate);

// read messages
while (true) {
   if (reader.next()) {
      int type = reader.readType();
      if (type == 0) {
         reader.readMessage(priceUpdate)
      }
   }
}
```

Read/write messages using byte arrays:
```java
byte[] buffer = new byte[32];

// write a buffer
writer.write(buffer, 0, buffer.length);

// read buffers
while (true) {
   if (reader.next()) {
      int length = reader.readBuffer(buffer, 0);
   }
}
```

### Examples

The project contains examples of an object based and a byte array based reader/writer.

The object based one works as follows. The ObjectWriter class will send a message, PriceUpdate, which contains three fields: source, price and quantity. The first argument of the ObjectWriter is the source. The ObjectReader simply prints every message it receives.

```
> java -cp mappedbus.jar io.mappedbus.sample.object.ObjectWriter 0
...
```
```
> java -cp mappedbus.jar io.mappedbus.sample.object.ObjectWriter 1
...
```
```
> java -cp mappedbus.jar io.mappedbus.sample.object.ObjectReader
...
Read: PriceUpdate [source=0, price=20, quantity=40]
Read: PriceUpdate [source=1, price=8, quantity=16]
Read: PriceUpdate [source=0, price=22, quantity=44]
```

The byte array based example is run in the same way.

Another example simulates a token being passed around between a number of nodes. Each node will send a message, Token, which contains two fields: to and from. When a node receives a token it will check whether it's the receiver and if so it will send a new token message with the "to" field set to it's id + 1 mod "number of nodes".
```
> java -cp mappedbus.jar io.mappedbus.sample.token.Node 0 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```
```
> java -cp mappedbus.jar io.mappedbus.sample.token.Node 1 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```
```
> java -cp mappedbus.jar io.mappedbus.sample.token.Node 2 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```


### Performance

The project contains a performance test which can be run as follows:
```
> rm -rf /tmp/test;java -cp mappedbus.jar io.mappedbus.perf.MessageReader /tmp/test
...
Elapsed: 5660 ms
Per op: 70 ns
Op/s: 14131938
```
```
> java -cp mappedbus.jar io.mappedbus.perf.MessageWriter /tmp/test
...
```

### Implementation

This is how Mappedbus solves the synchronization problem between multiple writers (each running in it's own process/JVM):

* The first eight bytes of the file make up a field called the limit. This field specifies how much data has been written to the file. The readers will poll the limit field (using volatile) to see whether there's a new record to be read.

* When a writer adds a record to the file it will use the fetch-and-add instruction to atomically update the limit field.

* When the limit field has increased a reader will know there's new data to be read, but the writer which updated the limit field might not yet have written any data in the record. To avoid this problem each record contains an initial four bytes which make up the status flag field. The status flag field has three possible values: not set, committed, rolled back.

* When a writer has finished writing a record it will set the status field to value indicating the record has been committed (using compare and swap) and the reader will only start reading a record once it has seen that the commit field has been set.

* A writer might crash after it has updated the limit field but before it has updated the status flag field indicating the record has been committed. To avoid this problem the reader has a timeout for how long it will wait for the commit field to be set. When that time is reached the reader will set the status flag field (using compare and swap) to a value indicating the record has been rolled back, and continue with the next record. When the status flag field is set to indicate it's been rolled back the record is always ignored by the readers.

* A slow writer may write a message and be about to set the status flag to indicate the record has been committed, while a reader has already timed out and set the status flag to indicate the record has been rolled back. Since the status flag is updated using compare-and-swap the writer will detect this, and the write() method call will return false to indicate the write was not successful.

The solution seems to work well on Linux x86 with Oracle's JVM (1.8) but it probably won't work on all platforms. The project contains a test (called IntegrityTest) to check whether it works on the platform used.

### Questions

For questions or suggestions drop an email to info@mappedbus.io
