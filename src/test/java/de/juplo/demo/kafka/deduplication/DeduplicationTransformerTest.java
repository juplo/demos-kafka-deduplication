package de.juplo.demo.kafka.deduplication;

import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
public class DeduplicationTransformerTest
{
  @MockBean
  ProcessorContext context;
  @MockBean
  KeyValueStore<Integer, Long> store;

  DeduplicationTransformer transformer = new DeduplicationTransformer();


  @BeforeEach
  public void setUpTransformer()
  {
    when(context.getStateStore(DeduplicationTransformer.STORE)).thenReturn(store);
    transformer.init(context);
  }


  @Test
  public void testStoresSequenceNumberAndForwardesValueIfStoreIsEmpty()
  {
    when(store.get(anyInt())).thenReturn(null);
    when(context.partition()).thenReturn(0);

    Iterator<String> result = transformer.transform("1").iterator();

    assertThat(result.hasNext()).isTrue();
    assertThat(result.next()).isEqualTo("1");
    assertThat(result.hasNext()).isFalse();
    verify(store, atLeastOnce()).put(eq(0), eq(1l));
  }

  @ParameterizedTest
  @ValueSource(longs = { 2, 3, 4, 5, 6, 7 })
  public void testStoresSequenceNumberAndForwardesValueIfSequenceNumberIsGreater(long sequenceNumber)
  {
    String value = Long.toString(sequenceNumber);

    when(store.get(anyInt())).thenReturn(1l);
    when(context.partition()).thenReturn(0);

    Iterator<String> result = transformer.transform(value).iterator();

    assertThat(result.hasNext()).isTrue();
    assertThat(result.next()).isEqualTo(value);
    assertThat(result.hasNext()).isFalse();
    verify(store, atLeastOnce()).put(eq(0), eq(sequenceNumber));
  }

  @ParameterizedTest
  @ValueSource(longs = { 1, 2, 3, 4, 5, 6, 7 })
  public void testDropsValueIfSequenceNumberIsNotGreater(long sequenceNumber)
  {
    String value = Long.toString(sequenceNumber);

    when(store.get(anyInt())).thenReturn(7l);
    when(context.partition()).thenReturn(0);

    Iterator<String> result = transformer.transform(value).iterator();

    assertThat(result.hasNext()).isFalse();
    verify(store, never()).put(eq(0), eq(sequenceNumber));
  }
}
