package queue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class AsyncReplicationEventProducer {

    // You can have multiple, comma separated bootstrap servers
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TOPIC = "sync-events";

    public Producer<Long, String> createKafkaProducer(String bootstrapServers) {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "events-producer");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(properties);
    }

    public void publishMessages(int numberOfMessages, Producer<Long, String> kafkaProducer, byte[] requestBytes) throws ExecutionException, InterruptedException {
        // Since we only have one partition now with a replication factor of 1
        int partition = 0;

        int i = 0;
        while (i < numberOfMessages) {
            long key = i;
            String value = String.format(new String(requestBytes));
            long timeStamp = System.currentTimeMillis();

            // We can overload the ProducerRecord to remove the partition, and it will automatically hash range by key
            // (depending on number of partitions in the kafka server) - You can also remove the key and kafka will
            // round robin the value to different partitions
            // Blocking debug call: RecordMetadata recordMetadata = kafkaProducer.send(record).get();
            ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC, partition, timeStamp, key, value);
            kafkaProducer.send(record);

            System.out.println(String.format("Async record with (key: %s, value: %s), was sent to (partition: %d, " +
                    "offset: %d", record.key(), record.value(), 123, 123));
            //recordMetadata.partition(), recordMetadata.offset()));
            i++;
        }
    }


}
