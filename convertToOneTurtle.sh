#!/usr/bin/bash



project="uniprot"
while getopts uhrsgmc option; do
    case "$option" in
        u) project="uniprot";;
        h) project="hamap";;
        r) project="rhea";;
        s) project="swisslipids";;
        g) project="glyconnect";;
        m) project="metanetx";;
        c) project="covid";;
        h) help; exit 0;;
        *) help; exit 1;;
    esac
done
rm examples_${project}.ttl
echo "# baseURI: http://sparql.${project}.org/.well-known/sparql-examples#" > examples_${project}.ttl
echo "# imports: http://purl.uniprot.org/core/ " >> examples_${project}.ttl
echo "# prefix: ex" >> examples_${project}.ttl


if which riot
then
  riot --formatted=turtle prefixes.ttl  > examples_${project}.ttl
  riot --formatted=turtle $project/*.ttl  >> examples_${project}.ttl
else
  rapper -q -i turtle <(cat ${project}/[1-9]*.ttl prefixes.ttl) -o turtle  > examples_${project}.ttl
fi
