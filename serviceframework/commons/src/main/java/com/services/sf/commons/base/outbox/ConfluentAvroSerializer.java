package com.services.sf.commons.base.outbox;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.outbox.enabled", stringValue = "true")
@IfBuildProperty(name = "framework.outbox.serializer", stringValue = "confluentAvroSerializer")
public class ConfluentAvroSerializer implements OutboxSerializer {


    final KafkaAvroSerializer serializer;


    public ConfluentAvroSerializer(@ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG) List<String> SCHEMA_REGISTRY_URL_CONFIG,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.MAX_SCHEMAS_PER_SUBJECT_CONFIG, defaultValue = "1000") Integer MAX_SCHEMAS_PER_SUBJECT_CONFIG,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG, defaultValue = "true") boolean AVRO_REFLECTION_ALLOW_NULL_CONFIG,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, defaultValue = "true") boolean AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG, defaultValue = "true") boolean AVRO_REMOVE_JAVA_PROPS_CONFIG,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.NORMALIZE_SCHEMAS, defaultValue = "true") boolean NORMALIZE_SCHEMAS,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, defaultValue = "true") boolean AUTO_REGISTER_SCHEMAS,
                                   @ConfigProperty(name = "framework.outbox." + KafkaAvroSerializerConfig.SCHEMA_REFLECTION_CONFIG, defaultValue = "true") boolean SCHEMA_REFLECTION_CONFIG
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL_CONFIG);
        properties.put(KafkaAvroSerializerConfig.MAX_SCHEMAS_PER_SUBJECT_CONFIG, MAX_SCHEMAS_PER_SUBJECT_CONFIG);
        properties.put(KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG, AVRO_REFLECTION_ALLOW_NULL_CONFIG);
        properties.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG);
        properties.put(KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG, AVRO_REMOVE_JAVA_PROPS_CONFIG);
        properties.put(KafkaAvroSerializerConfig.NORMALIZE_SCHEMAS, NORMALIZE_SCHEMAS);
        properties.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, AUTO_REGISTER_SCHEMAS);
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REFLECTION_CONFIG, SCHEMA_REFLECTION_CONFIG);
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(SCHEMA_REGISTRY_URL_CONFIG, MAX_SCHEMAS_PER_SUBJECT_CONFIG);

        this.serializer = new KafkaAvroSerializer(schemaRegistryClient, properties);
    }

    public byte[] serialize(String topic, Object record) {
        return this.serializer.serialize(topic, record);
    }

    public byte[] serialize(String topic, Headers headers, Object record) {
        return this.serializer.serialize(topic, headers, record);
    }
}
