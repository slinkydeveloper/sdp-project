package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.gateway.model.Node;
import com.slinkydeveloper.sdp.gateway.model.SensorReadingsAverage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.stream.Collectors;

@Path("node")
public class NodeResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Node> join(Node newNode) {
        DataRepository.getHosts().put(newNode.getId(), newNode.getHost());
        return DataRepository.getNodesSet();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishNewHosts(Set<Node> hosts) {
        DataRepository
            .getHosts()
            .replaceAll(
                hosts
                    .stream()
                    .collect(Collectors.toMap(Node::getId, Node::getHost))
            );
        return Response.accepted().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishReadings(SensorReadingsAverage newAverage) {
        DataRepository
            .getSensorReadings()
            .append(new SimpleImmutableEntry<>(ZonedDateTime.now(), newAverage));
        return Response.accepted().build();
    }

}
