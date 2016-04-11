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

    public Observable<RangerInstance> getInstanceEvents(Map<String, SimpleShardedServiceFinder<ShardInfo>> serviceFinders,
                                                        String environment, String namespace) {
        final ShardInfo shardInfo = new ShardInfo(environment);
        return Observable.
                create((Subscriber<? super RangerInstance> subscriber) -> {
                    try {
                        serviceFinders.forEach( (service, serviceFinder) -> {
                            List<ServiceNode<ShardInfo>> instances = serviceFinder.getAll(shardInfo);
                            for (ServiceNode<ShardInfo> node : instances) {
                                logger.info("Fetching instance list for service: {} | Instance: {}" + service, node.getHost());
                                switch (node.getHealthcheckStatus()) {
                                    case healthy:
                                        subscriber.onNext(RangerInstance.create(node, namespace, service));
                                }
                            }
                            subscriber.onCompleted();
                        });
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .toList()
                .repeatWhen(a -> a.flatMap(n -> Observable.timer(30, TimeUnit.SECONDS))) // repeat after 30 second delay
                .startWith(new ArrayList<RangerInstance>())
                .buffer(2, 1)
                .filter(l -> l.size() == 2)
                .flatMap(RangerInstanceDiscovery::delta);
    }

    static Observable<RangerInstance> delta(List<List<RangerInstance>> listOfLists) {
        if (listOfLists.size() == 1) {
            return Observable.from(listOfLists.get(0));
        } else {
            // diff the two
            List<RangerInstance> newList = listOfLists.get(1);
            List<RangerInstance> oldList = new ArrayList<>(listOfLists.get(0));

            Set<RangerInstance> delta = new LinkedHashSet<>();
            delta.addAll(newList);
            // remove all that match in old
            delta.removeAll(oldList);

            // filter oldList to those that aren't in the newList
            oldList.removeAll(newList);

            // for all left in the oldList we'll create DROP events
            for (RangerInstance old : oldList) {
                delta.add(RangerInstance.create(RangerInstance.Status.DOWN, old.getInstanceInfo(), old.getNamespace(), old.getService()));
            }

            return Observable.from(delta);
        }
    }

}