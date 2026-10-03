package uk.gov.hmcts.cp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.resultsstore.openapi.api.SharesApi;
import uk.gov.hmcts.cp.resultsstore.openapi.model.DayVersions;
import uk.gov.hmcts.cp.resultsstore.openapi.model.KeyDetails;
import uk.gov.hmcts.cp.resultsstore.openapi.model.ProblemDetail;
import uk.gov.hmcts.cp.resultsstore.openapi.model.PullOrSearchShares200Response;
import uk.gov.hmcts.cp.resultsstore.openapi.model.PullPage;
import uk.gov.hmcts.cp.resultsstore.openapi.model.SearchPage;
import uk.gov.hmcts.cp.resultsstore.openapi.model.ShareSummary;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiObjectsTest {

    private static final Path SPEC = Path.of("src/main/resources/openapi/openapi-spec.yml");
    private static final Pattern OPERATION_ID = Pattern.compile("^\\s*operationId:\\s*(\\w+)\\s*$", Pattern.MULTILINE);

    private static List<String> operationIdsInSpec() throws IOException {
        Matcher matcher = OPERATION_ID.matcher(Files.readString(SPEC));
        return matcher.results().map(result -> result.group(1)).toList();
    }

    private static Method method(String name) {
        return Arrays.stream(SharesApi.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static Class<?> responseBodyType(String operation) {
        ParameterizedType returnType = (ParameterizedType) method(operation).getGenericReturnType();
        assertThat(returnType.getRawType()).isEqualTo(ResponseEntity.class);
        Object body = returnType.getActualTypeArguments()[0];
        return body instanceof ParameterizedType parameterized ? (Class<?>) parameterized.getRawType() : (Class<?>) body;
    }

    @Test
    void spec_should_declare_the_four_read_operations() throws IOException {
        assertThat(operationIdsInSpec()).containsExactlyInAnyOrder(
                "pullOrSearchShares", "getShare", "getSharePayload", "listHearingDayShares");
    }

    @Test
    void generated_shares_api_should_have_a_method_for_every_operation_in_the_spec() throws IOException {
        assertThat(SharesApi.class).hasDeclaredMethods(operationIdsInSpec().toArray(String[]::new));
    }

    @Test
    void generated_shares_api_should_return_the_contract_types() {
        assertThat(responseBodyType("pullOrSearchShares")).isEqualTo(PullOrSearchShares200Response.class);
        assertThat(responseBodyType("getShare")).isEqualTo(ShareSummary.class);
        assertThat(responseBodyType("getSharePayload")).isEqualTo(java.util.Map.class);
        assertThat(responseBodyType("listHearingDayShares")).isEqualTo(DayVersions.class);
    }

    @Test
    void generated_get_share_payload_should_take_share_id_and_if_none_match() {
        assertThat(method("getSharePayload").getParameterTypes()).containsExactly(UUID.class, String.class);
    }

    @Test
    void generated_pull_and_search_pages_should_both_answer_the_shares_operation() {
        assertThat(PullOrSearchShares200Response.class).isAssignableFrom(PullPage.class);
        assertThat(PullOrSearchShares200Response.class).isAssignableFrom(SearchPage.class);
    }

    @Test
    void generated_share_summary_should_have_expected_fields() {
        assertThat(ShareSummary.class).hasDeclaredFields(
                "shareId", "hearingId", "hearingDay", "sharedTime", "storedSeq", "storedAt", "sharedDayLondon",
                "sharedDayUtc", "keyDetails", "anySubjectIsYouth", "dayYouthSeen", "isLatest", "predecessorShareId",
                "arrivedOutOfOrder", "enrichmentApplied", "projectionStatus", "projectionVersion", "projectedAt",
                "versionNumber");
    }

    @Test
    void generated_share_summary_should_map_instants_days_and_ids() throws Exception {
        assertThat(ShareSummary.class.getDeclaredField("sharedTime").getType()).isEqualTo(Instant.class);
        assertThat(ShareSummary.class.getDeclaredField("storedAt").getType()).isEqualTo(Instant.class);
        assertThat(ShareSummary.class.getDeclaredField("projectedAt").getType()).isEqualTo(Instant.class);
        assertThat(ShareSummary.class.getDeclaredField("hearingDay").getType()).isEqualTo(LocalDate.class);
        assertThat(ShareSummary.class.getDeclaredField("storedSeq").getType()).isEqualTo(Long.class);
        assertThat(ShareSummary.class.getDeclaredField("shareId").getType()).isEqualTo(UUID.class);
    }

    @Test
    void generated_key_details_should_have_expected_fields() {
        assertThat(KeyDetails.class).hasDeclaredFields(
                "courtCentreId", "courtRoomId", "ljaCode", "jurisdictionType", "isSjp", "isGroupProceedings",
                "youthCourtId", "isReshare");
    }

    @Test
    void generated_pull_page_should_have_expected_fields() throws Exception {
        assertThat(PullPage.class).hasDeclaredFields("items", "nextStoredAfterSeq", "hasMore", "visibleUpTo");
        assertThat(PullPage.class.getDeclaredField("nextStoredAfterSeq").getType()).isEqualTo(Long.class);
        assertThat(PullPage.class.getDeclaredField("visibleUpTo").getType()).isEqualTo(Instant.class);
    }

    @Test
    void generated_search_page_should_have_expected_fields() {
        assertThat(SearchPage.class).hasDeclaredFields("items", "nextCursor");
    }

    @Test
    void generated_day_versions_should_have_expected_fields() {
        assertThat(DayVersions.class).hasDeclaredFields("items");
    }

    @Test
    void generated_problem_detail_should_have_expected_fields() {
        assertThat(ProblemDetail.class).hasDeclaredFields("type", "title", "status", "reason");
    }

    @Test
    void generated_models_should_write_null_fields() {
        // The contract writes every field, null when missing: no model may drop nulls
        List.of(ShareSummary.class, KeyDetails.class, PullPage.class, SearchPage.class, DayVersions.class,
                        ProblemDetail.class)
                .forEach(model -> assertThat(model.getAnnotation(JsonInclude.class)).as(model.getSimpleName()).isNull());
    }
}
