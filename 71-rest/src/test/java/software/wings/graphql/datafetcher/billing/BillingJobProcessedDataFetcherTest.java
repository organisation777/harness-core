package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.BatchJobScheduledDataDao;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.graphql.schema.type.aggregation.billing.QLBatchLastProcessedData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BillingJobProcessedDataFetcherTest extends AbstractDataFetcherTest {
  @Inject @InjectMocks BillingJobProcessedDataFetcher billingJobProcessedDataFetcher;
  @Mock private BatchJobScheduledDataDao batchJobScheduledDataDao;

  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String BATCH_JOB_TYPE = "UNALLOCATED_BILLING_HOURLY";
  private final Instant NOW = Instant.now().truncatedTo(ChronoUnit.DAYS);
  private final Instant LAST_PROCESSED_DATA_START_TIME = NOW.minus(2, ChronoUnit.DAYS);
  private final Instant LAST_PROCESSED_DATA_END_TIME = NOW.minus(1, ChronoUnit.DAYS);

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchLastProcessedTime() {
    when(batchJobScheduledDataDao.fetchLastBatchJobScheduledData(ACCOUNT_ID, BATCH_JOB_TYPE))
        .thenReturn(getBatchJobScheduledData());
    QLBatchLastProcessedData batchLastProcessedData =
        billingJobProcessedDataFetcher.fetch(new QLNoOpQueryParameters(), ACCOUNT_ID);
    assertThat(batchLastProcessedData.getLastProcessedTime()).isEqualTo(LAST_PROCESSED_DATA_END_TIME.toEpochMilli());
  }

  private BatchJobScheduledData getBatchJobScheduledData() {
    return new BatchJobScheduledData(
        ACCOUNT_ID, BATCH_JOB_TYPE, 1200, LAST_PROCESSED_DATA_START_TIME, LAST_PROCESSED_DATA_END_TIME);
  }
}
