# MetaNetX prefixes generation

⚠️ `prefixes.ttl` is automatically generated as follow:

```bash
git clone https://github.com/MetaNetX/MNXtools.git MNXtools
cd MNXtools/perl
perl -e 'use lib "."; use Prefix; print Prefix->new()->get_prefix_ontology()' > prefixes.ttl
```
**Nota Bene**: There are often two prefixes and two IRIs for the same entity. One corresponds to 
the IRI used by the RDF community at SIB, and often starts with "http://purl.". It
is the recommended prefix. The other prefix corresponds to the "MIRIAM" prefixes 
which were adopted by the Systems Biology community (https://sbml.org/documents/elaborations/miriam_annotation_syntax/), 
and typically starts with "https://identifiers.org/". Very unfortunately, identifiers.org 
has promoted the usage of the short form of IRIs in SBML annotations, and is maintaining 
a list of "official" MIRIAM prefixes at https://registry.identifiers.org. To ensure 
interoperability with the Systems Biology community and avoid namespace clashes, MetaNetX 
has to respects the MIRIAM prefixe nomenclature, and has no other choice than to define 
ad hoc prefixes for those not covered by MIRIAM.   
