drop table IF EXISTS "DummyEntity";
drop table IF EXISTS "OutboxEntity";

create TABLE "DummyEntity" (
     "id"             SERIAL PRIMARY KEY,
     "hello"          VARCHAR(40),
     "someOtherStuff" VARCHAR(40),
     "tenantId"       VARCHAR(20),
     "isTestData"     VARCHAR(10),
     "createdAt"      TIMESTAMP,
     "lastModifiedAt" TIMESTAMP,
     "version"        INT,
     "deletedAt"      TIMESTAMP,
     "createdBy"      VARCHAR(40)
);

create TABLE "OutboxEntity" (
     "id"                    SERIAL PRIMARY KEY,
     "aggregateId"           VARCHAR(40),
     "aggregateType"         VARCHAR(40),
     "artifactId"         VARCHAR(40),
      "groupId"         VARCHAR(40),
     "payload"               BYTEA,
     "eventType"             VARCHAR(40),
     "additionalFieldValues" JSON,
     "expireAt"              TIMESTAMP,
     "tenantId"              VARCHAR(20),
     "isTestData"            VARCHAR(10),
     "createdAt"             TIMESTAMP,
     "lastModifiedAt"        TIMESTAMP,
     "eventOccurredAt"       TIMESTAMP,
     "version"               INT,
     "deletedAt"             TIMESTAMP, 
     "createdBy"             VARCHAR(40)
);
insert into "DummyEntity" ("id", "hello", "someOtherStuff", "tenantId", "isTestData", "createdAt", "lastModifiedAt", "version", "deletedAt", "createdBy")
values
 (010000, 'testGetFound', 'prop1-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (020000, 'testPatchInUpsert', 'prop2-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (030000, 'testGetNotFoundByTenant', 'prop3-2', 'framework-testing2', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (040000, 'testGetByIdsFound1', 'prop4-2', 'framework-testing', 'true', '2022-01-01', '2022-02-01', 1, null, 'no-one'),
 (050000, 'testGetByIdsFound2', 'prop5-2', 'framework-testing', 'true', '2022-01-01', '2022-03-01', 1, null, 'no-one'),
 (060000, 'testGetByIdsFound3', 'prop6-2', 'framework-testing', 'true', '2022-01-01', '2022-04-01', 1, null, 'no-one'),
 (070000, 'testGetByIdsPartialNotFoundByTenant1', 'prop7-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (080000, 'testGetByIdsPartialNotFoundByTenant2', 'prop8-2', 'framework-testing3', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (090000, 'testGetByIdsPartialNotFoundInDb1', 'prop9-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (100000, 'testGetByIdsSorted1', 'prop10-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (110000, 'testGetByIdsSorted3', 'prop11-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (120000, 'testGetByIdsSorted2', 'prop12-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (130000, 'testPatchFound', 'prop13-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (140000, 'testPutFound', 'prop14-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (150000, 'testDelete', 'prop15-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (160000, 'testDeleteWhenAlreadyDeleted', 'prop16-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, '2022-01-02', 'no-one'),
 (170000, 'testDeleteWithDifferentTenant', 'prop17-2', 'framework-testing2', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (180000, 'testBulkPatchFound', 'prop18-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (190000, 'testBulkPatchFound', 'prop19-2', 'framework-testing', 'true', '2022-01-01', '2022-01-01', 1, null, 'no-one'),
 (210000, 'testGetByLastModifiesLessThan', 'prop20-2', 'framework-testing', 'true', '2021-01-01', '2021-01-01', 1, null, 'no-one');
