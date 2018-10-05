#!/usr/bin/bash
rm examples.ttl
echo "# baseURI: http://sparql.uniprot.org/.well-known/sparql-examples#" > examples.ttl
echo "# imports: http://purl.uniprot.org/core/ " >> examples.ttl
echo "# prefix: ex" >> examples.ttl
riot --formatted=turtle *.ttl  >> examples.ttl

