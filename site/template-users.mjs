// Writes examples/template-users.md, which the home page links to. It has two
// tables:
//
// 1. The repositories made from sib-swiss/sparql-examples-template, by forking
//    it, with GitHub's "Use this template" button, or by hand (listed in
//    site/template-users-extra.txt).
// 2. The forks of sib-swiss/sparql-examples, for reference. Forking is not the
//    recommended way to reuse the examples, so the page says so before the list.
//
// All of it comes from the GitHub GraphQL API. Repositories made with "Use this
// template" are not forks and have no list of their own, so they are found by
// searching for repositories with sparql examples in their name and reading
// each result's templateRepository. Only repositories whose name matches a
// search can be found this way, which is why the hand-maintained list exists.
//
// Run from the repository root:
//   GITHUB_TOKEN=... node site/template-users.mjs
// GraphQL needs a token, even for public data. In GitHub Actions the built-in
// GITHUB_TOKEN is enough.
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT = path.join(ROOT, 'examples', 'template-users.md');
// Repositories that use the template without GitHub knowing about it (not a
// fork, no template link), one owner/name per line.
const EXTRA_FILE = path.join(ROOT, 'site', 'template-users-extra.txt');

const TEMPLATE_OWNER = 'sib-swiss';
const TEMPLATE_NAME = 'sparql-examples-template';
const TEMPLATE = `${TEMPLATE_OWNER}/${TEMPLATE_NAME}`;
const EXAMPLES = 'sib-swiss/sparql-examples';
const GRAPHQL = 'https://api.github.com/graphql';
// Name searches that can find repositories made from the template. Names are
// chosen by the people who make them, so this finds most but not all of them.
const NAME_QUERIES = ['sparql examples in:name', 'sparql-examples in:name'];

const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
if (!token) {
  console.error('GITHUB_TOKEN is required: the GitHub GraphQL API does not accept unauthenticated requests.');
  process.exit(1);
}

const FIELDS = `
  fragment RepoFields on Repository {
    nameWithOwner
    url
    description
    pushedAt
    stargazerCount
    templateRepository { nameWithOwner }
  }
`;

async function graphql(query, variables = {}) {
  const response = await fetch(GRAPHQL, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'User-Agent': 'sparql-examples-site',
    },
    body: JSON.stringify({ query: query + FIELDS, variables }),
  });
  const text = await response.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    throw new Error(`GitHub GraphQL ${response.status}: ${text.slice(0, 300)}`);
  }
  if (!response.ok || body.errors) {
    throw new Error(`GitHub GraphQL ${response.status}: ${JSON.stringify(body.errors ?? body).slice(0, 300)}`);
  }
  return body.data;
}

// Collects every page of a connection. `select` returns the connection from the response data.
async function allNodes(query, variables, select) {
  const nodes = [];
  let cursor = null;
  for (;;) {
    const connection = select(await graphql(query, { ...variables, cursor }));
    nodes.push(...connection.nodes.filter(Boolean));
    if (!connection.pageInfo.hasNextPage) return { nodes, totalCount: connection.totalCount };
    cursor = connection.pageInfo.endCursor;
  }
}

// Forks of the template, which is where forking it shows up.
function templateForks() {
  return allNodes(
    `query($owner: String!, $name: String!, $cursor: String) {
      repository(owner: $owner, name: $name) {
        forks(first: 100, after: $cursor) { totalCount pageInfo { hasNextPage endCursor } nodes { ...RepoFields } }
      }
    }`,
    { owner: TEMPLATE_OWNER, name: TEMPLATE_NAME },
    (data) => data.repository.forks,
  );
}

// Forks of the original examples repository.
function examplesForks() {
  return allNodes(
    `query($owner: String!, $name: String!, $cursor: String) {
      repository(owner: $owner, name: $name) {
        forks(first: 100, after: $cursor) { totalCount pageInfo { hasNextPage endCursor } nodes { ...RepoFields } }
      }
    }`,
    { owner: 'sib-swiss', name: 'sparql-examples' },
    (data) => data.repository.forks,
  );
}

// Repositories with a name matching the query, with their template link.
function nameSearch(searchQuery) {
  return allNodes(
    `query($q: String!, $cursor: String) {
      search(query: $q, type: REPOSITORY, first: 100, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes { ... on Repository { ...RepoFields } }
      }
    }`,
    { q: searchQuery },
    (data) => data.search,
  );
}

