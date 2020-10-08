package de.juplo.demo.kafka.deduplication;

import org.apache.kafka.common.header.Headers;


public interface SequenceNumberExtractor<K,V>
{
  /**
   * Extracts a sequence number from the given value.
   *
   * The sequence number must be represented as a {@link Long} value.
   *
   * @param topic The topic, the message was issued on
   * @param partition The partition, the message was written to
   * @param offset The offset of the message in the partition
   * @param key The key of the message
   * @param value The value of the message
   * @return a unique ID
   */
  public long extract(String topic, int partition, long offset, Headers headers, K key, V value);
}
