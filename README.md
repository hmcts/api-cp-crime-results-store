# api-cp-crime-results-store

The OpenAPI contract for the **Results Store read API**, and the Java interfaces and models generated
from it.

The Results Store (`service-cp-crime-results-store`) keeps every share of every resulted hearing day as
a versioned, queryable record. This repository holds the contract for reading it. The service implements
the generated `SharesApi`; teams that read from the store (YOT results distribution, probation results
distribution, court register, support staff) can use the same spec to build their clients.

This is an internal API. It is not published to the API Marketplace.

## What the API offers

Every route is `GET` only, under `/results-store/v1`. Send `CJSCPPUID` on every request; the caller must
be in "System Users" or "Second Line Support".

| Operation (`SharesApi`) | Method and path | Purpose |
|---|---|---|
| `pullOrSearchShares` | `GET /results-store/v1/shares` | Pull with `storedAfterSeq` (a `PullPage`, in stored order), or search one court centre over London days or a `sharedTime` range (a `SearchPage`) |
| `getShare` | `GET /results-store/v1/shares/{shareId}` | One share's current key details, chain and youth facts (`ShareSummary`) |
| `getSharePayload` | `GET /results-store/v1/shares/{shareId}/payload` | The payload the store holds for one share, without `_metadata`, with a strong `ETag` and the `Results-Store-*` headers; `If-None-Match` gives a 304 |
| `listHearingDayShares` | `GET /results-store/v1/hearings/{hearingId}/days/{hearingDay}/shares` | Every version of one hearing day, in `sharedTime` order (`DayVersions`) |

Every 4xx and 5xx body is a `ProblemDetail` with a fixed `reason`. Every field of every item is always
written, `null` when missing. The spec is
[`src/main/resources/openapi/openapi-spec.yml`](src/main/resources/openapi/openapi-spec.yml); the full
behaviour (cursors, visibility lag, error reasons) is in the description of each operation.

## What the jar contains

- `uk.gov.hmcts.cp.resultsstore.openapi.api.SharesApi`: the Spring interface (`interfaceOnly`)
- `uk.gov.hmcts.cp.resultsstore.openapi.model.*`: the models, with Lombok builders. `date-time` maps to
  `java.time.Instant`
- `openapi/openapi-spec.yml`, and the same file again at the jar root as `results-store-openapi.yaml`
  (the service's audit filter finds its spec by that unique name)
- `META-INF/CHANGELOG.md` and the SBOM at `META-INF/sbom/bom.json`

The generated code carries no bean validation: request validation stays in the service. The jar brings
only annotation libraries at runtime (`swagger-annotations`, `jackson-annotations`,
`jakarta.annotation-api`, `jakarta.validation-api`). Spring Web and the servlet API are compile-time
only here: the consuming Spring Boot service supplies them.

## Using it

Artefacts are published to Azure Artifacts (`hmcts-lib`) and GitHub Packages as
`uk.gov.hmcts.cp:api-cp-crime-results-store`.

```groovy
repositories {
  maven { url = 'https://pkgs.dev.azure.com/hmcts/Artifacts/_packaging/hmcts-lib/maven/v1' }
}

configurations {
  apiSpec
  implementation.extendsFrom apiSpec
}

dependencies {
  apiSpec "uk.gov.hmcts.cp:api-cp-crime-results-store:X.Y.Z"
}
```

### Versions

| Build | Trigger | Version |
|---|---|---|
| Draft | push to `team/<name>` | `<name>-<short-sha>`, for example `rs-1a2b3c4` |
| Draft | push to `main` | `<projectVersion>-<short-sha>` (`projectVersion` is in `gradle.properties`) |
| Release | a published GitHub Release `vX.Y.Z` | `X.Y.Z` |

CI writes the artefact version into `info.version` of the published spec, so the version in the file
on a branch does not matter.

A service may build against a draft while a change is in review, but its release build must pin a fixed
`X.Y.Z` (the service's `validateApiSpecVersions` task refuses anything else).

## Changing the contract

1. Branch from `main`, change the spec (and `OpenApiObjectsTest` if operations or models change), and
   add an entry under *Unreleased* in [`CHANGELOG.md`](CHANGELOG.md).
2. Push the branch to `team/<name>` as well. CI publishes a draft `<name>-<short-sha>`.
3. In the service, pin the draft in `apiSpec` and build the implementation against it.
4. Open a pull request here. It needs one approval and the checks to pass.
5. Merge to `main`, then publish a GitHub Release `vX.Y.Z` (move the *Unreleased* entries under the new
   version first). CI publishes `X.Y.Z`.
6. In the service, replace the draft with `X.Y.Z`.

Follow semantic versioning: removing or renaming anything a caller reads is a major change.

## Building locally

Java 25 is required.

```bash
./gradlew build -DAPI_SPEC_VERSION=0.0.999
```

This generates the sources, compiles them, runs the tests and writes the SBOM. `./gradlew pmdMain` runs
PMD on hand-written main code (there is none today; generated code is excluded). Lint the spec with
Spectral:

```bash
npx -y @stoplight/spectral-cli lint src/main/resources/openapi/openapi-spec.yml --ruleset .spectral.yml
```

## Ownership

Owned by [@hmcts/results-validation-service-team](https://github.com/orgs/hmcts/teams/results-validation-service-team).
See [CONTRIBUTING.md](.github/CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).

## License

This project is licensed under the [MIT License](LICENSE).
