package org.neo4j.qa.aggregator.model;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

@Entity
public class StatisticRecord {
// ------------------------------ FIELDS ------------------------------

    @Id @GeneratedValue
    private long id;
    private String component;
    private String version;
    private Date timestamp;
    @ElementCollection
    private Map<String, Double> values;

// --------------------------- CONSTRUCTORS ---------------------------

    protected StatisticRecord() {
    }

    public StatisticRecord(String component, String version, Map<String, Double> values) {
        this.component = component;
        this.version = version;
        this.values = values;
        this.timestamp = new Date();
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getComponent() {
        return component;
    }

    public long getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

// -------------------------- OTHER METHODS --------------------------

    public Map<String, Double> getValues() {
        return unmodifiableMap(values);
    }
}
