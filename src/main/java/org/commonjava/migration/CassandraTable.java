package org.commonjava.migration;

public class CassandraTable
{

    private String keyspace;
    private String table;
    private String tempView;
    private String filter;
    private String id;

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

    @Override
    public String toString() {
        return "Table{" +
                "keyspace='" + keyspace + '\'' +
                ", table='" + table + '\'' +
                ", tempView='" + tempView + '\'' +
                ", filter='" + filter + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
