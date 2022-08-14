package com.services.sf.test;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.services.common.domain.util.LocalContext;
import com.services.sf.domain.base.outbox.OutboxSerializer;
import com.services.sf.domain.basesql.outbox.OutboxEntity;
import com.services.sf.domain.basesql.outbox.OutboxRepository;
import com.services.sf.test.dummy.Dummy;
import com.services.sf.test.dummy.DummyService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("UnstableApiUsage")
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(PostgresResource.class)
public class TestSqlDummy {

    private static final Duration maxDurationToWait = Duration.ofMinutes(5);
    private static final String tenantIdToUse = "framework-testing";
    private static final List<Dummy> dataForSortCheck = List.of(
            Dummy.builder().id("100000").hello("testGetByIdsSorted1").someOtherStuff("prop10-2").version(1L).build(),
            Dummy.builder().id("110000").hello("testGetByIdsSorted3").someOtherStuff("prop11-2").version(1L).build(),
            Dummy.builder().id("120000").hello("testGetByIdsSorted2").someOtherStuff("prop12-2").version(1L).build()
    );
    @Inject
    protected LocalContext localContext;
    @Inject
    protected DummyService service;
    @Inject
    protected OutboxRepository outboxRepository;

    @InjectMock
    OutboxSerializer serializer;
    String userId = RandomStringUtils.randomAlphanumeric(10);

    @BeforeEach
    public void setup() {
        setLocalContext();
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        Mockito.when(serializer.serialize(Mockito.anyString(), Mockito.any())).thenReturn(byteBuffer.array());

    }

    @AfterAll
    public void destroy() {
    }

    private void setLocalContext() {
        localContext.setTenantId(tenantIdToUse);
        localContext.setUserId(userId);
    }

    @Test
    public void testCreate() {
        Dummy dummy = Dummy.builder().hello("testCreate").someOtherStuff("otherStuff").build();
        Instant beforeCreate = Instant.now();
        Dummy createdDummy = service.create(dummy).await().atMost(maxDurationToWait);
        var afterCreate = Instant.now();
        assertExpectedDummySameAsCreatedDummy(dummy, createdDummy, beforeCreate, afterCreate);

        // get and see if it exists
        var beforeGet = Instant.now();
        var foundDummy = service.get(createdDummy.getId()).await().atMost(maxDurationToWait);
        var afterGet = Instant.now();
        dummy.setId(createdDummy.getId());
        dummy.setVersion(1L);
        assertExpectedDummySameAsFetchedDummy(dummy, foundDummy, beforeGet, afterGet);
    }

    @Test
    public void testCreateInUpsert() {
        var dummy = Dummy.builder().hello("testCreateInUpsert").someOtherStuff("testCreateInUpsert-SomeOther").build();
        var beforeUpsert = Instant.now();
        var createdDummy = service.create(dummy).await().atMost(maxDurationToWait);
        var afterUpsert = Instant.now();
        assertExpectedDummySameAsCreatedDummy(dummy, createdDummy, beforeUpsert, afterUpsert);

        // get and see if it exists
        var beforeGet = Instant.now();
        var foundDummy = service.get(createdDummy.getId()).await().atMost(maxDurationToWait);
        var afterGet = Instant.now();
        dummy.setId(createdDummy.getId());
        dummy.setVersion(1L);
        assertExpectedDummySameAsFetchedDummy(dummy, foundDummy, beforeGet, afterGet);
    }

    private void assertExpectedDummySameAsCreatedDummy(Dummy expected, Dummy createdDummy, Instant beforeCreate, Instant afterCreate) {
        assertThat("Property 'Hello' is not same", Objects.equals(expected.getHello(), createdDummy.getHello()));
        assertThat("Property 'someOtherStuff' is not same", Objects.equals(expected.getSomeOtherStuff(), createdDummy.getSomeOtherStuff()));
        assertThat("CreateTime not in range", beforeCreate.isBefore(createdDummy.getCreatedAt()) && afterCreate.isAfter(createdDummy.getCreatedAt()));
        assertThat("Created ID is null", Objects.nonNull(createdDummy.getId()));
        assertThat("Tenant Id is not same", tenantIdToUse.equals(createdDummy.getTenantId()));
    }

