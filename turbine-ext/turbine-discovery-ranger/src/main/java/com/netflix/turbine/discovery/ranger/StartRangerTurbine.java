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

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.turbine.Turbine;

public class StartRangerTurbine {
    private static final Logger logger = LoggerFactory.getLogger(StartRangerTurbine.class);

    public static void main(String[] args) {
        OptionParser optionParser = new OptionParser();
        optionParser.accepts("port").withRequiredArg();
        optionParser.accepts("namespace").withRequiredArg();
        optionParser.accepts("zookeeper").withRequiredArg();
        optionParser.accepts("environment").withRequiredArg();
        optionParser.accepts("streamPath").withRequiredArg();
        optionParser.accepts("services").withOptionalArg();

        OptionSet options = optionParser.parse(args);
        int port = -1;
        if (!options.has("port")) {
            System.err.println("Argument -port required for SSE HTTP server to start on. Eg. -port 8989");
            System.exit(-1);
        } else {
            try {
                port = Integer.parseInt(String.valueOf(options.valueOf("port")));
            } catch (NumberFormatException e) {
                System.err.println("Value of port must be an integer but was: " + options.valueOf("port"));
            }
        }

        String namespace = null;
        if (!options.has("namespace")) {
            System.err.println("Argument -namespace required for Ranger instance discovery. Eg. -namespace mynamespace");
            System.exit(-1);
        } else {
            namespace = String.valueOf(options.valueOf("namespace"));
        }

        String zookeeper = null;
        if (!options.has("zookeeper")) {
            System.err.println("Argument -zookeeper required. Eg. -zookeeper localhost:2181");
            System.exit(-1);
        } else {
            zookeeper = String.valueOf(options.valueOf("zookeeper"));
        }

        String environment = null;
        if (!options.has("environment")) {
            System.err.println("Argument -environment required. Eg. -environment stage");
            System.exit(-1);
        } else {
            environment = String.valueOf(options.valueOf("environment"));
        }

        String services = null;
        if (options.has("services")) {
            services = String.valueOf(options.valueOf("services"));
        }

        String streamPath = null;
        if (!options.has("streamPath")) {
            System.err.println("Argument -streamPath required. Eg. -streamPath hystrix.stream");
            System.exit(-1);
        } else {
            streamPath = String.valueOf(options.valueOf("streamPath"));
        }

        logger.info("Turbine => Ranger Namespace: " + namespace);
        logger.info("Turbine => Zookeeper Connection String: " + zookeeper);
        logger.info("Turbine => Environment: " + environment);
        logger.info("Turbine => Services: " + (services == null ? "ALL" : services));

        try {
            Turbine.startServerSentEventServer(port, RangerStreamDiscovery.create(zookeeper, namespace, services, environment, streamPath));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
