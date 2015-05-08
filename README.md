# MappedBus
MappedBus is a low latency, high throughput message bus, built on top of a memory mapped file, supporting multiple readers and writers.

It is inspired by Java Chronicle  with the key difference that it's designed to efficiently support multiple writers – enabling use cases where the ordering of events produced by multiple processes are important.

The bus is intended to be used as the backbone for an [event driven architecture](http://www.reactivemanifesto.com) where multiple producers create events which should appear in the same sequence to all consumers. When a consumer is restarted the events can be replayed since they're persisted in the memory mapped file.

The throughput (on a laptop, i7-4558U @ 2.8 GHZ) between a single producer writing at full speed, and a single consumer is around 50 million events per second (a small message consisting of three ints) and the time for writing and reading is around 20 ns.

**Usage**

Setting up the MappedBus:
```java
// Setup a reader
MappedBusReader reader = new MappedBusReader("/tmp/test", 100000L);

// Set up a writer
MappedBusWriter writer = new MappedBusWriter();
writer.init("/tmp/test", 100000L, true);
```

The bus supports two modes of operation: byte array based (raw data), and message based (object oriented).

Byte Array based:
```java
// write a buffer
writer.write(buffer, offset, length);

// read a buffer
while (reader.hasNext()) {
   int length = reader.read(buffer)
}
```

Message based:

Each message object need to implement an interface with a size, read and write method.

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

**Samples**

The project contains two samples: one byte array based and one message based.

The message based sample can be run as follows:

In the first terminal:
```
> java sample.messagebased.MessageWriter 0
...
```

In a second terminal:
```
> java sample.messagebased.MessageWriter 0
...
```

In a third terminal:
```
> java sample.messagebased.MessageReader
...
Read: PriceUpdate [source=1, price=6, quantity=12]
Read: PriceUpdate [source=0, price=14, quantity=28]
Read: PriceUpdate [source=1, price=8, quantity=16]
```

The byte array based sample is run in the same way.

**How is the MappedBus implemented?**

Here's how MappedBus guarantees that records can be written by multiple processes in order to the file.

The first eight bytes of the file make up a field called the limit. This field specifies how much data has actually been written to the file. The readers will poll the limit field (using volatile) to see whether there's a new record to be read.

When a writer wants to add a record to the file it will first read the limit field (using volatile) and then use CAS to increase the limit field (specifying the value of the last limit it could see). By using CAS the writer will know whether it has succeeded in updating the limit field or not (in other words, whether the limit it specified in the CAS operation was still valid). If the CAS operation fails it means another writer succeeded in updating the limit field first, and in this case the writer which failed will retry the operation.

When the limit field has increased a reader will know there's new data to be read, but the writer which updated the limit field might not yet have written any data in the record. To avoid this problem each record contains an initial 4 bytes which make up the commit field. When a writer has finished writing a record it will set the commit field (using volatile) and the reader will only start reading a record once it has seen that the commit field has been set.

**Questions**

For any questions or comments about the MappedBus feel free to drop a mail to: mappedbuf@gmail.com
