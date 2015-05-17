## MappedBus is a Java based low latency, high throughput message bus, built on top of a memory mapped file

MappedBus was inspired by [Java Chronicle](https://github.com/OpenHFT/Chronicle-Queue) with the main difference that it's designed to efficiently support multiple writers – enabling use cases where the order of messages produced by multiple processes are important.

MappedBus can be also described as an efficient IPC mechanism which enable several Java programs to communicate by exchanging messages.

<p align="center">
  <img src="http://3.bp.blogspot.com/-L51XiyruNMA/VU5K9dMtx9I/AAAAAAAAACg/AOkdwjTrzgI/s320/mappedbus.png">
</p>

The bus is intended to be used as the backbone for a [message driven architecture](http://www.reactivemanifesto.com) where multiple producers create messages which should appear in the same order to all consumers.

A powerful feature of MappedBus is that even though the messages are ordered there's no single component (and thus no single point of failure) taking care of the ordering, instead the processes independently determine who has access to the bus.

The throughput (on a laptop, i7-4558U @ 2.8 GHZ) between a single producer writing at full speed and a single consumer is around 40 million messages per second (a small message consisting of three integer fields), and the average read/write latency is around 25 ns per message. MappedBus does not create any objects after startup and therefore has no GC impact.

#### Features:
* IPC between multiple processes by exchanging messages
* All messages are persisted
* No single point of failure
* Support for either object or byte array based messages

### Getting Started

Download mappedbus.jar from the release tab (or clone the project and build it from source by running "ant") and try out any of the sample applications described below.

### Usage

Setting up the MappedBus:
```java
// Setup a reader
MappedBusReader reader = new MappedBusReader("/tmp/test", 100000L, 32);

// Setup a writer
MappedBusWriter writer = new MappedBusWriter("/tmp/test", 100000L, 32, true);
```

Object based read/write: 

```java
PriceUpdate priceUpdate = new PriceUpdate();

// write a message
writer.write(priceUpdate);

// read messages
while (true) {
   if (reader.hasNext()) {
      if (!reader.next()) {
         continue; // message was rolled back, skip it
      }
      int type = reader.readType();
      if (type == 0) {
         reader.readMessage(priceUpdate)
      }
   }
}
```

Byte array based read/write:

```java
// write a buffer
writer.write(buffer, 0, buffer.length);

// read buffers
while (true) {
   if (reader.hasNext()) {
      if (!reader.next()) {
         continue; // message was rolled back, skip it
      }
      int length = reader.read(buffer, 0);
   }
}
```

### Examples

The project contains examples for how to create a byte array and message based reader/writer.

The message based one work as follows. The MessageWriter will send a message, PriceUpdate, which contains three fields: source, price and quantity. The first argument of the MessageWriter is used to populate the source. The MessageReader simply prints every message it receives.

```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.message.MessageWriter 0
...
```
```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.message.MessageWriter 1
...
```
```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.message.MessageReader
...
Read: PriceUpdate [source=0, price=20, quantity=40]
Read: PriceUpdate [source=1, price=8, quantity=16]
Read: PriceUpdate [source=0, price=22, quantity=44]
```

The byte array based example is run in the same way.

Another example simulates a token passed around between a number of nodes. Each node will send a message, Token, which contains two fields: to and from. When a node receives a token it will check whether it's the receiver and if so it will send a new token message with the "to" field set to it's id + 1 mod "number of nodes".
```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.token.Token 0 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```
```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.token.Token 1 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```
```
> java -cp mappedbus.jar se.caplogic.mappedbus.sample.token.Token 2 3
Read: Token [from=0, to=1]
Read: Token [from=1, to=2]
...
```


### Performance

The project contains a performance test which can be run as follows:
```
> java -cp mappedbus.jar se.caplogic.mappedbus.perf.MessageWriter /home/youraccount/tmp/test
...
```
```
> java -cp mappedbus.jar se.caplogic.mappedbus.perf.MessageReader /home/youraccount/tmp/test
Elapsed: 1801 ms
Per op: 22 ns
Op/s: 44404868
```

### Implementation

Here's how MappedBus solves the synchronization problem between multiple writers (each running in it's own process/JVM):

* The first eight bytes of the file make up a field called the limit. This field specifies how much data has actually been written to the file. The readers will poll the limit field (using volatile) to see whether there's a new record to be read.

* When a writer wants to add a record to the file it will first read the limit field (using volatile) and then use CAS to increase the limit field (specifying the value of the last limit it could see). By using CAS the writer will know whether it has succeeded in updating the limit field or not (in other words, whether the limit it specified in the CAS operation was still valid). If the CAS operation fails it means another writer succeeded in updating the limit field first, and in this case the writer which failed will retry the operation.

* When the limit field has increased a reader will know there's new data to be read, but the writer which updated the limit field might not yet have written any data in the record. To avoid this problem each record contains an initial 4 bytes which make up the commit field.

* When a writer has finished writing a record it will set the commit field (using volatile) and the reader will only start reading a record once it has seen that the commit field has been set.

* A writer might crash after it has updated the limit field but before it has updated the commit field. To avoid this problem there's a field next to the commit field called the rollback field. The reader has a timeout for how long it will wait for the commit field to be set. When that time is reached the reader will set the rollback field (using volatile) and continue with the next record. The rollback field has precedence over the commit field, when the rollback field is set the record is always ignored by the readers.

The solution seems to work well on Linux x86 with Oracle's JVM (1.8) but it probably won't work on all platforms. The project contains a test (called ScrambleTest) to check whether it works on the platform used.

### Questions

For any questions or comments about the MappedBus feel free to drop a mail to: mappedbus@gmail.com
