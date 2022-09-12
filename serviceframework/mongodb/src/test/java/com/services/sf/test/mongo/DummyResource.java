package com.services.sf.test.mongo;

import com.services.sf.commons.base.BaseResourceImpl;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

@Path("/api/v1/dummy")
@ApplicationScoped
public class DummyResource extends BaseResourceImpl {

    @GET
    @Path("/1")
    public Uni<List<String>> testList1() {
        return Uni.createFrom().item(new ArrayList<>());
    }

    @GET
    @Path("/2")
    public Uni<List<String>> testList2() {
        return Uni.createFrom().item(List.of("1"));
    }

    @GET
    @Path("/3")
    public Uni<List<String>> testList3() {
        return Uni.createFrom().nullItem();
    }

    @GET
    @Path("/4")
    public Uni<List<String>> testList4() {
        return Uni.createFrom().item(new ArrayList<>());
    }
}
