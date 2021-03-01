package com.provectus.kafka.ui.cluster.service;

import com.provectus.kafka.ui.cluster.exception.DuplicateEntityException;
import com.provectus.kafka.ui.cluster.exception.NotFoundException;
import com.provectus.kafka.ui.cluster.exception.UnprocessableEntityException;
import com.provectus.kafka.ui.cluster.mapper.ClusterMapper;
import com.provectus.kafka.ui.cluster.model.ClustersStorage;
import com.provectus.kafka.ui.cluster.model.KafkaCluster;
import com.provectus.kafka.ui.cluster.model.schemaregistry.InternalCompatibilityCheck;
import com.provectus.kafka.ui.cluster.model.schemaregistry.InternalCompatibilityLevel;
import com.provectus.kafka.ui.cluster.model.schemaregistry.InternalNewSchema;
import com.provectus.kafka.ui.cluster.model.schemaregistry.SubjectIdResponse;
import com.provectus.kafka.ui.model.CompatibilityCheckResponse;
import com.provectus.kafka.ui.model.CompatibilityLevel;
import com.provectus.kafka.ui.model.NewSchemaSubject;
import com.provectus.kafka.ui.model.SchemaSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Formatter;
import java.util.Objects;
import java.util.function.Function;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
@Log4j2
@RequiredArgsConstructor
public class SchemaRegistryService {
    private static final String URL_SUBJECTS = "/subjects";
    private static final String URL_SUBJECT = "/subjects/{schemaName}";
    private static final String URL_SUBJECT_VERSIONS = "/subjects/{schemaName}/versions";
    private static final String URL_SUBJECT_BY_VERSION = "/subjects/{schemaName}/versions/{version}";
    private static final String LATEST = "latest";

    private final ClustersStorage clustersStorage;
    private final ClusterMapper mapper;
    private final WebClient webClient;

    public Flux<SchemaSubject> getAllLatestVersionSchemas(String clusterName) {
        var allSubjectNames = getAllSubjectNames(clusterName);
        return allSubjectNames
                .flatMapMany(Flux::fromArray)
                .flatMap(subject -> getLatestSchemaSubject(clusterName, subject));
    }

    public Mono<String[]> getAllSubjectNames(String clusterName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECTS)
                        .retrieve()
                        .bodyToMono(String[].class)
                        .doOnError(log::error)
                )
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Flux<SchemaSubject> getAllVersionsBySubject(String clusterName, String subject) {
        Flux<Integer> versions = getSubjectVersions(clusterName, subject);
        return versions.flatMap(version -> getSchemaSubjectByVersion(clusterName, subject, version));
    }

    private Flux<Integer> getSubjectVersions(String clusterName, String schemaName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_VERSIONS, schemaName)
                        .retrieve()
                        .onStatus(NOT_FOUND::equals,
                                throwIfNotFoundStatus(formatted("No such schema %s"))
                        ).bodyToFlux(Integer.class)
                ).orElse(Flux.error(new NotFoundException("No such cluster")));
    }

    public Mono<SchemaSubject> getSchemaSubjectByVersion(String clusterName, String schemaName, Integer version) {
        return this.getSchemaSubject(clusterName, schemaName, String.valueOf(version));
    }

    public Mono<SchemaSubject> getLatestSchemaSubject(String clusterName, String schemaName) {
        return this.getSchemaSubject(clusterName, schemaName, LATEST);
    }

    private Mono<SchemaSubject> getSchemaSubject(String clusterName, String schemaName, String version) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_BY_VERSION, schemaName, version)
                        .retrieve()
                        .onStatus(NOT_FOUND::equals,
                                throwIfNotFoundStatus(formatted("No such schema %s with version %s", schemaName, version))
                        ).bodyToMono(SchemaSubject.class)
                        .zipWith(getSchemaCompatibilityInfoOrGlobal(clusterName, schemaName))
                        .map(tuple -> {
                            SchemaSubject schema = tuple.getT1();
                            String compatibilityLevel = tuple.getT2().getCompatibility().getValue();
                            schema.setCompatibilityLevel(compatibilityLevel);
                            return schema;
                        })
                )
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<ResponseEntity<Void>> deleteSchemaSubjectByVersion(String clusterName, String schemaName, Integer version) {
        return this.deleteSchemaSubject(clusterName, schemaName, String.valueOf(version));
    }

    public Mono<ResponseEntity<Void>> deleteLatestSchemaSubject(String clusterName, String schemaName) {
        return this.deleteSchemaSubject(clusterName, schemaName, LATEST);
    }

    private Mono<ResponseEntity<Void>> deleteSchemaSubject(String clusterName, String schemaName, String version) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.delete()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_BY_VERSION, schemaName, version)
                        .retrieve()
                        .onStatus(NOT_FOUND::equals,
                                throwIfNotFoundStatus(formatted("No such schema %s with version %s", schemaName, version))
                        ).toBodilessEntity()
                ).orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<ResponseEntity<Void>> deleteSchemaSubjectEntirely(String clusterName, String schemaName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.delete()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT, schemaName)
                        .retrieve()
                        .onStatus(NOT_FOUND::equals, throwIfNotFoundStatus(formatted("No such schema %s", schemaName))
                        )
                        .toBodilessEntity())
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<SchemaSubject> registerNewSchema(String clusterName, Mono<NewSchemaSubject> newSchemaSubject) {
        return newSchemaSubject
                .flatMap(schema -> {
                    Mono<InternalNewSchema> newSchema = Mono.just(new InternalNewSchema(schema.getSchema()));
                    String subject = schema.getSubject();
                    return createNewSchema(clusterName, subject, newSchema);
                });
    }

    private Mono<SchemaSubject> createNewSchema(String clusterName, String subject, Mono<InternalNewSchema> newSchemaSubject) {
        return clustersStorage.getClusterByName(clusterName)
                .map(KafkaCluster::getSchemaRegistry)
                .map(schemaRegistry -> {
                            Mono<SchemaSubject> checking = webClient.post()
                                    .uri(schemaRegistry + URL_SUBJECT, subject)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromPublisher(newSchemaSubject, InternalNewSchema.class))
                                    .retrieve()
                                    .onStatus(NOT_FOUND::equals, res -> Mono.empty())
                                    .bodyToMono(SchemaSubject.class)
                                    .handle((schema, handler) -> {
                                        if(Objects.isNull(schema) && Objects.isNull(schema.getId())) {
                                            handler.complete();
                                        } else {
                                            handler.error(new DuplicateEntityException("Such schema already exists"));
                                        }
                                    });
//                    webClient.post()
//                            .uri(schemaRegistry + URL_SUBJECT_VERSIONS, subject)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .body(BodyInserters.fromPublisher(newSchemaSubject, InternalNewSchema.class))
//                            .retrieve()
//                            .onStatus(UNPROCESSABLE_ENTITY::equals, r -> Mono.error(new UnprocessableEntityException("Invalid params")))
//                            .bodyToMono(SubjectIdResponse.class)

                            checking.subscribe(System.out::println);
                            return checking;
                        }
                )
