package com.services.sf.test.domain.mongo;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.constant.GlobalConstant;
import com.services.common.domain.basemongo.Outbox;
import com.services.common.domain.util.LocalContext;
import com.services.sf.domain.base.outbox.OutboxSerializer;
import com.services.sf.domain.basemongo.outbox.OutboxRepository;
import com.services.sf.domain.basemongo.util.MongoHelper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.List;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(MongoReplicaSetTestResource.class)
public class TestDummyMongo {

    @Inject
    protected LocalContext localContext;

    @Inject
    protected DummyService service;
    @Inject
    protected MongoHelper mongoHelper;
    @Inject
    protected DummyRepository repository;
    @Inject
    protected OutboxRepository outboxRepository;
    @InjectMock
    OutboxSerializer serializer;
    String userId = new ObjectId().toString();

    String testService = "testService";

    @BeforeEach
    public void setup() {
        repository.mongoDatabase().createCollection("testCollection").await().indefinitely();
        repository.mongoDatabase().createCollection("outbox").await().indefinitely();
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        Mockito.when(serializer.serialize(Mockito.anyString(), Mockito.any())).thenReturn(byteBuffer.array());
        setLocalContext();
    }

    @AfterEach
    public void postTest() {
        repository.mongoDatabase().drop().await().indefinitely();
    }


    private void setLocalContext() {
        localContext.setTenantId("framework");
        localContext.setUserId(userId);
        localContext.set(GlobalConstant.HEADER_SERVICE, testService);
    }

    @Test
    public void testCreate() {
        Dummy dummy = Dummy.builder().hello("hello").a(2).b(2).build();
        Dummy createdDummy = service.create(dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        assert (createdDummy.getA().equals(2));
        assert (createdDummy.getB().equals(2));
        assert (createdDummy.getCreatedBy().equals(testService));
    }

    @Test
    public void testPatch() {
        Dummy dummy = Dummy.builder().hello("hello").a(2).b(2).build();
        Dummy createdDummy = service.create(dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        assert (createdDummy.getA().equals(2));
        assert (createdDummy.getB().equals(2));
        assert (createdDummy.getCreatedBy().equals(testService));
        createdDummy.setHello("bye");
        createdDummy.setA(3);
        Dummy updatedDummy = service.patch(createdDummy.getId(), createdDummy).await().indefinitely();
        assert (updatedDummy.getA().equals(3));
        assert (updatedDummy.getB().equals(2));
        assert (updatedDummy.getHello().equals(createdDummy.getHello()));
        assert (updatedDummy.getCreatedBy().equals(testService));
        assert (updatedDummy.getVersion() == 2);
    }

    @Test
    public void testPut() {
        Dummy dummy = Dummy.builder().hello("hello").a(2).b(2).build();
        Dummy createdDummy = service.create(dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        assert (createdDummy.getA().equals(2));
        assert (createdDummy.getB().equals(2));
        assert (createdDummy.getCreatedBy().equals(testService));
        Dummy putDummy = Dummy.builder()
                .id(createdDummy.getId())
                .hello("bye")
                .a(3)
                .build();
        Dummy updatedDummy = service.put(putDummy.getId(), putDummy).await().indefinitely();
        assert (updatedDummy.getA().equals(3));
        assert (updatedDummy.getB() == null);
        assert (updatedDummy.getHello().equals("bye"));
        assert (updatedDummy.getCreatedBy().equals(testService));
        assert (updatedDummy.getVersion() == 1);
        Dummy getDummy = service.get(createdDummy.getId()).await().indefinitely();
        assert (getDummy.getVersion() == 1);
        assert (getDummy.getA().equals(3));
        assert (getDummy.getB() == null);
        assert (getDummy.getHello().equals("bye"));
        assert (getDummy.getCreatedBy().equals(testService));

    }

    @Test
    public void testTransactionalCreate() {
        Dummy dummy = Dummy.builder().hello("hello").build();
        ClientSession clientSession = mongoHelper.createClientSessionPrimary().await().indefinitely();
        clientSession.startTransaction();
        Dummy createdDummy = service.create(dummy, clientSession).await().indefinitely();
        Uni.createFrom().publisher(clientSession.commitTransaction()).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
    }

    @Test
    public void outboxCreation() {
        Dummy dummy = Dummy.builder().hello("hello").build();
        Dummy createdDummy = service.create(dummy, "DummyCreated", "DummyCreated").await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        List<Outbox> outboxList = outboxRepository.findAll(new Document("aggregateId", createdDummy.getId())).collect().asList().await().indefinitely();
        assert (outboxList.size() == 1);
        assert (outboxList.get(0).getAggregateType().equals("DummyCreated"));
    }

    @Test
    public void testUpsert() {
        Dummy dummy = Dummy.builder().hello("hello").build();
        Dummy createdDummy = service.upsert(dummy, dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
    }

    @Test
    public void testSetOnInsert() {
        Dummy dummy = Dummy.builder()
                .hello("hello")
                .a(10)
                .b(10)
                .build();
        Dummy createdDummy = repository.testSetOnInsertWithSet(dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        assert (createdDummy.getA().equals(2));
        assert (createdDummy.getB().equals(2));
    }

    @Test
    public void testInc() {
        Dummy dummy = Dummy.builder()
                .hello("hello")
                .a(10)
                .b(10)
                .build();
        Dummy createdDummy = repository.testIncWithSet(dummy).await().indefinitely();
        assert (dummy.getHello().equals(createdDummy.getHello()));
        assert (createdDummy.getA().equals(2));
        assert (createdDummy.getB().equals(2));
    }

}
