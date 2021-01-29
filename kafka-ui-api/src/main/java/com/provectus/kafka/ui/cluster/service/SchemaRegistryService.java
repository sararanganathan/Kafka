package com.provectus.kafka.ui.cluster.service;

import com.provectus.kafka.ui.cluster.exception.NotFoundException;
import com.provectus.kafka.ui.cluster.model.ClustersStorage;
import com.provectus.kafka.ui.model.NewSchemaSubject;
import com.provectus.kafka.ui.model.SchemaSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Log4j2
@RequiredArgsConstructor
public class SchemaRegistryService {
    private static final String URL_SUBJECTS = "/subjects";
    private static final String URL_SUBJECT = "/subjects/{subjectName}";
    private static final String URL_SUBJECT_VERSIONS = "/subjects/{subjectName}/versions";
    private static final String URL_SUBJECT_BY_VERSION = "/subjects/{subjectName}/versions/{version}";

    private final ClustersStorage clustersStorage;
    private final WebClient webClient;

    public Flux<String> getAllSchemaSubjects(String clusterName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECTS)
                        .retrieve()
                        .bodyToFlux(String.class))
                .orElse(Flux.error(new NotFoundException("No such cluster")));
    }

    public Flux<Integer> getSchemaSubjectVersions(String clusterName, String subjectName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_VERSIONS, subjectName)
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals, resp -> Mono.error(new NotFoundException("No such subject")))
                        .bodyToFlux(Integer.class))
                .orElse(Flux.error(new NotFoundException("No such cluster")));
    }

    public Flux<SchemaSubject> getSchemaSubjectByVersion(String clusterName, String subjectName, Integer version) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.get()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_BY_VERSION, subjectName, version)
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals, resp -> Mono.error(new NotFoundException("No such subject or version")))
                        .bodyToFlux(SchemaSubject.class))
                .orElse(Flux.error(new NotFoundException()));
    }

    public Mono<ResponseEntity<Void>> deleteSchemaSubjectByVersion(String clusterName, String subjectName, Integer version) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.delete()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_BY_VERSION, subjectName, version)
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals, resp -> Mono.error(new NotFoundException("No such subject or version")))
                        .toBodilessEntity())
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<ResponseEntity<Void>> deleteSchemaSubject(String clusterName, String subjectName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.delete()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT, subjectName)
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals, resp -> Mono.error(new NotFoundException("No such subject or version")))
                        .toBodilessEntity())
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    public Mono<ResponseEntity<SchemaSubject>> createNewSubject(String clusterName, String subjectSchema, Mono<NewSchemaSubject> newSchemaSubject) {
        return clustersStorage.getClusterByName(clusterName)
                .map(cluster -> webClient.post()
                        .uri(cluster.getSchemaRegistry() + URL_SUBJECT_VERSIONS, subjectSchema)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromPublisher(newSchemaSubject, NewSchemaSubject.class))
                        .retrieve()
                        .toEntity(SchemaSubject.class))
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }
}
