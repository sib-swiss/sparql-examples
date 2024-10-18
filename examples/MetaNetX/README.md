# MetaNetX prefixes generation

⚠️ `prefixes.ttl` is automatically generated as follow:

```bash
git clone https://github.com/MetaNetX/MNXtools.git MNXtools
cd MNXtools/perl
perl -e 'use lib "."; use Prefix; print Prefix->new()->get_prefix_ontology()' > prefixes.ttl
```
