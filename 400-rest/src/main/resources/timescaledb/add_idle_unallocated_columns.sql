BEGIN;
ALTER TABLE BILLING_DATA ADD COLUMN ACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN UNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUUNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYUNALLOCATEDCOST DOUBLE PRECISION;
COMMIT;