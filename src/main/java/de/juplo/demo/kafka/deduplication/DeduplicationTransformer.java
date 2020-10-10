package de.juplo.demo.kafka.deduplication;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Arrays;
import java.util.Collections;


@Slf4j
public class DeduplicationTransformer implements ValueTransformer<String, Iterable<String>>
{
  public final static String STORE = DeduplicationTransformer.class.getCanonicalName() + "_STORE";
  private ProcessorContext context;
  private KeyValueStore<Integer, Long> store;


  @Override
  public void init(ProcessorContext context)
  {
    this.context = context;
    store = (KeyValueStore<Integer, Long>) context.getStateStore(STORE);
  }

  @Override
  public Iterable<String> transform(String value)
  {
    String topic = context.topic();
    Integer partition = context.partition();
    long offset = context.offset();
    Headers headers = context.headers();

    long sequenceNumber = Long.parseLong(value);

    Long seen = store.get(partition);
    if (seen == null || seen < sequenceNumber)
    {
      store.put(partition, sequenceNumber);
      return Arrays.asList(value);
    }

    log.info("ignoring message with sequence-number {} <= {}", sequenceNumber, seen);

    // Signal, that the message has already been seen.
    // Downstream has to filter the null-values...
    return Collections.emptyList();
  }

  @Override
  public void close() {}
}
