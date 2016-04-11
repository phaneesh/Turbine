package com.netflix.turbine.discovery.ranger;

/**
 * @author phaneesh
 */
public class ShardInfo {

    public ShardInfo() {

    }

    public ShardInfo(String environment) {
        this.environment = environment;
    }

    private String environment;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((environment == null) ? 0 : environment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ShardInfo) {
            return ((ShardInfo)obj).environment.equals(environment);
        } else {
            return false;
        }
    }
}
