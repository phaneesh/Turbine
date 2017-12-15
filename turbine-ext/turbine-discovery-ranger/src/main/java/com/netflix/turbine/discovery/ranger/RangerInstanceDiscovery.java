/*
 * Copyright 2013 Netflix
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

import com.flipkart.ranger.finder.sharded.SimpleShardedServiceFinder;
import com.flipkart.ranger.model.ServiceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RangerInstanceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(RangerInstanceDiscovery.class);

    public RangerInstanceDiscovery() {

    }

    public Observable<RangerInstance> getInstanceEvents(String service, SimpleShardedServiceFinder<ShardInfo> serviceFinder,
                                                        String environment, String namespace) {
        final ShardInfo shardInfo = new ShardInfo(environment);
        List<RangerInstance> nodes = new ArrayList<>();
        List<ServiceNode<ShardInfo>> instances = serviceFinder.getAll(shardInfo);
        for (ServiceNode<ShardInfo> node : instances) {
            logger.info("Fetching instance list for service: {} | Instance: {} | Port: {}", service, node.getHost(), node.getPort());
            switch (node.getHealthcheckStatus()) {
                case healthy:
                    nodes.add(RangerInstance.create(node, namespace, service));
            }
        }
        return Observable.from(nodes);
    }
}