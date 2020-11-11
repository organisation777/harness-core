---------- ANOMALIES TABLE START ------------
BEGIN;
DROP TABLE ANOMALIES;
COMMIT;

BEGIN;
CREATE TABLE IF NOT EXISTS ANOMALIES (
  ID TEXT NOT NULL,
  ACCOUNTID TEXT NOT NULL,
  ACTUALCOST DOUBLE PRECISION NOT NULL,
  EXPECTEDCOST DOUBLE PRECISION NOT NULL,
  ANOMALYTIME TIMESTAMPTZ NOT NULL,
  TIMEGRANULARITY TEXT NOT NULL,
  NOTE TEXT,
  CLUSTERID TEXT,
  CLUSTERNAME TEXT,
  WORKLOADNAME TEXT,
  WORKLOADTYPE TEXT,
  NAMESPACE TEXT,
  REGION TEXT,
  GCPPRODUCT TEXT,
  GCPSKUID TEXT,
  GCPSKUDESCRIPTION TEXT,
  GCPPROJECT TEXT,
  AWSSERVICE TEXT,
  AWSACCOUNT TEXT,
  AWSINSTANCETYPE TEXT,
  AWSUSAGETYPE TEXT,
  ANOMALYSCORE DOUBLE PRECISION,
  REPORTEDBY TEXT
);
COMMIT;
SELECT CREATE_HYPERTABLE('ANOMALIES','anomalytime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS ANOMALY_ACCOUNTID_INDEX ON ANOMALIES(ACCOUNTID ,ANOMALYTIME DESC);
COMMIT;

---------- ANOMALIES TABLE END ------------