//                .map(resp -> getLatestSchemaSubject(clusterName, subject))
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    @NotNull
    private Function<ClientResponse, Mono<? extends Throwable>> throwIfNotFoundStatus(String formatted) {
        return resp -> Mono.error(new NotFoundException(formatted));
    }

    /**
     * Updates a compatibility level for a <code>schemaName</code>
     *
     * @param schemaName is a schema subject name
     * @see com.provectus.kafka.ui.model.CompatibilityLevel.CompatibilityEnum
     */
    public Mono<Void> updateSchemaCompatibility(String clusterName, String schemaName, Mono<CompatibilityLevel> compatibilityLevel) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> {
                    String configEndpoint = Objects.isNull(schemaName) ? "/config" : "/config/{schemaName}";
                    return webClient.put()
                            .uri(cluster.getSchemaRegistry() + configEndpoint, schemaName)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromPublisher(compatibilityLevel, CompatibilityLevel.class))
                            .retrieve()
                            .onStatus(NOT_FOUND::equals,
                                    throwIfNotFoundStatus(formatted("No such schema %s", schemaName)))
                            .bodyToMono(Void.class);
                }).orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<Void> updateSchemaCompatibility(String clusterName, Mono<CompatibilityLevel> compatibilityLevel) {
        return updateSchemaCompatibility(clusterName, null, compatibilityLevel);
    }

    public Mono<CompatibilityLevel> getSchemaCompatibilityLevel(String clusterName, String schemaName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> {
                    String configEndpoint = Objects.isNull(schemaName) ? "/config" : "/config/{schemaName}";
                    return webClient.get()
                            .uri(cluster.getSchemaRegistry() + configEndpoint, schemaName)
                            .retrieve()
                            .bodyToMono(InternalCompatibilityLevel.class)
                            .map(mapper::toCompatibilityLevel)
                            .onErrorResume(error -> Mono.empty());
                }).orElse(Mono.empty());
    }

    public Mono<CompatibilityLevel> getGlobalSchemaCompatibilityLevel(String clusterName) {
        return this.getSchemaCompatibilityLevel(clusterName, null);
    }

    private Mono<CompatibilityLevel> getSchemaCompatibilityInfoOrGlobal(String clusterName, String schemaName) {
        return this.getSchemaCompatibilityLevel(clusterName, schemaName)
                .switchIfEmpty(this.getGlobalSchemaCompatibilityLevel(clusterName));
    }

    public Mono<CompatibilityCheckResponse> checksSchemaCompatibility(String clusterName, String schemaName, Mono<NewSchemaSubject> newSchemaSubject) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.post()
                        .uri(cluster.getSchemaRegistry() + "/compatibility/subjects/{schemaName}/versions/latest", schemaName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromPublisher(newSchemaSubject, NewSchemaSubject.class))
                        .retrieve()
                        .onStatus(NOT_FOUND::equals, throwIfNotFoundStatus(formatted("No such schema %s", schemaName)))
                        .bodyToMono(InternalCompatibilityCheck.class)
                        .map(mapper::toCompatibilityCheckResponse)
                        .log()
                ).orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public String formatted(String str, Object... args) {
        return new Formatter().format(str, args).toString();
    }

    public static String extractRecordType(String schema) {
        if (schema.contains("record")) {
            return "AVRO";
        } else if (schema.contains("proto")) {
            return "PROTO";
        } else return schema;
    }
}
