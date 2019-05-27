#!/usr/bin/bash
rm examples.ttl


echo "# baseURI: http://sparql.uniprot.org/.well-known/sparql-examples#" > examples.ttl
echo "# imports: http://purl.uniprot.org/core/ " >> examples.ttl
echo "# prefix: ex" >> examples.ttl

project="uniprot"
while getopts uhrs option; do
    case "$option" in
        u) project="uniprot";;
        h) project="hamap";;
        r) project="rhea";;
        s) project="swisslipids";;
        h) help; exit 0;;
        *) help; exit 1;;
    esac
done


if which riot
then
  riot --formatted=turtle prefixes.ttl  > examples_${project}.ttl
  riot --formatted=turtle $project/*.ttl  >> examples_${project}.ttl
else
  rapper -q -i turtle <(cat ${project}/[1-9]*.ttl prefixes.ttl) -o turtle  > examples_${project}.ttl
fi
