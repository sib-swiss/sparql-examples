# SPARQL examples testing and conversion utilities

This is a set of tools to convert, test and help maintain SPARQL query examples.


The code comes from the [SIB SPARQL Examples](https://github.com/sib-swiss/sparql-examples/) project. 

For this code to work each query should be in a turtle file.

Each SPARQL query is itself in a turtle file. We use the following ontologies for the basic concepts.

* ShACL for the relation to the text of the Select/Ask queries, and declaring prefixes
* RDFS for comments and labels as shown in the user interfaces
* RDF for basic type relations
* schema.org for the target SPARQL endpoint and tagging relevant keywords

The following illustrates an example to retrieve all taxa from the UniProt SPARQL endpoint.

```sparql
prefix ex: <https://sparql.uniprot.org/.well-known/sparql-examples/>  # <!-- change per dataset
prefix sh: <http://www.w3.org/ns/shacl#> 
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> 
ex:1  # <!-- UniProt, Rhea and Swiss-Lipids are numbered but this can be anything.
    a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;
    sh:prefixes _:sparql_examples_prefixes ; # <!-- required for the import of the prefix declarations. Note the blank node
    rdfs:comment """A comment <em>May have HTML in them</em>. Example: Select all taxa from the <a href=\"https://www.uniprot.org/taxonomy/\">UniProt taxonomy</a>"""^^rdf:HTML ;
    sh:select """PREFIX up: <http://purl.uniprot.org/core/>

SELECT ?taxon
FROM <http://sparql.uniprot.org/taxonomy>
WHERE
{
    ?taxon a up:Taxon .
}""" ;
    schema:target <https://sparql.uniprot.org/sparql/> ;
    schema:keywords "taxa".
```

If you want to add a label to a query please use the [schema.org keywords](https://schema.org/keywords).

# Running the code

If using a release:
```bash
mkdir target
wget "https://github.com/sib-swiss/sparql-examples-utils/releases/download/v2.0.0/sparql-examples-utils-2.0.0-uber.jar" 
mv sparql-examples-utils-2.0.0-uber.jar target

# This target directory is only so that the commands in the examples match as if the code was build locally.
# basically target/ can be remove from all examples below
```
If building locally in this git repository:
```bash
mvn package
```


# Quality Assurance (QA).

To test your examples pass the folder/directory containing your examples as an argument (`--input-directory` to the `test` subcommand, e.g.:

```bash
java -jar target/sparql-examples-utils-*-uber.jar test --input-directory=../sparql-examples/examples
```

The queries can be executed automatically on all endpoints they apply to using an extra argument for the test `--also-run-slow-tests`. This does change the queries to add a `LIMIT 1` if no limit was set in the query.

```bash
java -jar target/sparql-examples-utils-*-uber.jar test --input-directory=../sparql-examples/examples -p MetaNetX --also-run-slow-tests
```

> [!NOTE]
>
> All CLI commands provided in this readme expects you have the [`sparql-examples`](https://github.com/sib-swiss/sparql-examples) folder cloned in the same directory alongside this `sparql-examples-utils` project folder. Feel free to change them for your own example folder and path.

# Conversion for upload in SPARQL endpoint

Before loading the examples into a SPARQL endpoint they should be concatenated into one file, including the prefixes/namespaces definitions.

Compile all query files for a specific example subfolder, into a local turtle file:

```bash
java -jar target/sparql-examples-utils-*-uber.jar convert -i ../sparql-examples/examples -p Bgee -f ttl > examples_Bgee.ttl
```

Or compile all subfolders as JSON-LD to the standard output:

```bash
java -jar target/sparql-examples-utils-*-uber.jar convert -i ../sparql-examples/examples -p all -f jsonld
```

## Conversion to RQ files

For easier use by other tools we can also generate [`.rq`](https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#mediaType) files. Following the syntax of [grlc.io](https://grlc.io/) allowing to use these queries as HTTP APIs.
```bash
java -jar target/sparql-examples-utils-*-uber.jar convert -i ../sparql-examples/examples -p all -r
```

## Generate markdown file

Generate markdown files with the query and a mermaid diagram of the queries, to be used to deploy a static website for the query examples.

```bash
java -jar target/sparql-examples-utils-*-uber.jar convert -i ../sparql-examples/examples -m
```

# Querying for queries

As the SPARQL examples are themselves RDF, they can be queried for as soon as they are loaded in a SPARQL endpoint.
```sparql
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT DISTINCT ?sq ?comment ?query
WHERE {
    ?sq a sh:SPARQLExecutable ;
        rdfs:comment ?comment ;
        sh:select|sh:ask|sh:construct|sh:describe ?query .
} ORDER BY ?sq
```

## Finding queries that run on more than one endpoint

```bash
java -jar target/sparql-examples-utils-*-uber.jar convert --input-directory ../sparql-examples/examples > examples_all.ttl

sparql --data examples_all.ttl "SELECT ?query (GROUP_CONCAT(?target ; separator=', ') AS ?targets) WHERE { ?query <https://schema.org/target> ?target } GROUP BY ?query HAVING (COUNT(DISTINCT ?target) > 1) "
```

# Native executable

The project is ready to be compiled by the native-image tool of the [GraalVM](https://www.graalvm.org/) project. 

To do so set your `JAVA_HOME` to the graalvm location and user maven package -Pnative

e.g. on linux with graalvm installed in your `$HOME/bin` directory:
```bash
export JAVA_HOME=$HOME/bin/graalvm-jdk-${GRAALVM_VERSION}
mvn package -Pnative

# Then there is now a local native binary that you can use to convert all entries
./target/sparql-examples-utils converter -i ${DIRECTORY_WITH_EXAMPLES}
```