    private void assertExpectedDummySameAsFetchedDummy(Dummy expected, Dummy foundDummy, Instant beforeFetch, Instant afterFetch) {
        assertThat("Property 'Hello' is not same: %s == %s".formatted(expected.getHello(), foundDummy.getHello()), expected.getHello().equals(foundDummy.getHello()));
        assertThat("Property 'someOtherStuff' is not same", Objects.equals(expected.getSomeOtherStuff(), foundDummy.getSomeOtherStuff()));
        assertThat("Created ID is not same", Objects.equals(expected.getId(), foundDummy.getId()));
        assertThat("Tenant Id is not same", tenantIdToUse.equals(foundDummy.getTenantId()));
        assertThat("Version is not Same", Objects.equals(foundDummy.getVersion(), expected.getVersion()));
    }

    private void assertExpectedDummySameAsPatchedDummy(Dummy expected, Dummy patchedDummy, Instant beforePatch, Instant afterPatch) {
        assertExpectedDummySameAsFetchedDummy(expected, patchedDummy, beforePatch, afterPatch);
        assertThat("UpdatedAt not in range", beforePatch.isBefore(patchedDummy.getLastModifiedAt()) && afterPatch.isAfter(patchedDummy.getLastModifiedAt()));

    }

    @Test
    public void testPatchInUpsert() {
        var dummy = Dummy.builder().id("20000").hello("updatedValue").build();
        var startTime = Instant.now();
        var patchedDummy = service.upsert(dummy, dummy).await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var expectedDummy = Dummy.builder().id("20000").hello("updatedValue")
                .someOtherStuff("prop2-2")
                .version(2L).build();
        assertExpectedDummySameAsPatchedDummy(expectedDummy, patchedDummy, startTime, endTime);
    }

    @Test
    public void testGetFound() {
        var startTime = Instant.now();
        var foundDummy = service.get("10000").await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var inputDummy = Dummy.builder().id("10000").hello("testGetFound").someOtherStuff("prop1-2").version(1L).build();

        assertExpectedDummySameAsFetchedDummy(inputDummy, foundDummy, startTime, endTime);
    }

    @Test
    public void testGetNotFoundByTenant() {
        var foundDummy = service.get("30000").await().atMost(maxDurationToWait);
        assertThat("Data found when it should be not visible", Objects.isNull(foundDummy));
    }

    @Test
    public void testGetNotFound() {
        var foundDummy = service.get("-100").await().atMost(maxDurationToWait);
        assertThat("Data found when it should be not visible", Objects.isNull(foundDummy));
    }

