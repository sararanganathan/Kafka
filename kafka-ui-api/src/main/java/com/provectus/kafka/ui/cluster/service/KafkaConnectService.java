package com.provectus.kafka.ui.cluster.service;

import com.provectus.kafka.ui.cluster.client.KafkaConnectClients;
import com.provectus.kafka.ui.cluster.exception.NotFoundException;
import com.provectus.kafka.ui.cluster.mapper.ClusterMapper;
import com.provectus.kafka.ui.cluster.mapper.KafkaConnectMapper;
import com.provectus.kafka.ui.cluster.model.ClustersStorage;
import com.provectus.kafka.ui.cluster.model.KafkaCluster;
import com.provectus.kafka.ui.cluster.model.KafkaConnectCluster;
import com.provectus.kafka.ui.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class KafkaConnectService {
    private final ClustersStorage clustersStorage;
    private final ClusterMapper clusterMapper;
    private final KafkaConnectMapper kafkaConnectMapper;

    public Mono<Flux<Connect>> getConnects(String clusterName) {
        return Mono.just(
                Flux.fromIterable(clustersStorage.getClusterByName(clusterName)
                        .map(KafkaCluster::getKafkaConnect).stream()
                        .flatMap(Collection::stream)
                        .map(clusterMapper::toKafkaConnect)
                        .collect(Collectors.toList())
                )
        );
    }


    public Flux<String> getConnectors(String clusterName, String connectName) {
        return getConnectAddress(clusterName, connectName)
                .flatMapMany(connect ->
                        KafkaConnectClients.withBaseUrl(connect).getConnectors()
                                .doOnError(log::error)
                );
    }

    public Mono<Connector> createConnector(String clusterName, String connectName, Mono<NewConnector> connector) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        connector.cache()
                                .map(kafkaConnectMapper::toClient)
                                .flatMap(c ->
                                        KafkaConnectClients.withBaseUrl(connect).createConnector(c)
                                )
                                .flatMap(c -> getConnector(clusterName, connectName, c.getName()))
                );
    }

    public Mono<Connector> getConnector(String clusterName, String connectName, String connectorName) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        KafkaConnectClients.withBaseUrl(connect).getConnector(connectorName)
                                .map(kafkaConnectMapper::fromClient)
                                .flatMap(connector ->
                                        KafkaConnectClients.withBaseUrl(connect).getConnectorStatus(connector.getName())
                                                .map(connectorStatus -> {
                                                    var status = connectorStatus.getConnector();
                                                    connector.status(kafkaConnectMapper.fromClient(status));
                                                    return (Connector) new Connector()
                                                            .status(kafkaConnectMapper.fromClient(status))
                                                            .type(connector.getType())
                                                            .tasks(connector.getTasks())
                                                            .name(connector.getName())
                                                            .config(connector.getConfig());
                                                })
                                )
                );
    }

    public Mono<Map<String, Object>> getConnectorConfig(String clusterName, String connectName, String connectorName) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        KafkaConnectClients.withBaseUrl(connect).getConnectorConfig(connectorName)
                );
    }

    public Mono<Connector> setConnectorConfig(String clusterName, String connectName, String connectorName, Mono<Object> requestBody) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        requestBody.flatMap(body ->
                                KafkaConnectClients.withBaseUrl(connect).setConnectorConfig(connectorName, (Map<String, Object>) body)
                        )
                                .map(kafkaConnectMapper::fromClient)
                );
    }

    public Mono<Void> deleteConnector(String clusterName, String connectName, String connectorName) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        KafkaConnectClients.withBaseUrl(connect).deleteConnector(connectorName)
                );
    }

    public Mono<Void> updateConnectorState(String clusterName, String connectName, String connectorName, ConnectorAction action) {
        Function<String, Mono<Void>> kafkaClientCall;
        switch (action) {
            case RESTART:
                kafkaClientCall = connect -> KafkaConnectClients.withBaseUrl(connect).restartConnector(connectorName);
                break;
            case PAUSE:
                kafkaClientCall = connect -> KafkaConnectClients.withBaseUrl(connect).pauseConnector(connectorName);
                break;
            case RESUME:
                kafkaClientCall = connect -> KafkaConnectClients.withBaseUrl(connect).resumeConnector(connectorName);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
        return getConnectAddress(clusterName, connectName)
                .flatMap(kafkaClientCall);
    }

    public Flux<Task> getConnectorTasks(String clusterName, String connectName, String connectorName) {
        return getConnectAddress(clusterName, connectName)
                .flatMapMany(connect ->
                        KafkaConnectClients.withBaseUrl(connect).getConnectorTasks(connectorName)
                                .map(kafkaConnectMapper::fromClient)
                                .flatMap(task ->
                                        KafkaConnectClients.withBaseUrl(connect).getConnectorTaskStatus(connectorName, task.getId().getTask())
                                                .map(kafkaConnectMapper::fromClient)
                                                .map(task::status)
                                )
                );
    }

    public Mono<Void> restartConnectorTask(String clusterName, String connectName, String connectorName, Integer taskId) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        KafkaConnectClients.withBaseUrl(connect).restartConnectorTask(connectorName, taskId)
                );
    }

    public Mono<Flux<ConnectorPlugin>> getConnectorPlugins(String clusterName, String connectName) {
        return Mono.just(getConnectAddress(clusterName, connectName)
                .flatMapMany(connect ->
                        KafkaConnectClients.withBaseUrl(connect).getConnectorPlugins()
                                .map(kafkaConnectMapper::fromClient)
                ));
    }

    public Mono<ConnectorPluginConfigValidationResponse> validateConnectorPluginConfig(String clusterName, String connectName, String pluginName, Mono<Object> requestBody) {
        return getConnectAddress(clusterName, connectName)
                .flatMap(connect ->
                        requestBody.flatMap(body ->
                                KafkaConnectClients.withBaseUrl(connect).validateConnectorPluginConfig(pluginName, (Map<String, Object>) body)
                        )
                                .map(kafkaConnectMapper::fromClient)
                );
    }

    private Mono<KafkaCluster> getCluster(String clusterName) {
        return clustersStorage.getClusterByName(clusterName)
                .map(Mono::just)
                .orElse(Mono.error(new NotFoundException("No such cluster")));
    }

    private Mono<String> getConnectAddress(String clusterName, String connectName) {
        return getCluster(clusterName)
                .map(kafkaCluster ->
                        kafkaCluster.getKafkaConnect().stream()
                                .filter(connect -> connect.getName().equals(connectName))
                                .findFirst()
                                .map(KafkaConnectCluster::getAddress)
                )
                .flatMap(connect -> connect
                        .map(Mono::just)
                        .orElse(Mono.error(new NotFoundException("No such connect cluster")))
                );
    }
}
