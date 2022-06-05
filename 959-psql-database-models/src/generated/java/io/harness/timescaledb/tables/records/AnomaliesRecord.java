/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.Anomalies;

import java.time.OffsetDateTime;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class AnomaliesRecord extends TableRecordImpl<AnomaliesRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.anomalies.id</code>.
   */
  public AnomaliesRecord setId(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.id</code>.
   */
  public String getId() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.anomalies.accountid</code>.
   */
  public AnomaliesRecord setAccountid(String value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.anomalies.actualcost</code>.
   */
  public AnomaliesRecord setActualcost(Double value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.actualcost</code>.
   */
  public Double getActualcost() {
    return (Double) get(2);
  }

  /**
   * Setter for <code>public.anomalies.expectedcost</code>.
   */
  public AnomaliesRecord setExpectedcost(Double value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.expectedcost</code>.
   */
  public Double getExpectedcost() {
    return (Double) get(3);
  }

  /**
   * Setter for <code>public.anomalies.anomalytime</code>.
   */
  public AnomaliesRecord setAnomalytime(OffsetDateTime value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.anomalytime</code>.
   */
  public OffsetDateTime getAnomalytime() {
    return (OffsetDateTime) get(4);
  }

  /**
   * Setter for <code>public.anomalies.timegranularity</code>.
   */
  public AnomaliesRecord setTimegranularity(String value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.timegranularity</code>.
   */
  public String getTimegranularity() {
    return (String) get(5);
  }

  /**
   * Setter for <code>public.anomalies.note</code>.
   */
  public AnomaliesRecord setNote(String value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.note</code>.
   */
  public String getNote() {
    return (String) get(6);
  }

  /**
   * Setter for <code>public.anomalies.clusterid</code>.
   */
  public AnomaliesRecord setClusterid(String value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.clusterid</code>.
   */
  public String getClusterid() {
    return (String) get(7);
  }

  /**
   * Setter for <code>public.anomalies.clustername</code>.
   */
  public AnomaliesRecord setClustername(String value) {
    set(8, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.clustername</code>.
   */
  public String getClustername() {
    return (String) get(8);
  }

  /**
   * Setter for <code>public.anomalies.workloadname</code>.
   */
  public AnomaliesRecord setWorkloadname(String value) {
    set(9, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.workloadname</code>.
   */
  public String getWorkloadname() {
    return (String) get(9);
  }

  /**
   * Setter for <code>public.anomalies.workloadtype</code>.
   */
  public AnomaliesRecord setWorkloadtype(String value) {
    set(10, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.workloadtype</code>.
   */
  public String getWorkloadtype() {
    return (String) get(10);
  }

  /**
   * Setter for <code>public.anomalies.namespace</code>.
   */
  public AnomaliesRecord setNamespace(String value) {
    set(11, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.namespace</code>.
   */
  public String getNamespace() {
    return (String) get(11);
  }

  /**
   * Setter for <code>public.anomalies.region</code>.
   */
  public AnomaliesRecord setRegion(String value) {
    set(12, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.region</code>.
   */
  public String getRegion() {
    return (String) get(12);
  }

  /**
   * Setter for <code>public.anomalies.gcpproduct</code>.
   */
  public AnomaliesRecord setGcpproduct(String value) {
    set(13, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.gcpproduct</code>.
   */
  public String getGcpproduct() {
    return (String) get(13);
  }

  /**
   * Setter for <code>public.anomalies.gcpskuid</code>.
   */
  public AnomaliesRecord setGcpskuid(String value) {
    set(14, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.gcpskuid</code>.
   */
  public String getGcpskuid() {
    return (String) get(14);
  }

  /**
   * Setter for <code>public.anomalies.gcpskudescription</code>.
   */
  public AnomaliesRecord setGcpskudescription(String value) {
    set(15, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.gcpskudescription</code>.
   */
  public String getGcpskudescription() {
    return (String) get(15);
  }

  /**
   * Setter for <code>public.anomalies.gcpproject</code>.
   */
  public AnomaliesRecord setGcpproject(String value) {
    set(16, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.gcpproject</code>.
   */
  public String getGcpproject() {
    return (String) get(16);
  }

  /**
   * Setter for <code>public.anomalies.awsservice</code>.
   */
  public AnomaliesRecord setAwsservice(String value) {
    set(17, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.awsservice</code>.
   */
  public String getAwsservice() {
    return (String) get(17);
  }

  /**
   * Setter for <code>public.anomalies.awsaccount</code>.
   */
  public AnomaliesRecord setAwsaccount(String value) {
    set(18, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.awsaccount</code>.
   */
  public String getAwsaccount() {
    return (String) get(18);
  }

  /**
   * Setter for <code>public.anomalies.awsinstancetype</code>.
   */
  public AnomaliesRecord setAwsinstancetype(String value) {
    set(19, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.awsinstancetype</code>.
   */
  public String getAwsinstancetype() {
    return (String) get(19);
  }

  /**
   * Setter for <code>public.anomalies.awsusagetype</code>.
   */
  public AnomaliesRecord setAwsusagetype(String value) {
    set(20, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.awsusagetype</code>.
   */
  public String getAwsusagetype() {
    return (String) get(20);
  }

  /**
   * Setter for <code>public.anomalies.anomalyscore</code>.
   */
  public AnomaliesRecord setAnomalyscore(Double value) {
    set(21, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.anomalyscore</code>.
   */
  public Double getAnomalyscore() {
    return (Double) get(21);
  }

  /**
   * Setter for <code>public.anomalies.reportedby</code>.
   */
  public AnomaliesRecord setReportedby(String value) {
    set(22, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.reportedby</code>.
   */
  public String getReportedby() {
    return (String) get(22);
  }

  /**
   * Setter for <code>public.anomalies.feedback</code>.
   */
  public AnomaliesRecord setFeedback(String value) {
    set(23, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.feedback</code>.
   */
  public String getFeedback() {
    return (String) get(23);
  }

  /**
   * Setter for <code>public.anomalies.slackdailynotification</code>.
   */
  public AnomaliesRecord setSlackdailynotification(Boolean value) {
    set(24, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.slackdailynotification</code>.
   */
  public Boolean getSlackdailynotification() {
    return (Boolean) get(24);
  }

  /**
   * Setter for <code>public.anomalies.slackinstantnotification</code>.
   */
  public AnomaliesRecord setSlackinstantnotification(Boolean value) {
    set(25, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.slackinstantnotification</code>.
   */
  public Boolean getSlackinstantnotification() {
    return (Boolean) get(25);
  }

  /**
   * Setter for <code>public.anomalies.slackweeklynotification</code>.
   */
  public AnomaliesRecord setSlackweeklynotification(Boolean value) {
    set(26, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.slackweeklynotification</code>.
   */
  public Boolean getSlackweeklynotification() {
    return (Boolean) get(26);
  }

  /**
   * Setter for <code>public.anomalies.newentity</code>.
   */
  public AnomaliesRecord setNewentity(Boolean value) {
    set(27, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.newentity</code>.
   */
  public Boolean getNewentity() {
    return (Boolean) get(27);
  }

  /**
   * Setter for <code>public.anomalies.azuresubscriptionguid</code>.
   */
  public AnomaliesRecord setAzuresubscriptionguid(String value) {
    set(28, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.azuresubscriptionguid</code>.
   */
  public String getAzuresubscriptionguid() {
    return (String) get(28);
  }

  /**
   * Setter for <code>public.anomalies.azureresourcegroup</code>.
   */
  public AnomaliesRecord setAzureresourcegroup(String value) {
    set(29, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.azureresourcegroup</code>.
   */
  public String getAzureresourcegroup() {
    return (String) get(29);
  }

  /**
   * Setter for <code>public.anomalies.azuremetercategory</code>.
   */
  public AnomaliesRecord setAzuremetercategory(String value) {
    set(30, value);
    return this;
  }

  /**
   * Getter for <code>public.anomalies.azuremetercategory</code>.
   */
  public String getAzuremetercategory() {
    return (String) get(30);
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached AnomaliesRecord
   */
  public AnomaliesRecord() {
    super(Anomalies.ANOMALIES);
  }

  /**
   * Create a detached, initialised AnomaliesRecord
   */
  public AnomaliesRecord(String id, String accountid, Double actualcost, Double expectedcost,
      OffsetDateTime anomalytime, String timegranularity, String note, String clusterid, String clustername,
      String workloadname, String workloadtype, String namespace, String region, String gcpproduct, String gcpskuid,
      String gcpskudescription, String gcpproject, String awsservice, String awsaccount, String awsinstancetype,
      String awsusagetype, Double anomalyscore, String reportedby, String feedback, Boolean slackdailynotification,
      Boolean slackinstantnotification, Boolean slackweeklynotification, Boolean newentity,
      String azuresubscriptionguid, String azureresourcegroup, String azuremetercategory) {
    super(Anomalies.ANOMALIES);

    setId(id);
    setAccountid(accountid);
    setActualcost(actualcost);
    setExpectedcost(expectedcost);
    setAnomalytime(anomalytime);
    setTimegranularity(timegranularity);
    setNote(note);
    setClusterid(clusterid);
    setClustername(clustername);
    setWorkloadname(workloadname);
    setWorkloadtype(workloadtype);
    setNamespace(namespace);
    setRegion(region);
    setGcpproduct(gcpproduct);
    setGcpskuid(gcpskuid);
    setGcpskudescription(gcpskudescription);
    setGcpproject(gcpproject);
    setAwsservice(awsservice);
    setAwsaccount(awsaccount);
    setAwsinstancetype(awsinstancetype);
    setAwsusagetype(awsusagetype);
    setAnomalyscore(anomalyscore);
    setReportedby(reportedby);
    setFeedback(feedback);
    setSlackdailynotification(slackdailynotification);
    setSlackinstantnotification(slackinstantnotification);
    setSlackweeklynotification(slackweeklynotification);
    setNewentity(newentity);
    setAzuresubscriptionguid(azuresubscriptionguid);
    setAzureresourcegroup(azureresourcegroup);
    setAzuremetercategory(azuremetercategory);
  }
}
