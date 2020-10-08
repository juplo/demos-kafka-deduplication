package de.juplo.demo.kafka.deduplication;

import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier;


public class DeduplicationTransformerSupplier<K, V> implements ValueTransformerWithKeySupplier<K, V, Iterable<V>>
{
  SequenceNumberExtractor<K, V> extractor;


  public DeduplicationTransformerSupplier(SequenceNumberExtractor<K, V> extractor)
  {
    this.extractor = extractor;
  }


  @Override
  public ValueTransformerWithKey<K, V, Iterable<V>> get()
  {
    return new DeduplicationTransformer<K, V>(extractor);
  }
}
