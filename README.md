# SPARQL examples

This is a collection of SPARQL examples usable on different SIB related SPARQL endpoints or datasets. The examples are stored one query per file in project specific repositories in the [examples](https://github.com/sib-swiss/sparql-examples/tree/master/examples) folder.

Each SPARQL query is itself in a turtle file. We use the following ontologies for the basic concepts.

* ShACL for the relation to the text of the Select/Ask queries, and declaring prefixes
* RDFS for comments and labels as shown in the user interfaces, annotated with a language tag
* RDF for basic type relations
* schema.org for the target SPARQL endpoint and tagging relevant keywords

The following illustrates an example to retrieve retrieve human enzymes that metabolize sphingolipids from the UniProt SPARQL endpoint, with a service call to Rhea endpoint.

```turtle
@prefix ex: <https://sparql.uniprot.org/.well-known/sparql-examples/> . # <!-- change per dataset
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix schema: <https://schema.org/> .
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix spex:<https://purl.expasy.org/sparql-examples/ontology#> .

ex:040 # <!-- UniProt, Rhea and Swiss-Lipids are numbered but this can be anything.
	a sh:SPARQLExecutable, sh:SPARQLSelectExecutable ;
    rdfs:comment "Retrieve human enzymes that metabolize sphingolipids and are annotated in ChEMBL"@en ;
    sh:prefixes _:sparql_examples_prefixes ; # <!-- required for the import of the prefix declarations. Note the blank node
    sh:select """PREFIX CHEBI: <http://purl.obolibrary.org/obo/CHEBI_>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rh: <http://rdf.rhea-db.org/>
PREFIX taxon: <http://purl.uniprot.org/taxonomy/>
PREFIX up: <http://purl.uniprot.org/core/>

SELECT DISTINCT ?protein ?chemblEntry WHERE {
  SERVICE <https://sparql.rhea-db.org/sparql> {
    ?rhea rdfs:subClassOf rh:Reaction ;
      rh:side/rh:contains/rh:compound/rh:chebi/rdfs:subClassOf+ CHEBI:26739 .
  }
  ?protein up:annotation/up:catalyticActivity/up:catalyzedReaction ?rhea ;
    up:organism taxon:9606 ;
    rdfs:seeAlso ?chemblEntry .
  ?chemblEntry up:database <http://purl.uniprot.org/database/ChEMBL> .
}""" ;
    schema:keywords "enzyme" ;
    schema:target <https://sparql.uniprot.org/sparql/> ;
    spex:federatesWith <https://sparql.rhea-db.org/sparql> .
```

## Artifact generation and quality assurance

We use the [SIB SPARQL Examples utils](https://github.com/sib-swiss/sparql-examples-utils/) for testing and generating artifacts.

First, download the jar file with:

```bash
wget -O sparql-examples-utils.jar 'https://github.com/sib-swiss/sparql-examples-utils/releases/download/v2.0.19/sparql-examples-utils-2.0.19-uber.jar'
```

### Compile all query files into one file to upload to your endpoint

Compile all query files for a specific example folder, into a local file including the prefixes/namespaces definitions:

```bash
java -jar sparql-examples-utils.jar convert -i examples/ -p UniProt -f ttl > examples_UniProt.ttl
```

> You can then load this file to this project SPARQL endpoint! We recommend to upload it to a named graph: your endpoint URL + `/.well-known/sparql-examples`

Or compile for all example folders, as JSON-LD, to the standard output:

```bash
java -jar sparql-examples-utils.jar convert -i examples/ -p all -f jsonld
```

### Generate RQ files

For easier use by other tools we can also generate [rq](https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#mediaType) files. Following the syntax of [grlc](https://grlc.io/) allowing to use these queries as APIs.
```bash
java -jar sparql-examples-utils.jar convert -i examples/ -p all -r
```

### Generate markdown file

Generate markdown files with the query and a mermaid diagram of the queries, to be used to deploy a static website for the query examples.

```bash
java -jar sparql-examples-utils.jar convert -i examples/ -m
```

### Testing the queries

The queries are parsed and validated but not executed with junit using the Tester

```bash
java -jar sparql-examples-utils.jar test --input-directory=./examples
```

should return no test failures. RDF4j and Jena are both a lot stricter than virtuoso.

The queries can be executed automatically on all endpoints they apply to using an extra argument `--also-run-slow-tests`:

```bash
java -jar sparql-examples-utils.jar test --input-directory=./examples/MetaNetX --also-run-slow-tests
```

> This does change the queries to add a LIMIT 1 if no limit was set in the query. Then check if there is a result it is fetched.

## Querying for queries

As the SPARQL examples are themselves RDF, they can be queried for as soon as they are loaded in a SPARQL endpoint.
```sparql
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX spex: <https://purl.expasy.org/sparql-examples/ontology#>

SELECT DISTINCT ?sq ?comment ?query
WHERE {
    ?sq a sh:SPARQLExecutable ;
        rdfs:comment ?comment ;
        sh:select|sh:ask|sh:construct|spex:describe ?query .
} ORDER BY ?sq
```

## Finding queries that run on more than one endpoint

This expects the Jena tools to be available in your $PATH. e.g. `export PATH="$JENA_HOME/bin:$PATH"`

```bash
java -jar sparql-examples-utils.jar convert -i examples/ -p all -f ttl > examples_all.ttl

sparql --data examples_all.ttl "SELECT ?query (GROUP_CONCAT(?target ; separator=', ') AS ?targets) WHERE { ?query <https://schema.org/target> ?target } GROUP BY ?query HAVING (COUNT(DISTINCT ?target) > 1) "
```

## How to cite this work

If you reuse any part of this work, please cite [the GigaScience paper](https://academic.oup.com/gigascience/article/doi/10.1093/gigascience/giaf045/8133871):

```
@misc{largecollectionsparqlquestionquery,
    author = {Bolleman, Jerven and Emonet, Vincent and Altenhoff, Adrian and Bairoch, Amos and Blatter, Marie-Claude and Bridge, Alan and Duvaud, Severine and Gasteiger, Elisabeth and Kuznetsov, Dmitry and Moretti, Sebastien and Michel, Pierre-Andre and Morgat, Anne and Pagni, Marco and Redaschi, Nicole and Zahn-Zabal, Monique and Mendes de Farias, Tarcisio and Sima, Ana Claudia},
    doi = {10.1093/gigascience/giaf045},
    month = {10},
    title = {A large collection of bioinformatics question-query pairs over federated knowledge graphs: methodology and applications},
    url = {https://github.com/sib-swiss/sparql-examples-utils},
    year = {2025}
}
```
