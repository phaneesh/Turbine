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

import com.flipkart.ranger.model.ServiceNode;

public class RangerInstance {

    public enum Status {
        UP, DOWN
    }

    private final Status status;
    private final ServiceNode<ShardInfo> instance;
    private final String namespace;
    private final String service;

    private RangerInstance(Status status, ServiceNode<ShardInfo> instance, String namespace, String service) {
        this.status = status;
        this.instance = instance;
        this.namespace = namespace;
        this.service = service;
    }

    public static RangerInstance create(ServiceNode<ShardInfo> instance, String namespace, String service) {
        Status status;
        switch (instance.getHealthcheckStatus()) {
            case healthy:
                status = Status.UP;
                break;
            case unhealthy:
            default:
                status = Status.DOWN;
        }
        return new RangerInstance(status, instance, namespace, service);
    }

    public static RangerInstance create(Status status, ServiceNode<ShardInfo> instance, String namespace, String service) {
        return new RangerInstance(status, instance, namespace, service);
    }

    public Status getStatus() {
        return status;
    }

    public ServiceNode<ShardInfo> getInstanceInfo() {
        return instance;
    }

    public String getService() {
        return service;
    }

    public String getNamespace() {
        return namespace;
    }

    public Integer getHostPort() {
        return instance.getPort();
    }

    public String getHostName() {
        return instance.getHost();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getNamespace() == null) ? 0 : getNamespace().hashCode());
        result = prime * result + ((getService() == null) ? 0 : getService().hashCode());
        result = prime * result + ((getHostName() == null) ? 0 : getHostName().hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((getHostPort() == null) ? 0 : getHostPort().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RangerInstance other = (RangerInstance) obj;
        if (getNamespace() == null) {
            if (other.getNamespace() != null)
                return false;
        } else if (!getNamespace().equals(other.getNamespace()))
            return false;
        if (getService() == null) {
            if (other.getService() != null)
                return false;
        } else if (!getService().equals(other.getService()))
            return false;
        if (getHostName() == null) {
            if (other.getHostName() != null)
                return false;
        } else if (!getHostName().equals(other.getHostName()))
            return false;
        if (getHostPort() == null) {
            if (other.getHostPort() != null)
                return false;
        } else if (!getHostPort().equals(other.getHostPort()))
            return false;
        if (status != other.status)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RangerInstance [status=" + status  + ", hostname=" + instance.getHost() + "]";
    }

}
