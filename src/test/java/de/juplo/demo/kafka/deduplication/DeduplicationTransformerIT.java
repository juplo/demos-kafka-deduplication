package de.juplo.demo.kafka.deduplication;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
public class DeduplicationTransformerIT
{
  @Test
  public void test()
  {
    DeduplicationTransformer transformer = new DeduplicationTransformer();
    MockProcessorContext context = new MockProcessorContext();
    KeyValueStore<Integer, Long> store =
        Stores
            .keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(DeduplicationTransformer.STORE),
                Serdes.Integer(),
                Serdes.Long())
            .withLoggingDisabled() // Changelog is not supported by MockProcessorContext.
            .build();
    store.init(context, store);
    context.register(store, null);
    transformer.init(context);
    context.setTopic("foo");
    context.setOffset(1);

    Iterator<String> transformed;

    context.setPartition(0);
    transformed = transformer.transform("1", "1").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("1");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(1l);

    context.setPartition(1);
    transformed = transformer.transform("2", "2").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("2");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(1l);
    assertThat(store.get(1)).isEqualTo(2l);

    context.setPartition(0);
    transformed = transformer.transform("1", "1").iterator();
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(1l);
    assertThat(store.get(1)).isEqualTo(2l);

    context.setPartition(0);
    transformed = transformer.transform("1", "4").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("4");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(4l);
    assertThat(store.get(1)).isEqualTo(2l);

    // The order is only guaranteed per partition!
    context.setPartition(2);
    transformed = transformer.transform("3", "3").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("3");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(4l);
    assertThat(store.get(1)).isEqualTo(2l);
    assertThat(store.get(2)).isEqualTo(3l);

    context.setPartition(1);
    transformed = transformer.transform("2", "2").iterator();
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(4l);
    assertThat(store.get(1)).isEqualTo(2l);
    assertThat(store.get(2)).isEqualTo(3l);

    context.setPartition(2);
    transformed = transformer.transform("3", "5").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("5");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(4l);
    assertThat(store.get(1)).isEqualTo(2l);
    assertThat(store.get(2)).isEqualTo(5l);

    // The order is only guaranteed per partition!
    context.setPartition(1);
    transformed = transformer.transform("2", "6").iterator();
    assertThat(transformed.hasNext()).isTrue();
    assertThat(transformed.next()).isEqualTo("6");
    assertThat(transformed.hasNext()).isFalse();
    assertThat(store.get(0)).isEqualTo(4l);
    assertThat(store.get(1)).isEqualTo(6l);
    assertThat(store.get(2)).isEqualTo(5l);
  }
}
