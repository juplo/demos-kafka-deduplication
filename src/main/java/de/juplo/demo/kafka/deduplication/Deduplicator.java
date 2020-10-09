package de.juplo.demo.kafka.deduplication;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;


@Component
public class Deduplicator
{
  final static Logger LOG = LoggerFactory.getLogger(Deduplicator.class);

  public final KafkaStreams streams;


  public Deduplicator()
  {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "kafka:9092");
    properties.put("application.id", "streams-deduplicator");
    properties.put("default.key.serde", Serdes.StringSerde.class);
    properties.put("default.value.serde", Serdes.StringSerde.class);

    streams = new KafkaStreams(Deduplicator.buildTopology(), properties);
    streams.setUncaughtExceptionHandler((Thread t, Throwable e) ->
    {
      LOG.error("Unexpected error in thread {}: {}", t, e.toString());
      try
      {
        streams.close(Duration.ofSeconds(5));
      }
      catch (Exception ex)
      {
        LOG.error("Could not close KafkaStreams!", ex);
      }
    });
  }

  static Topology buildTopology()
  {
    StreamsBuilder builder = new StreamsBuilder();

    // Create state-store for sequence numbers
    StoreBuilder<KeyValueStore<Integer,Long>> store =
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(DeduplicationTransformer.STORE),
            Serdes.Integer(),
            Serdes.Long());
    // register store
    builder.addStateStore(store);

    builder
        .<String, String>stream("input")
        .flatTransformValues(
            new ValueTransformerWithKeySupplier<String, String, Iterable<String>>()
            {
              @Override
              public ValueTransformerWithKey<String, String, Iterable<String>> get()
              {
                return new DeduplicationTransformer();
              }
            },
            DeduplicationTransformer.STORE)
        .to("output");

    return builder.build();
  }

  @PostConstruct
  public void start()
  {
    streams.start();
  }

  @PreDestroy
  public void stop()
  {
    streams.close(Duration.ofSeconds(5));
  }
}
