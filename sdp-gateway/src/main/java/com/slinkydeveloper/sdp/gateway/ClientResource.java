package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.gateway.model.Node;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Path("client")
public class ClientResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Node> nodes() {
        return DataRepository.getNodesSet();
    }

}
