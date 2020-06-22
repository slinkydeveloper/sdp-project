package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataAverage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("node")
public class NodeResource {

    private final static Logger LOG = LoggerConfig.getLogger(NodeResource.class);

    @POST
    @Path("join")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response join(Node newNode) {
        LOG.info("POST join. new nodes: " + newNode);

        boolean added = DataRepository.getHosts().putIf(
            newNode.getId(),
            newNode.getHost(),
            m -> !m.containsKey(newNode.getId()) || (m.containsKey(newNode.getId()) && newNode.getHost().equals(m.get(newNode.getId())))
        );
        if (added) {
            return Response.ok(DataRepository.getNodesSet(), MediaType.APPLICATION_JSON).build();
        }
        return Response.status(400, "Node cannot be added").build();
    }

    @POST
    @Path("publishNewHosts")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishNewHosts(Set<Node> hosts) {
        LOG.info("POST publishNewHosts. new hosts: " + hosts);

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
    @Path("publishNewAverage")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishNewAverage(SensorDataAverage newAverage) {
        LOG.info("POST publishNewAverage. new average: " + newAverage);
        DataRepository
            .getSensorData()
            .append(new SimpleImmutableEntry<>(ZonedDateTime.now(), newAverage));
        return Response.accepted().build();
    }

}