async function repository(fullName) {
  const [owner, name] = fullName.split('/');
  const data = await graphql(
    `query($owner: String!, $name: String!) { repository(owner: $owner, name: $name) { ...RepoFields } }`,
    { owner, name },
  );
  return data.repository;
}

// The hand-maintained list in template-users-extra.txt. Lines starting with # are comments.
function extraRepositories() {
  if (!existsSync(EXTRA_FILE)) return [];
  return readFileSync(EXTRA_FILE, 'utf8')
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'));
}

function escapeCell(text) {
  return String(text ?? '')
    .replace(/\|/g, '\\|')
    .replace(/[\r\n]+/g, ' ')
    .trim();
}

function nameCell(repo) {
  return `[${escapeCell(repo.nameWithOwner)}](${repo.url})`;
}

function pushedDate(repo) {
  return repo.pushedAt ? repo.pushedAt.slice(0, 10) : '';
}

function usersTable(users) {
  return [
    '| Repository | Made by | Description | Last push | Stars |',
    '|------------|---------|-------------|-----------|-------|',
    ...users.map(({ repo, how }) =>
      `| ${nameCell(repo)} | ${how} | ${escapeCell(repo.description)} | ${pushedDate(repo)} | ${repo.stargazerCount} |`),
    '',
  ];
}

function forksTable(forks) {
  return [
    '| Repository | Description | Last push | Stars |',
    '|------------|-------------|-----------|-------|',
    ...forks.map((repo) =>
      `| ${nameCell(repo)} | ${escapeCell(repo.description)} | ${pushedDate(repo)} | ${repo.stargazerCount} |`),
    '',
  ];
}

function renderPage({ users, forks, forksTotal, date }) {
  const lines = [
    '# Users of the sparql-examples template',
    '',
    `The repositories below publish their own SPARQL examples, made from the [sparql-examples-template](https://github.com/${TEMPLATE}). ` +
      'They either forked the template, used it with "Use this template", or were added by hand.',
    '',
    `Generated on ${date} from the GitHub API. Repositories are listed by most recent push.`,
    '',
  ];
  if (users.length === 0) {
    lines.push('No repositories made from the template were found.', '');
  } else {
    lines.push(...usersTable(users));
  }

  lines.push(
    '## Forks of sparql-examples',
    '',
    'Forking is not the recommended way to reuse these examples. To publish your own examples, ' +
      `start from the [sparql-examples-template](https://github.com/${TEMPLATE}) instead. ` +
      'The forks below are listed for reference only.',
    '',
    `GitHub counts ${forksTotal} forks of [sparql-examples](https://github.com/${EXAMPLES}).`,
    '',
  );
  if (forks.length === 0) {
    lines.push('No forks were found.', '');
  } else {
    lines.push(...forksTable(forks));
  }
  return lines.join('\n');
}

async function main() {
  // Repositories made from the template, keyed by name so each is listed once.
  // The first source that finds a repository gives its "Made by" value.
  const users = new Map();
  const add = (repo, how) => {
    if (repo.nameWithOwner !== TEMPLATE && !users.has(repo.nameWithOwner)) users.set(repo.nameWithOwner, { repo, how });
  };

  for (const repo of (await templateForks()).nodes) add(repo, 'Fork');

  const matches = new Map();
  for (const query of NAME_QUERIES) {
    for (const repo of (await nameSearch(query)).nodes) matches.set(repo.nameWithOwner, repo);
  }
  console.log(`Name searches matched ${matches.size} repositories.`);
  for (const repo of matches.values()) {
    if (repo.templateRepository?.nameWithOwner === TEMPLATE) add(repo, 'Use this template');
  }

  for (const name of extraRepositories()) {
    if (users.has(name) || name === TEMPLATE) continue;
    const repo = await repository(name);
    if (repo) add(repo, 'Listed by hand');
    else console.warn(`Not found, skipped: ${name}`);
  }

  const forks = await examplesForks();
  const sortedUsers = [...users.values()].sort((a, b) => (b.repo.pushedAt ?? '').localeCompare(a.repo.pushedAt ?? ''));
  const sortedForks = forks.nodes.sort((a, b) => (b.pushedAt ?? '').localeCompare(a.pushedAt ?? ''));
  const date = new Date().toISOString().slice(0, 10);

  mkdirSync(path.dirname(OUT), { recursive: true });
  writeFileSync(OUT, renderPage({ users: sortedUsers, forks: sortedForks, forksTotal: forks.totalCount, date }));
  console.log(`Wrote ${sortedUsers.length} template users and ${sortedForks.length} forks to ${path.relative(ROOT, OUT)}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
