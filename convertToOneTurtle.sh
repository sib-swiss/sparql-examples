#!/usr/bin/bash
rm examples.ttl
echo "# baseURI: http://sparql.uniprot.org/.well-known/sparql-examples#" > examples.ttl
echo "# imports: http://purl.uniprot.org/core/ " >> examples.ttl
echo "# prefix: ex" >> examples.ttl
if [ ! -z $(which riot) ]
then
  riot --formatted=turtle *.ttl  >> examples.ttl
else
  rapper -q -i turtle <(cat [1-9]*.ttl prefixes.ttl) -o turtle  >> examples.ttl
fi
