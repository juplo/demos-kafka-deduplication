package de.juplo.demo.kafka.deduplication;


import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.kstream.ValueTransformerSupplier;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;


@RestController
public class Deduplicator
{
  final static Logger LOG = LoggerFactory.getLogger(Deduplicator.class);

  public final KafkaStreams streams;
  public final String host;
  public final int port;

  public Deduplicator(
      ServerProperties serverProperties,
      StreamsHealthIndicator healthIndicator)
  {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "kafka:9092");
    properties.put("application.id", "streams-deduplicator");
    properties.put("default.key.serde", Serdes.StringSerde.class);
    properties.put("default.value.serde", Serdes.StringSerde.class);

    this.host = serverProperties.getAddress().getHostAddress();
    this.port = serverProperties.getPort();
    properties.put("application.server", host + ":" + port);

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
    streams.setStateListener(healthIndicator);
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
            new ValueTransformerSupplier<String, Iterable<String>>()
            {
              @Override
              public ValueTransformer<String, Iterable<String>> get()
              {
                return new DeduplicationTransformer();
              }
            },
            DeduplicationTransformer.STORE)
        .to("output");

    return builder.build();
  }

  @GetMapping(path = "/watermark/{partition}", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getWatermarks(@PathVariable Integer partition)
  {
    KeyQueryMetadata metadata =
        streams.queryMetadataForKey(
            DeduplicationTransformer.STORE,
            partition, new IntegerSerializer());

    if (metadata.getActiveHost().port() != port ||
        !metadata.getActiveHost().host().equals(host))
    {
      return
          ResponseEntity
              .status(HttpStatus.TEMPORARY_REDIRECT)
              .header(
                  HttpHeaders.LOCATION,
                  "http://" +
                      metadata.getActiveHost().host() +
                      ":" +
                      metadata.getActiveHost().port() +
                      "/watermark/" +
                      partition)
              .build();
    }

    ReadOnlyKeyValueStore<Integer, Long> store = streams.store(
        StoreQueryParameters.fromNameAndType(
            DeduplicationTransformer.STORE,
            QueryableStoreTypes.keyValueStore()));

    Long watermark = store.get(partition);
    if (watermark == null)
      return ResponseEntity.notFound().build();

    return ResponseEntity.ok().body(watermark.toString());
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
