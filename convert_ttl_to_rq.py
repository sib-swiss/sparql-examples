import sys
from pathlib import Path

from rdflib import Graph, Namespace
from rdflib.namespace import RDF, RDFS

SH = Namespace("http://www.w3.org/ns/shacl#")
SCHEMA = Namespace("https://schema.org/")
SPEX = Namespace("https://purl.expasy.org/sparql-examples/ontology#")

_QUERY_PREDICATES = [
    (SH.select, "SELECT"),
    (SH.ask, "ASK"),
    (SH.construct, "CONSTRUCT"),
    (SH.describe, "DESCRIBE"),
    (SPEX.describe, "DESCRIBE"),
]

_SKIP_FILENAMES = {"prefixes.ttl", "sparql-examples-ontology.ttl"}


def convert_ttl_to_rq(ttl_path: Path) -> Path:
    """Parse a SPARQL example .ttl file and write a .rq file beside it."""
    g = Graph()
    g.parse(ttl_path, format="turtle")

    # Find the subject that holds the query body
    subject = None
    query_body = None
    for pred, _ in _QUERY_PREDICATES:
        for s, _, o in g.triples((None, pred, None)):
            subject = s
            query_body = str(o)
            break
        if subject is not None:
            break

    if subject is None or query_body is None:
        raise ValueError(f"No SPARQL query predicate found in {ttl_path}")

    # Metadata
    endpoints = sorted({str(o) for o in g.objects(subject, SCHEMA.target)})
    summary = next((str(o) for o in g.objects(subject, RDFS.comment)), None)
    keywords = sorted({str(o) for o in g.objects(subject, SCHEMA.keywords)})

    # Build header
    lines: list[str] = []
    if endpoints:
        lines.append(f"#+ endpoint: {", ".join(endpoints)}")
    if summary:
        lines.append(f"#+ summary: {summary.replace(chr(10), chr(10) + '#+   ')}")
    if keywords:
        # lines.append(f"#+ tags: {", ".join(keywords)}")
        lines.append("#+ tags:")
        for kw in keywords:
            lines.append(f"#+ - {kw}")

    lines.append(query_body.strip())

    out_path = ttl_path.with_suffix(".rq")
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return out_path


def convert_all(examples_dir: Path) -> None:
    """Recursively convert all SPARQL example .ttl files to .rq files."""
    ttl_files = sorted(
        f for f in examples_dir.rglob("*.ttl") if f.name not in _SKIP_FILENAMES
    )
    failed: list[tuple[Path, Exception]] = []
    for ttl_file in ttl_files:
        try:
            out_path = convert_ttl_to_rq(ttl_file)
            print(f"  {ttl_file.relative_to(examples_dir)} → {out_path.name}")
        except Exception as exc:
            failed.append((ttl_file, exc))

    if failed:
        print(f"\nFailed to convert {len(failed)} file(s):", file=sys.stderr)
        for f, exc in failed:
            print(f"  {f.relative_to(examples_dir)}: {exc}", file=sys.stderr)

    print(f"\nConverted {len(ttl_files) - len(failed)}/{len(ttl_files)} files.")


def main() -> None:
    examples_dir = Path(__file__).parent / "examples"
    print(f"Converting SPARQL examples in {examples_dir}\n")
    convert_all(examples_dir)


if __name__ == "__main__":
    main()

