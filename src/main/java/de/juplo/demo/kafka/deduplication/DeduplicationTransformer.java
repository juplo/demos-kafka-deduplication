package de.juplo.demo.kafka.deduplication;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Arrays;
import java.util.Collections;


@Slf4j
public class DeduplicationTransformer<K, V> implements ValueTransformerWithKey<K, V, Iterable<V>>
{
  final SequenceNumberExtractor<K, V> extractor;

  public final static String STORE = DeduplicationTransformer.class.getCanonicalName() + "_STORE";
  private ProcessorContext context;
  private KeyValueStore<Integer, Long> store;


  public DeduplicationTransformer(SequenceNumberExtractor<K, V> extractor)
  {
    this.extractor = extractor;
  }


  @Override
  public void init(ProcessorContext context)
  {
    this.context = context;
    store = (KeyValueStore<Integer, Long>) context.getStateStore(STORE);
  }

  @Override
  public Iterable<V> transform(K key, V value)
  {
    String topic = context.topic();
    Integer partition = context.partition();
    long offset = context.offset();
    Headers headers = context.headers();

    long sequenceNumber = extractor.extract(topic, partition, offset, headers, key, value);

    Long seen = store.get(partition);
    if (seen == null || seen < sequenceNumber)
    {
      store.put(partition, sequenceNumber);
      return Arrays.asList(value);
    }

    log.info(
        "ignoring message for key {} with sequence-number {} <= {} found at offset {} of partition {}",
        key,
        sequenceNumber,
        seen,
        offset,
        partition);

    // Signal, that the message has already been seen.
    // Downstream has to filter the null-values...
    return Collections.emptyList();
  }

  @Override
  public void close() {}
}
