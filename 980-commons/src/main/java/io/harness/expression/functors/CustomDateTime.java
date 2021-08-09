package io.harness.expression.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.utils.DateTimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@OwnedBy(HarnessTeam.CDC)
public class CustomDateTime {
  private ZonedDateTime dateTime;

  public CustomDateTime() {
    this.dateTime = ZonedDateTime.now(ZoneId.of(DateTimeUtils.UTC_TIMEZONE));
  }

  public CustomDateTime plusYears(long years) {
    dateTime = dateTime.plusYears(years);
    return this;
  }

  public CustomDateTime plusMonths(long months) {
    dateTime = dateTime.plusMonths(months);
    return this;
  }

  public CustomDateTime plusWeeks(long weeks) {
    dateTime = dateTime.plusWeeks(weeks);
    return this;
  }

  public CustomDateTime plusDays(long days) {
    dateTime = dateTime.plusDays(days);
    return this;
  }

  public CustomDateTime plusHours(long hours) {
    dateTime = dateTime.plusHours(hours);
    return this;
  }

  public CustomDateTime plusMinutes(long minutes) {
    dateTime = dateTime.plusMinutes(minutes);
    return this;
  }

  public CustomDateTime plusSeconds(long seconds) {
    dateTime = dateTime.plusSeconds(seconds);
    return this;
  }

  public CustomDateTime plusNanos(long nanos) {
    dateTime = dateTime.plusNanos(nanos);
    return this;
  }

  @Override
  public String toString() {
    return DateTimeUtils.formatDateTime(dateTime);
  }
}
