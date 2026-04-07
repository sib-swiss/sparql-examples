UTILS_VERSION := 2.0.23
JAR           := data/sparql-examples-utils.jar
JAR_URL       := https://github.com/sib-swiss/sparql-examples-utils/releases/download/v$(UTILS_VERSION)/sparql-examples-utils-$(UTILS_VERSION)-uber.jar

# Default base URL for `make ttl` (can be overridden with command args)
base ?= https://sib-swiss.github.io/sparql-examples/.well-known/sparql-examples/

# Per-endpoint base and SPARQL endpoint URLs
base_uniprot     := https://sparql.uniprot.org/.well-known/sparql-examples/
endpoint_uniprot := https://sparql.uniprot.org/sparql/

base_bgee        := https://www.bgee.org/sparql/.well-known/sparql-examples/
endpoint_bgee    := https://www.bgee.org/sparql/

base_cellosaurus     := https://purl.expasy.org/cellosaurus/rdf/ontology/
endpoint_cellosaurus := https://sparql.cellosaurus.org/sparql

base_emi     := https://kg.earthmetabolome.org/metrin/api/.well-known/examples/
endpoint_emi := https://kg.earthmetabolome.org/metrin/api/

base_glyconnect     := https://glyconnect.expasy.org/.well-known/sparql-examples
endpoint_glyconnect := https://glyconnect.expasy.org/sparql

base_hamap     := https://sparql.hamap.expasy.org/.well-known/sparql-examples/
endpoint_hamap := https://hamap.expasy.org/sparql/

base_metanetx     := https://sparql.metanetx.org/.well-known/sparql-examples/
endpoint_metanetx := https://rdf.metanetx.org/sparql/

base_nextprot     := https://purl.expasy.org/sparql-examples/neXtProt/
endpoint_nextprot := https://sparql.nextprot.org/sparql

base_oma     := https://sparql.omabrowser.org/.well-known/sparql-examples/
endpoint_oma := https://sparql.omabrowser.org/sparql

base_orthodb     := https://sparql.orthodb.org/.well-known/sparql-examples/
endpoint_orthodb := https://sparql.orthodb.org/sparql/

base_rhea     := https://sparql.rhea-db.org/.well-known/sparql-examples/
endpoint_rhea := https://sparql.rhea-db.org/sparql

base_swisslipids     := https://sparql.swisslipids.org/.well-known/sparql-examples/
endpoint_swisslipids := https://sparql.swisslipids.org/sparql/

ENDPOINTS := uniprot bgee cellosaurus emi glyconnect hamap metanetx nextprot oma orthodb rhea swisslipids

.PHONY: install ttl $(ENDPOINTS) fmt md test clean

install:
	mkdir -p data
	curl -L "$(JAR_URL)" -o $(JAR)

ttls: clean
	java -jar $(JAR) import-rq -i examples -c "endpoints.properties"

ttl:
	java -jar $(JAR) convert -i examples $(if $(project),-p $(project)) -f ttl > data/examples$(if $(project),_$(project)).ttl

$(ENDPOINTS): clean
	java -jar $(JAR) import-rq -i examples -b "$(base_$@)" --endpoint $(endpoint_$@)
	java -jar $(JAR) convert -i examples -f ttl > data/examples.ttl

md: data/examples.ttl
	java -jar $(JAR) convert -i examples -m

test: data/examples.ttl
	java -jar $(JAR) test -i examples

fmt:
	for f in examples/**/*.rq; do uvx qlue-ls format --writeback "$$f"; done

clean:
	rm -f examples/**/*.ttl examples/**/*.md examples/*.ttl data/examples.ttl
