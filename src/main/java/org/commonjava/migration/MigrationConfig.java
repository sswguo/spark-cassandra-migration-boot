package org.commonjava.migration;

import java.util.List;

public class MigrationConfig
{

    private String host;
    private String port;
    private String user;
    private String password;
    private List<CassandraTable> tables;

    private String awsAccessKeyID;

    private String awsSecretAccessKey;

    private String bucketRegion;

    private String bucketName;

    private Boolean sharedStorage;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<CassandraTable> getTables() {
        return tables;
    }

    public void setTables(List<CassandraTable> tables) {
        this.tables = tables;
    }

    public String getAwsAccessKeyID() {
        return awsAccessKeyID;
    }

    public void setAwsAccessKeyID(String awsAccessKeyID) {
        this.awsAccessKeyID = awsAccessKeyID;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public void setAwsSecretAccessKey(String awsSecretAccessKey) {
        this.awsSecretAccessKey = awsSecretAccessKey;
    }

    public String getBucketRegion() {
        return bucketRegion;
    }

    public void setBucketRegion(String bucketRegion) {
        this.bucketRegion = bucketRegion;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Boolean getSharedStorage() {
        return sharedStorage;
    }

    public void setSharedStorage(Boolean sharedStorage) {
        this.sharedStorage = sharedStorage;
    }

    @Override
    public String toString() {
        return "MigrationConfig{" +
                "host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", tables=" + tables +
                ", awsAccessKeyID='" + awsAccessKeyID + '\'' +
                ", awsSecretAccessKey='" + awsSecretAccessKey + '\'' +
                ", bucketRegion='" + bucketRegion + '\'' +
                ", bucketName='" + bucketName + '\'' +
                '}';
    }
}
