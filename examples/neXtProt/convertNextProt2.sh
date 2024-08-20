#! /usr/bin/bash
for i in nextprot-queries/src/main/resources/nextprot-queries/*.rq;
do 
	id=${i:53:-3};
	if [ ! -f ${id}.ttl ];
	then 
		echo "@prefix ex: <https://sparql.nextprot.org/.well-known/sparql-examples> ." > $id.ttl
		echo "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." >> $id.ttl
		echo "@prefix schema: <https://schema.org/> ." >> $id.ttl
		echo "@prefix sh: <http://www.w3.org/ns/shacl#> ." >> $id.ttl
		echo "ex:${id} a sh:SPARQLExecutable, sh:SPARQLSelectExecutable ;" >> $id.ttl
		echo "  sh:prefixes _:sparql_examples_prefixes ;" >> $id.ttl
		grep "#title:" $i | sed -e "s|#title:|  rdfs:comment '''|" -e "s|$|''' ;|" >> $id.ttl
		grep "#tags:" $i | sed -e 's|#tags:|  schema:keyword "|g' | sed -e 's|,|", "|g' | sed -e 's|$|" ;|' >> $id.ttl
		echo "  schema:target <https://sparql.nextprot.org/sparql> ;" >> $id.ttl
		hasSource=$(grep -c "source:" $i)
		hasDb=$(grep -c "db:" $i)
		hasRdf=$(grep -c "rdf:" $i)
		hasRdfs=$(grep -c "rdfs:" $i)
		hasXsd=$(grep -c "xsd" $i)
		echo "  sh:select '''PREFIX : <http://nextprot.org/rdf/>" >> $id.ttl
		echo "PREFIX cv: <http://nextprot.org/rdf/terminology/>" >> $id.ttl
		if [ $hasSource -ne 0 ]
		then
			echo "PREFIX source: <http://nextprot.org/rdf/source/>" >> $id.ttl
		fi
		if [ $hasDb -ne 0 ]
		then
			echo "PREFIX db: <http://nextprot.org/rdf/db/>" >> $id.ttl
		fi
		if [ $hasRdf -ne 0 ]
		then
			echo "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" >> $id.ttl
		fi
		if [ $hasRdfs -ne 0 ]
		then
			echo "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" >> $id.ttl
		fi
		if [ $hasXsd -ne 0 ]
		then
			echo "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" >> $id.ttl
		fi
		if [ $(grep -c "dc:" $i) -ne 0 ]
		then
			echo "PREFIX dc: <http://purl.org/dc/elements/1.1/>" >> $id.ttl
		fi
		if [ $(grep -c "entry:" $i) -ne 0 ]
		then
			echo "PREFIX entry: <http://nextprot.org/rdf/entry/>" >> $id.ttl
		fi
		echo -e "$(grep -v '^#' $i)''' ." >> $id.ttl
	fi;
done
