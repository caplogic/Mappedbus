# MappedBus
MappedBus is a Java based low latency, high throughput message bus, built on top of a memory mapped file, supporting multiple readers and writers.

It was inspired by [Java Chronicle](https://github.com/OpenHFT/Chronicle-Queue) with the main difference that it's designed to efficiently support multiple writers – enabling use cases where the ordering of events produced by multiple processes are important.

MappedBus can also be described as an efficient IPC mechanism which allows several Java programs to communicate by exchanging messages while leaving an audit trail of the messages that were sent.

<p align="center">
  <img src="http://3.bp.blogspot.com/-L51XiyruNMA/VU5K9dMtx9I/AAAAAAAAACg/AOkdwjTrzgI/s320/mappedbus.png">
</p>

The bus is intended to be used as the backbone for an [event driven architecture](http://www.reactivemanifesto.com) where multiple producers create events which should appear in the same sequence to all consumers. When a consumer is restarted the events can be replayed since they're persisted in the memory mapped file.

The throughput (on a laptop, i7-4558U @ 2.8 GHZ) between a single producer writing at full speed, and a single consumer is around 40 million records per second (a small message consisting of three integer fields), and the time for reading and writing is around 25 ns per record. MappedBus does not create any objects at all after startup and therefore has zero GC impact.

**Getting Started**

Download "mappedbuf.jar" from the release tab above (or clone the project and build it from source by running "ant") and try out any of the sample applications described below.

**Usage**

Setting up the MappedBus:
```java
// Setup a reader
MappedBusReader reader = new MappedBusReader("/tmp/test", 100000L);

// Setup a writer
MappedBusWriter writer = new MappedBusWriter();
writer.init("/tmp/test", 100000L, true);
```

The bus supports two modes of operation: byte array based (raw data), and message based (object oriented).

Byte Array based:
```java
// write a buffer
writer.write(buffer, 0, buffer.length);

// read a buffer
while (reader.hasNext()) {
   int length = reader.read(buffer, 0);
}
```

Message based:

```java
PriceUpdate priceUpdate = new PriceUpdate();

// write a message
writer.write(priceUpdate);

// read messages
while (reader.hasNext()) {
   int type = reader.readType();
   if (type == 0) {
      reader.readMessage(priceUpdate)
   }
}
```

**Sample Applications**

The project contains two very simple sample applications: one byte array based and one message based.

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

The byte array based sample application is run in the same way.

Another sample application simulates a token passed around between a number of nodes. Each node will send a message, Token, which contains two fields: to and from. When a node receives a token it will check whether it's the receiver and if so it will send a new token message with the to field set to it's id + 1.
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


**Performance Tests**

The project contains a performance test which can be run as follows.
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

**Implementation Notes**

This is how MappedBus guarantees that records can be written by multiple processes in the correct order.

The first eight bytes of the file make up a field called the limit. This field specifies how much data has actually been written to the file. The readers will poll the limit field (using volatile) to see whether there's a new record to be read.

When a writer wants to add a record to the file it will first read the limit field (using volatile) and then use CAS to increase the limit field (specifying the value of the last limit it could see). By using CAS the writer will know whether it has succeeded in updating the limit field or not (in other words, whether the limit it specified in the CAS operation was still valid). If the CAS operation fails it means another writer succeeded in updating the limit field first, and in this case the writer which failed will retry the operation.

When the limit field has increased a reader will know there's new data to be read, but the writer which updated the limit field might not yet have written any data in the record. To avoid this problem each record contains an initial 4 bytes which make up the commit field. When a writer has finished writing a record it will set the commit field (using volatile) and the reader will only start reading a record once it has seen that the commit field has been set.

**Questions**

For any questions or comments about the MappedBus feel free to drop a mail to: mappedbus@gmail.com
