#!/usr/bin/bash
while read -r i;
do
	id=$(echo $i | jq '.[0]')
	title=$(echo $i | jq '.[2]')
	sparql=$(echo $i | jq '.[1]'|sed -e 's| :| np:|g'|sed -e 's|/:|/np:|g'|sed -e 's|;:|; np:|g' |sed -e 's|cv:|nextprot_cv:|g')
	filename="${id:1:-1}.ttl"

	echo "prefix ex: <https://sparql.nextprot.org/.well-known/sparql-examples>" >$filename
	echo "prefix sh: <http://www.w3.org/ns/shacl#> "  >> $filename
	echo "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"  >> $filename
	echo "prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>"  >> $filename
	echo "ex:${id:1:-1}" >> $filename
	echo " sh:prefixes _:sparql_examples_prefixes ;" >> $filename
	echo -e "  rdfs:comment '''${title:1:-1}''' ;"  >> $filename
	echo -e "  sh:select '''${sparql:1:-1}'''"  >> $filename
	echo "."  >> $filename
done < <(curl 'https://api.nextprot.org/queries/tutorial.json?snorql=true' | jq -c '. [] | [ .publicId, .sparql, .title ]')
