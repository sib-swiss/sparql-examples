#!/usr/bin/bash



project="uniprot"
while getopts nuhrsgmcp: option; do
    case "$option" in
        p) project="$OPTARG";;
        u) project="uniprot";;
        h) project="hamap";;
        r) project="rhea";;
        s) project="swisslipids";;
        g) project="glyconnect";;
        m) project="metanetx";;
        c) project="covid";;
        n) project="nextprot";;
        b) project="bgee";;
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

if [ -f examples_${project}.ttl ]
then
    rm examples_${project}.ttl
fi
echo "# baseURI: http://sparql.${project}.org/.well-known/sparql-examples#" > examples_${project}.ttl
echo "# imports: http://purl.uniprot.org/core/ " >> examples_${project}.ttl
echo "# prefix: ex" >> examples_${project}.ttl


if which riot
then
  cat prefixes.ttl $project/*.ttl | riot --syntax=turtle --formatted=turtle > examples_${project}.ttl
else
  rapper -q -i turtle <(cat ${project}/[1-9]*.ttl prefixes.ttl) -o turtle  > examples_${project}.ttl
fi
