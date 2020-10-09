package de.juplo.demo.kafka.deduplication;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;


@Component
public class StreamsHealthIndicator extends AbstractHealthIndicator implements KafkaStreams.StateListener
{
  public final static Status CREATED = new Status("CREATED");
  public final static Status RUNNING = new Status("RUNNING");
  public final static Status REBALANCING = new Status("REBALANCING");
  public final static Status ERROR = new Status("ERROR");
  public final static Status PENDING_SHUTDOWN = new Status("PENDING_SHUTDOWN");
  public final static Status NOT_RUNNING = new Status("NOT_RUNNING");

  private Status status = Status.UNKNOWN;

  @Override
  protected synchronized void doHealthCheck(Health.Builder builder) throws Exception
  {
    builder.status(status);
  }

  @Override
  public synchronized void onChange(KafkaStreams.State newState, KafkaStreams.State oldState)
  {
    switch (newState)
    {
      case CREATED:
        status = CREATED;
        break;
      case RUNNING:
        status = RUNNING;
        break;
      case REBALANCING:
        status = REBALANCING;
        break;
      case ERROR:
        status = ERROR;
        break;
      case PENDING_SHUTDOWN:
        status = PENDING_SHUTDOWN;
        break;
      case NOT_RUNNING:
        status = NOT_RUNNING;
        break;
      default:
        status = null;
    }
  }
}
