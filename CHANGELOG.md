# Changelog

All notable changes to the Results Store API contract are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow semantic versioning.

## [Unreleased]

### Added
- First contract for the Results Store read API (`SharesApi`), generated into
  `uk.gov.hmcts.cp.resultsstore.openapi.api` and `uk.gov.hmcts.cp.resultsstore.openapi.model`:
  - `GET /results-store/v1/shares` (`pullOrSearchShares`): pull by `storedAfterSeq` (`PullPage`) or
    search by court centre and day or time range (`SearchPage`)
  - `GET /results-store/v1/shares/{shareId}` (`getShare`): one share's current details (`ShareSummary`)
  - `GET /results-store/v1/shares/{shareId}/payload` (`getSharePayload`): the stored payload without
    `_metadata`, with a strong ETag, `If-None-Match` / 304 and the `Results-Store-*` headers
  - `GET /results-store/v1/hearings/{hearingId}/days/{hearingDay}/shares` (`listHearingDayShares`): every
    version of one hearing day (`DayVersions`)
  - `ProblemDetail` for every 4xx and 5xx response, with the fixed `reason` list
- `GET /results-store/v1/shares/{shareId}/payload/arrived` (`getShareArrivedPayload`, returns
  `ResponseEntity<byte[]>`): the text as it arrived, before enrichment, without `_metadata`, served as exact
  bytes with a strong ETag (the SHA-256 over those bytes), `If-None-Match` / 304 and the same
  `Results-Store-*` headers as the payload operation, with `Results-Store-Payload-Form: arrived-text`
- Every model field is written, `null` when missing (no `@JsonInclude(NON_NULL)`); no bean validation
  annotations are relied on, as request validation stays in the service
- The jar carries the spec once, at `openapi/openapi-spec.yml`

### Changed
- `getSharePayload` now returns `ResponseEntity<byte[]>`: the 200 body is declared `type: string,
  format: binary` (still `application/json`), so the service serves the working copy's exact bytes,
  without `_metadata`, and the ETag is the SHA-256 over exactly those bytes. The generator maps `file` to
  `byte[]`; every other operation is unchanged
