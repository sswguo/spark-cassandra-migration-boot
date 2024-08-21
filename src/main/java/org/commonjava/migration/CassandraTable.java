package org.commonjava.migration;

public class CassandraTable
{

    private String keyspace;
    private String table;
    private String tempView;
    private String filter;
    private String id;

    /* Write Tuning Parameters - https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md */
    private String outputConsistencyLevel;
    private String outputBatchSizeRows;
    private String outputBatchSizeBytes;
    private String outputConcurrentWrites;

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getTempView() {
        return tempView;
    }

    public void setTempView(String tempView) {
        this.tempView = tempView;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOutputConsistencyLevel() {
        return outputConsistencyLevel;
    }

    public void setOutputConsistencyLevel(String outputConsistencyLevel) {
        this.outputConsistencyLevel = outputConsistencyLevel;
    }

    public String getOutputBatchSizeRows() {
        return outputBatchSizeRows;
    }

    public void setOutputBatchSizeRows(String outputBatchSizeRows) {
        this.outputBatchSizeRows = outputBatchSizeRows;
    }

    public String getOutputBatchSizeBytes() {
        return outputBatchSizeBytes;
    }

    public void setOutputBatchSizeBytes(String outputBatchSizeBytes) {
        this.outputBatchSizeBytes = outputBatchSizeBytes;
    }

    public String getOutputConcurrentWrites() {
        return outputConcurrentWrites;
    }

    public void setOutputConcurrentWrites(String outputConcurrentWrites) {
        this.outputConcurrentWrites = outputConcurrentWrites;
    }
}
