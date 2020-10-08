  package de.juplo.demo.kafka.deduplication;

  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;


  @SpringBootApplication
  public class StreamsDeduplicatorApplication
  {
    public static void main(String[] args)
    {
      SpringApplication.run(StreamsDeduplicatorApplication.class, args);
    }
  }
