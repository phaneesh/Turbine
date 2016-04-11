/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.turbine.discovery.ranger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.ranger.ServiceFinderBuilders;
import com.flipkart.ranger.finder.sharded.SimpleShardedServiceFinder;
import com.flipkart.ranger.healthcheck.HealthcheckStatus;
import com.flipkart.ranger.model.ServiceNode;
import com.netflix.turbine.discovery.StreamAction;
import com.netflix.turbine.discovery.StreamDiscovery;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class RangerStreamDiscovery implements StreamDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(RangerStreamDiscovery.class);

    public static RangerStreamDiscovery create(String zookeeper, String namespace, String services, String environment, String streamPath) {
        return new RangerStreamDiscovery(zookeeper, namespace, services, environment, streamPath);
    }

    private final String zookeeper;
    private final String namespace;
    private final String services;
    private final String environment;
    private final String streamPath;

    private List<String> filteredServices;

    private final CuratorFramework curatorFramework;

    private final ObjectMapper objectMapper;
    
    private Map<String, SimpleShardedServiceFinder<ShardInfo>> serviceFinders = new HashMap<>();

    private RangerStreamDiscovery(String zookeeper, String namespace, String services, String environment, String streamPath) {
        this.zookeeper = zookeeper;
        this.namespace = namespace;
        this.services = services;
        this.environment = environment;
        this.streamPath = streamPath;
        this.objectMapper = new ObjectMapper();
        this.curatorFramework = CuratorFrameworkFactory.builder().connectString(this.zookeeper)
                .namespace(this.namespace).retryPolicy(new RetryNTimes(10000, 1000)).build();
        curatorFramework.start();
        if(StringUtils.isEmpty(services))
            try {
                filteredServices = curatorFramework.getChildren().forPath("/");
            } catch (Exception e) {
                logger.error("Error getting service list", e);
            }
        else
            filteredServices = Arrays.asList(this.services.split(","));
        init();
    }

    private void init() {
        filteredServices.forEach( service -> {
            try {
                final SimpleShardedServiceFinder<ShardInfo> serviceFinder = ServiceFinderBuilders
                        .<ShardInfo>shardedFinderBuilder().withCuratorFramework(curatorFramework)
                        .withNamespace(namespace)
                        .withServiceName(service)
                        .withDeserializer(data -> {
                            try {
                                JsonNode nodeInfoRoot = objectMapper.readTree(data);
                                if (nodeInfoRoot.has("node_data")) {
                                    ServiceNode serviceNode = new ServiceNode(nodeInfoRoot.get("host").asText(), nodeInfoRoot.get("port").asInt(), objectMapper.treeToValue(nodeInfoRoot.get("node_data"), ShardInfo.class));
                                    serviceNode.setHealthcheckStatus(HealthcheckStatus.valueOf(nodeInfoRoot.get("healthcheck_status").asText()));
                                    serviceNode.setLastUpdatedTimeStamp(nodeInfoRoot.get("last_updated_time_stamp").asLong());
                                    return serviceNode;
                                }
                                return objectMapper.readValue(data, new TypeReference<ServiceNode<ShardInfo>>(){});
                            }
                            catch (IOException e) {
                                throw new RuntimeException("Error deserializing results", e);
                            }
                        })
                        .build();
                serviceFinder.start();
                serviceFinders.put(service, serviceFinder);
            } catch (Exception e) {
                logger.error("Error initializing ranger service finders", e);
            }
        });
    }

    @Override
    public Observable<StreamAction> getInstanceList() {
        return new RangerInstanceDiscovery()
                .getInstanceEvents(serviceFinders, environment, namespace)
                .map(ei -> {
                    URI uri;
                    try {
                        uri = new URI(String.format("http://%s:%d/%s", ei.getHostName(), ei.getHostPort(), streamPath ));
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid URI", e);
                    }
                    if (ei.getStatus() == RangerInstance.Status.UP) {
                        return StreamAction.create(StreamAction.ActionType.ADD, uri);
                    } else {
                        return StreamAction.create(StreamAction.ActionType.REMOVE, uri);
                    }
                });
    }

}
