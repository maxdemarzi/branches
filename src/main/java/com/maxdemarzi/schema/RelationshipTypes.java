package com.maxdemarzi.schema;

import org.neo4j.graphdb.RelationshipType;

public enum RelationshipTypes implements RelationshipType {
    HAS,
    IS_TRUE,
    IS_FALSE,
    REQUIRES
}
