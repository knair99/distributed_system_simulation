package queue;

import config.Configuration;
import networking.database.DatabaseHelper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class AsyncReplicationEventConsumer {
    // You can have multiple, comma separated bootstrap servers
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String CONSUMER_GROUP_PREFIX = "events-producer";
    public static final String TOPIC = "sync-events";

    public static Consumer<Long, String> createKafkaConsumer() {
        String currentServerAddress = "1.1.1.1";
        try {
            currentServerAddress = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(),
                    Configuration.getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String CONSUMER_GROUP = CONSUMER_GROUP_PREFIX + currentServerAddress;

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new KafkaConsumer<>(properties);
    }

    public static void consumeMessages(Consumer<Long, String> kafkaConsumer) {
        kafkaConsumer.subscribe(Collections.singletonList(TOPIC));

        while (true) {
            ConsumerRecords<Long, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));

            if (consumerRecords.isEmpty()) {
                // do something else
            }

            for (ConsumerRecord<Long, String> record : consumerRecords) {
                System.out.println(String.format("Received asynchronous replication event: (key: %d, value: %s," +
                                " partition: %d, offset: %d",
                        record.key(), record.value(), record.partition(), record.offset()));

                // Now write to the database replica
                DatabaseHelper.writeDataToDatabase(record.value());
            }

            // do something with the records
            kafkaConsumer.commitAsync();
        }
    }


}