    @Test
    public void testGetByIdsFound() {
        var dataToFind = List.of(
                Dummy.builder().id("40000").hello("testGetByIdsFound1").someOtherStuff("prop4-2").version(1L).build(),
                Dummy.builder().id("50000").hello("testGetByIdsFound2").someOtherStuff("prop5-2").version(1L).build(),
                Dummy.builder().id("60000").hello("testGetByIdsFound3").someOtherStuff("prop6-2").version(1L).build()
        );
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList()).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("Get size was not same as found size", foundDummys.size() == dataToFind.size());
        assertAll("Data Not Consistent", Streams.zip(dataToFind.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testGetByIdsPartialNotFoundByTenant() {
        var dataToBeFound = List.of(Dummy.builder().id("70000").hello("testGetByIdsPartialNotFoundByTenant1").someOtherStuff("prop7-2").version(1L).build());
        var dataNotToBeFound = List.of(Dummy.builder().id("80000").hello("testGetByIdsFound2").someOtherStuff("prop8-2").version(1L).build());
        var dataToFind = Stream.of(dataToBeFound, dataNotToBeFound).flatMap(Collection::stream).toList();
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList()).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var idsNotToBeFound = dataNotToBeFound.stream().map(Dummy::getId).collect(Collectors.toUnmodifiableSet());
        assertThat("Dummys which were not to be found are present",
                Sets.intersection(foundDummys.stream().map(Dummy::getId).collect(Collectors.toUnmodifiableSet()), idsNotToBeFound).size() == 0);
        assertThat("ToFind size was not same as found size", dataToBeFound.size() == foundDummys.size());
        assertAll("Data Not Consistent", Streams.zip(dataToBeFound.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testGetByIdsPartialNotFoundInDb() {
        var dataToBeFound = List.of(Dummy.builder().id("90000").hello("testGetByIdsPartialNotFoundInDb1").someOtherStuff("prop9-2").version(1L).build());
        var dataNotToBeFound = List.of(Dummy.builder().id("-1").build());
        var dataToFind = Stream.of(dataToBeFound, dataNotToBeFound).flatMap(Collection::stream).toList();
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList()).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var idsNotToBeFound = dataNotToBeFound.stream().map(Dummy::getId).collect(Collectors.toUnmodifiableSet());
        assertThat("Dummys which were not to be found are present",
                Sets.intersection(foundDummys.stream().map(Dummy::getId).collect(Collectors.toUnmodifiableSet()), idsNotToBeFound).size() == 0);
        assertThat("ToFind size was not same as found size", dataToBeFound.size() == foundDummys.size());
        assertAll("Data Not Consistent", Streams.zip(dataToBeFound.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testGetByIdsSortedById() {
        var dataToFind = dataForSortCheck.stream().sorted((a, b) -> (int) (Long.parseLong(a.getId()) - Long.parseLong(b.getId()))).toList();
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList(), List.of("id")).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("Get size was not same as found size", foundDummys.size() == dataToFind.size());
        assertAll("Data Not Consistent", Streams.zip(dataToFind.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testGetByIdsSortedByHello() {
        var dataToFind = dataForSortCheck.stream().sorted(Comparator.comparing(Dummy::getHello)).toList();
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList(), List.of("hello")).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("Get size was not same as found size", foundDummys.size() == dataToFind.size());
        assertAll("Data Not Consistent", Streams.zip(dataToFind.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testGetByIdsSortedByReverseSomeOtherStuff() {
        var dataToFind = dataForSortCheck.stream().sorted((a, b) -> -a.getSomeOtherStuff().compareTo(b.getSomeOtherStuff())).toList();
        var startTime = Instant.now();
        var foundDummys = service.getByIds(dataToFind.stream().map(Dummy::getId).toList(), List.of("-someOtherStuff")).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("Get size was not same as found size", foundDummys.size() == dataToFind.size());
        assertAll("Data Not Consistent", Streams.zip(dataToFind.stream(), foundDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testPatchFound() {
        var dummy = Dummy.builder().id("130000").hello("patchedValue13").build();
        var startTime = Instant.now();
        var patchedDummy = service.patch(dummy.getId(), dummy).await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var expectedDummy = Dummy.builder().id("130000").hello("patchedValue13")
                .someOtherStuff("prop13-2").version(2L).build();
        assertExpectedDummySameAsPatchedDummy(expectedDummy, patchedDummy, startTime, endTime);
        assertThat("UpdatedAt not in range", startTime.isBefore(patchedDummy.getLastModifiedAt()) && endTime.isAfter(patchedDummy.getLastModifiedAt()));
    }

    @Test
    public void testPatchNotFound() {
        var patchedInput = Dummy.builder().id("-100").hello("topatch").someOtherStuff("toPatchAnother").build();
        var foundDummy = service.patch(patchedInput.getId(), patchedInput).await().atMost(maxDurationToWait);
        assertThat("Data patched when it should be not visible", Objects.isNull(foundDummy));
    }

    @Test
    @Disabled
    public void testPutFound() {
        var dummy = Dummy.builder().id("140000").hello("putValue13").build();
        var startTime = Instant.now();
        var patchedDummy = service.put(dummy.getId(), dummy).await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        var expectedDummy = Dummy.builder().id("140000").hello("putValue13").someOtherStuff(null).version(2L).build();
        assertExpectedDummySameAsPatchedDummy(expectedDummy, patchedDummy, startTime, endTime);
        assertThat("UpdatedAt not in range", startTime.isBefore(patchedDummy.getLastModifiedAt()) && endTime.isAfter(patchedDummy.getLastModifiedAt()));
    }

    @Test
    public void testPutIfNotFound() {
        var dummy = Dummy.builder().id("-100").hello("someValue").build();
        var foundDummy = service.put(dummy.getId(), dummy).await().atMost(maxDurationToWait);
        assertThat("Data Put when it should be not visible", Objects.isNull(foundDummy));
    }

    @Test
    public void testbulkCreateWithResponse() {
        List<Dummy> dummysToCreate = IntStream.range(1, 5)
                .mapToObj(position -> Dummy.builder().hello("testbulkCreateP1" + position).someOtherStuff("testbulkCreateP2" + position).version(1L).build())
                .collect(Collectors.toList());
        var beforeCreate = Instant.now();
        var createdDummys = service.bulkCreateWithResponse(dummysToCreate).await().atMost(maxDurationToWait);
        var createdDummyIds = createdDummys.stream().map(Dummy::getId).toList();
        var afterCreate = Instant.now();
        // TODO bulkCreate is currently not returning the IDs
        assertThat("Create size was not same as found size", dummysToCreate.size() == createdDummyIds.size());
        var fetchedDummys = service.getByIds(createdDummyIds).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not same as found size", fetchedDummys.size() == createdDummyIds.size());
        assertAll("Data Not Consistent", Streams.zip(dummysToCreate.stream(), fetchedDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsCreatedDummy(correct, found, beforeCreate, afterCreate))
        );
    }

    @Test
    public void testbulkPatch() {
        List<Dummy> dummysToPatch = List.of(
                Dummy.builder().id("180000").hello("bulkPatched1-prop1").someOtherStuff("bulkPatched1-prop2").build(),
                Dummy.builder().id("190000").hello("bulkPatched2-prop1").build()
        );
        var beforePatch = Instant.now();
        service.bulkPatch(dummysToPatch).await().atMost(maxDurationToWait);
        var afterPatch = Instant.now();

        var fetchedDummys = service.getByIds(dummysToPatch.stream().map(Dummy::getId).toList()).collect().asList().await().atMost(maxDurationToWait);


        var expectedDummys = List.of(
                Dummy.builder().id("180000").hello("bulkPatched1-prop1").someOtherStuff("bulkPatched1-prop2").version(2L).build(),
                Dummy.builder().id("190000").hello("bulkPatched2-prop1").someOtherStuff("prop19-2").version(2L).build()
        );

        assertThat("Create size was not same as found size", fetchedDummys.size() == expectedDummys.size());
        assertAll("Data Not Consistent", Streams.zip(expectedDummys.stream(), fetchedDummys.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsPatchedDummy(correct, found, beforePatch, afterPatch))
        );
    }

    @Test
    @Disabled
    public void testbulkUpsert() {
        assert false;
    }

    @Test
    public void testDelete() {
        Dummy dummy = Dummy.builder().id("150000").hello("testDelete").someOtherStuff("prop15-2").version(1L).build();
        Instant beforeFetch = Instant.now();
        Dummy fetchBeforeDelete = service.get(dummy.getId()).await().atMost(maxDurationToWait);
        Instant afterFetch = Instant.now();
        assertExpectedDummySameAsFetchedDummy(dummy, fetchBeforeDelete, beforeFetch, afterFetch);
        Instant beforeDelete = Instant.now();
        Dummy dummyOnDelete = service.delete(dummy).await().atMost(maxDurationToWait);
        Instant afterDelete = Instant.now();
        dummy.setVersion(2L);
        assertExpectedDummySameAsFetchedDummy(dummy, dummyOnDelete, beforeDelete, afterDelete);
        Dummy deletedFetch = service.get(dummy.getId()).await().atMost(maxDurationToWait);
        assertThat("Dummy was found even after deletion", Objects.isNull(deletedFetch));
    }

    @Test
    public void testDeleteIfNotFound() {
        var dummy = Dummy.builder().id("-1000").build();
        var deletedFetch = service.delete(dummy).await().atMost(maxDurationToWait);
        assertThat("Dummy was deleted even after not being present", Objects.isNull(deletedFetch));
    }

    @Test
    public void testDeleteAfterAlreadyDeleted() {
        var dummy = Dummy.builder().id("160000").build();
        var deletedFetch = service.delete(dummy).await().atMost(maxDurationToWait);
        assertThat("Dummy was found even after deletion", Objects.isNull(deletedFetch));
    }

    @Test
    public void testDeleteForOtherTenant() {
        var dummy = Dummy.builder().id("170000").build();
        var deletedFetch = service.delete(dummy).await().atMost(maxDurationToWait);
        assertThat("Dummy was found even after being present on another tenant", Objects.isNull(deletedFetch));
    }

    @Test
    public void testFindByPageSortedById() {
        var offset = 0;
        var limit = Integer.MAX_VALUE;
        var foundData = service.findByPage(offset, limit, List.of("id")).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not as required size", foundData.size() <= limit);
        assertThat("Get size was not as required size", !foundData.isEmpty());
        var expectedIds = foundData.stream().sorted(Comparator.comparing(a -> Long.parseLong(a.getId()))).map(Dummy::getId).toList();
        assertAll("Data was not sorted as needed", Streams.zip(expectedIds.stream(), foundData.stream().map(Dummy::getId),
                (expected, found) -> () -> assertThat("ID is not same", Objects.equals(expected, found))));
    }

    @Test
    public void testFindByPageSortedByIdWithLimit() {
        var offset = 0;
        var limit = 2;
        var foundData = service.findByPage(offset, limit, List.of("id")).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not as required size", foundData.size() <= limit);
        assertThat("Get size was not as required size", !foundData.isEmpty());
        var expectedIds = foundData.stream().sorted(Comparator.comparing(a -> Long.parseLong(a.getId()))).map(Dummy::getId).toList();
        assertAll("Data was not sorted as needed", Streams.zip(expectedIds.stream(), foundData.stream().map(Dummy::getId),
                (expected, found) -> () -> assertThat("ID is not same", Objects.equals(expected, found))));
    }

    @Test
    public void testFindByPageSortedByIdWithLimitAndOffset() {
        var offset = 3;
        var limit = 2;
        var foundData = service.findByPage(offset, limit, List.of("id")).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not as required size", foundData.size() <= limit);
        assertThat("Get size was not as required size", !foundData.isEmpty());
        var expectedIds = foundData.stream().sorted(Comparator.comparing(a -> Long.parseLong(a.getId()))).map(Dummy::getId).toList();
        assertAll("Data was not sorted as needed", Streams.zip(expectedIds.stream(), foundData.stream().map(Dummy::getId),
                (expected, found) -> () -> assertThat("ID is not same", Objects.equals(expected, found))));
    }

    @Test
    public void testFindByPageSortedByHelloWithLimit() {
        var offset = 0;
        var limit = 10;
        var foundData = service.findByPage(offset, limit, List.of("hello")).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not as required size", foundData.size() <= limit - offset);
        assertThat("Get size was not as required size", !foundData.isEmpty());
        var expectedIds = foundData.stream().sorted(Comparator.comparing(Dummy::getHello)).map(Dummy::getId).toList();
        assertAll("Data was not sorted as needed", Streams.zip(expectedIds.stream(), foundData.stream().map(Dummy::getId),
                (expected, found) -> () -> assertThat("ID is not same", Objects.equals(expected, found))));
    }

    @Test
    public void testFindByPageSortedByHelloDescWithLimit() {
        var offset = 0;
        var limit = 10;
        var foundData = service.findByPage(offset, limit, List.of("-hello")).collect().asList().await().atMost(maxDurationToWait);
        assertThat("Get size was not as required size", foundData.size() <= limit - offset);
        assertThat("Get size was not as required size", !foundData.isEmpty());
        var expectedIds = foundData.stream().sorted((a, b) -> -a.getHello().compareTo(b.getHello())).map(Dummy::getId).toList();
        assertAll("Data was not sorted as needed", Streams.zip(expectedIds.stream(), foundData.stream().map(Dummy::getId),
                (expected, found) -> () -> assertThat("ID is not same", Objects.equals(expected, found))));
    }


    @Test
    public void testFindByCreatedAtGreaterThan() {
        var startTime = Instant.now();
        var extecped = Dummy.builder().hello("testFindByCreatedAtGreaterThan").someOtherStuff("otherStuff").build();
        var createdDummy = service.create(extecped).await().atMost(maxDurationToWait);
        var actual = service.findByCreatedAtGreaterThan(startTime, 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertExpectedDummySameAsCreatedDummy(createdDummy, actual.get(0), startTime, endTime);
    }

    @Test
    public void testFindByCreatedAtLessThan() {
        var expectedIds = Collections.singletonList("210000");
        var expectedFetched = service.getByIds(Collections.singletonList("210000")).collect().asList().await().atMost(maxDurationToWait);
        var startTime = Instant.now();
        var actual = service.findByCreatedAtLessThan(Instant.parse("2021-12-31T00:00:00.00Z"), 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("ToFind size was not same as found size", expectedIds.size() == actual.size());
        assertAll("Data Not Consistent", Streams.zip(expectedFetched.stream(), actual.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testFindByCreatedAtBetween() {
        var expectedIds = Collections.singletonList("210000");
        var expectedFetched = service.getByIds(expectedIds).collect().asList().await().atMost(maxDurationToWait);
        var startTime = Instant.now();
        var actual =
                service.findByCreatedAtBetween(Instant.parse("2019-12-31T00:00:00.00Z"), Instant.parse("2021-12-31T00:00:00.00Z"), 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("ToFind size was not same as found size", expectedIds.size() == actual.size());
        assertAll("Data Not Consistent", Streams.zip(expectedFetched.stream(), actual.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testFindByLastModifiedAtGreaterThan() {
        var expectedIds = Arrays.asList("50000", "60000");
        var expectedFetch = service.getByIds(expectedIds).collect().asList().await().atMost(maxDurationToWait);
        var startTime = Instant.now();
        var actual = service.findByLastModifiedAtGreaterThan(Instant.parse("2022-02-01T00:00:00.00Z"), 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("ToFind size was not same as found size", expectedIds.size() == actual.size());
        assertAll("Data Not Consistent", Streams.zip(expectedFetch.stream(), actual.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testFindByLastModifiedAtLessThan() {
        var expectedIds = Collections.singletonList("210000");
        var expectedFetch = service.getByIds(expectedIds).collect().asList().await().atMost(maxDurationToWait);
        var startTime = Instant.now();
        var actual = service.findByLastModifiedAtLessThan(Instant.parse("2021-12-31T00:00:00.00Z"), 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("ToFind size was not same as found size", expectedIds.size() == actual.size());
        assertAll("Data Not Consistent", Streams.zip(expectedFetch.stream(), actual.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }

    @Test
    public void testFindByLastModifiedAtBetween() {
        var expectedIds = Collections.singletonList("50000");
        var expectedFetch = service.getByIds(expectedIds).collect().asList().await().atMost(maxDurationToWait);
        var startTime = Instant.now();
        var actual = service.findByLastModifiedBetween(Instant.parse("2022-02-05T00:00:00.00Z"), Instant.parse("2022-03-30T00:00:00.00Z"), 0, 100, null).collect().asList().await().atMost(maxDurationToWait);
        var endTime = Instant.now();
        assertThat("ToFind size was not same as found size", expectedIds.size() == actual.size());
        assertAll("Data Not Consistent", Streams.zip(expectedFetch.stream(), actual.stream(),
                (correct, found) -> () -> assertExpectedDummySameAsFetchedDummy(correct, found, startTime, endTime))
        );
    }


    @Test
    public void outboxCreation() {
        Dummy dummy = Dummy.builder().hello("hello").build();
        Dummy createdDummy = service.create(dummy, "testOutBox", "testOutbox").await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        List<OutboxEntity> outboxList = outboxRepository.findAll().list().await().indefinitely();
        assertThat("Outbox size is incorrect", outboxList.size() == 1);
        assertThat("Outbox is incorrectly created", outboxList.get(0).getAggregateId().equals(createdDummy.getId()));
    }
}