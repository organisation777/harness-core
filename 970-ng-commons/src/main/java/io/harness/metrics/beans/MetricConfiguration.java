package io.harness.metrics.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.View;
import io.opencensus.tags.TagKey;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
// TODO: Give a better name
public class MetricConfiguration {
  String name;
  String identifier;
  String metricGroup;
  List<Metric> metrics;

  @Data
  @Builder
  public static class Metric {
    String metricName;
    String metricDefinition;
    String type;
    List<String> distribution;
    String unit;
    Measure measure;
    View view;

    public View getView(List<TagKey> tagKeys) {
      Measure measure = MeasureDouble.create(this.getMetricName(), this.getMetricDefinition(), this.getUnit());
      this.setMeasure(measure);
      if (isEmpty(type)) {
        return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
            Aggregation.LastValue.create(), tagKeys);
      }

      switch (type) {
        case "LastValue":
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.LastValue.create(), tagKeys);
        case "Count":
          measure = MeasureLong.create(this.getMetricName(), this.getMetricDefinition(), this.getUnit());
          this.setMeasure(measure);
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.Count.create(), tagKeys);
        case "Duration":
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.Distribution.create(BucketBoundaries.create(getDistributionMs())), tagKeys);
        case "Distribution":
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.Distribution.create(BucketBoundaries.create(
                  distribution.stream().map(op -> Double.valueOf(op)).collect(Collectors.toList()))),
              tagKeys);
        default:
          throw new IllegalStateException("Unsupported metric type");
      }
    }

    private List<Double> getDistributionMs() {
      return distribution.stream().map(durationString -> getMs(durationString)).collect(Collectors.toList());
    }

    private Double getMs(String distributionStringValue) {
      if (distributionStringValue.endsWith("ms")) {
        return Double.valueOf(
            Duration
                .ofMillis(Long.parseLong(distributionStringValue.substring(0, distributionStringValue.length() - 2)))
                .toMillis());
      }
      char last = distributionStringValue.charAt(distributionStringValue.length() - 1);
      long parsedDuration = Long.parseLong(distributionStringValue.substring(0, distributionStringValue.length() - 1));
      switch (last) {
        case 'm':
          return Double.valueOf(Duration.ofMinutes(parsedDuration).toMillis());
        case 's':
          return Double.valueOf(Duration.ofSeconds(parsedDuration).toMillis());
        default:
          throw new IllegalStateException(
              "Char: " + last + " can not be parsed to a valid duration. Please use 'm', 's' or 'ms' ");
      }
    }
  }
}