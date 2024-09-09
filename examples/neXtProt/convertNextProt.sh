#!/usr/bin/bash
while read -r i;
do
	id=$(echo $i | jq '.[0]')
	title=$(echo $i | jq '.[2]')
	sparql=$(echo $i | jq '.[1]'| sed -e 's|entry:|nextprot:|g' |sed -e 's|cv:|nextprot_cv:|g')
	sparql=$(echo $sparql| sed -e 's|chebi\:CHEBI\_|CHEBI:|g')
	filename="${id:1:-1}.ttl"

	echo "prefix ex: <https://sparql.nextprot.org/.well-known/sparql-examples>" > $filename
	echo "prefix sh: <http://www.w3.org/ns/shacl#> "  >> $filename
	echo "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"  >> $filename
	echo "prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>"  >> $filename
	echo "prefix schema:<http://schema.org/>"  >> $filename
	echo "ex:${id:1:-1}" >> $filename
	echo " sh:prefixes _:sparql_examples_prefixes ;" >> $filename
    echo "  a sh:SPARQLSelectExecutable, sh:SPARQLExecutable ;" >> $filename
	echo -e "  rdfs:comment '''${title:1:-1}''' ;"  >> $filename
	while read t;
	do
		echo " schema:keywords $t;" >> $filename
	done < <(echo $i| jq '.[3] | .'| grep -v "\[" | grep -v "\]"|tr ',' ' '|grep -v 'snorql-only')
	echo -e "  sh:select '''${sparql:1:-1}'''"  >> $filename
	echo "."  >> $filename
done < <(curl 'https://api.nextprot.org/queries/tutorial.json?snorql=true' | jq -c '. [] | [ .publicId, .sparql, .title, [.tags] ]')
