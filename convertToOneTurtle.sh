#!/usr/bin/bash



project="uniprot"
while getopts uhrsgmcp: option; do
    case "$option" in
        p) project="$OPTARG";;
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

case "$project" in
    trembl)
        ;&
    swiss-prot)
        if [ ! -L "${project}" ]
        then
            ln -s uniprot "${project}"
        fi
        ;;
esac

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
