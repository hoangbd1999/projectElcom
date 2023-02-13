package com.elcom.metacen.dispatcher.process.config.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 *
 * @author Admin
 */
@Configuration
public class KafkaProducerConfig {
    
    @Value("${kafka.bootstrap.servers}")
    private String kafkaBootstrapServers;
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaBootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, VsatPartitioner.class.getName());
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 31457280); // default 1048576 (bytes)
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 300000); // default 30000 (30s)
        
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 1024); // default 16384 (bytes) if not set
                
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
