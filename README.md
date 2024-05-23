# SPARQL examples

This is a collection of SPARQL examples useable on different SIB related SPARQL endpoints or datasets. The examples are stored one query per file in project
specific repositories. 

Each SPARQL query is itself in a turtle file. We use the following ontologies for the basic concepts.

* ShACL for the relation to the text of the Select/Ask queries, and declaring prefixes
* RDFS for comments and labels as shown in the user interfaces
* RDF for basic type relations

```sparql
prefix ex: <https://sparql.uniprot.org/.well-known/sparql-examples/>  # <!-- change per dataset
prefix sh: <http://www.w3.org/ns/shacl#> 
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> 
ex:1  # <!-- UniProt, Rhea and Swiss-Lipids are numbered but this can be anything.
    a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;
    sh:prefixes _:sparql_examples_prefixes ; # <!-- required for the import of the prefix declarations. Note the blank node
    rdfs:comment """A comment <em>May have HTML in them</em>"""^^rdf:HTML ;
    sh:select """SELECT ?s
WHERE
{
    ?s ?p ?o .
}""" .
```

# QA.

There is a maven task/test that validates that all queries in the examples are valid according to [Jena](https://jena.apache.org). and
[eclipse RDF4j](https://rdf4j.org/). This expects your `JAVA_HOME` to be version 21 or above

# Conversion

To load the examples into a sparql endpoint they should be concatanated into one example file. Use the script `convertIntoOneTurtle.sh`
provide the project name with a `-p` parameter

This expects the Jena tools to be available in your $PATH. e.g. `export PATH="$JENA_HOME/bin:$PATH"

```bash
# e.g. make file examples_uniprot.ttl
./convertToOneTurtle.sh -p uniprot
```


# Querying for queries

As the SPARQL examples are in RDF once loaded in a SPARQL endpoint they can be queried for.
```sparql
PREFIX sh: <http://www.w3.org/ns/shacl#>
SELECT *
WHERE {
  ?ex sh:select|sh:ask|sh:construct|sh:describe ?query .
}
```

# Testing your queries

The queries are parsed and validated but not executed with junit/maven

```bash
mvn test
```
should return no test failures. RDF4j and Jena are both a lot stricter than virtuoso.


# Labeling queries

If you want to add a label to a query please use [schema.org keyword](https://schema.org/keywords)
