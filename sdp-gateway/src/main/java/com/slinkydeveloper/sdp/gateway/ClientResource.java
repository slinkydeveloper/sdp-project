package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.log.LoggerConfig;
import com.slinkydeveloper.sdp.model.Node;
import com.slinkydeveloper.sdp.model.SensorDataAverage;
import com.slinkydeveloper.sdp.model.SensorDataStatistics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Path("client")
public class ClientResource {

    private final static Logger LOG = LoggerConfig.getLogger(ClientResource.class);

    @GET
    @Path("nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Node> nodes() {
        LOG.info("GET nodes");
        return DataRepository.getNodesSet();
    }

    @GET
    @Path("data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response data(@QueryParam("limit") Integer limit) {
        LOG.info("GET data. limit: " + limit);
        List<Map.Entry<ZonedDateTime, SensorDataAverage>> values = (limit == null || limit == 0) ?
            DataRepository.getSensorData().getCopy() :
            DataRepository.getSensorData().getLast(limit);

        return computeStatistics(values)
            .map(stats -> Response.ok(stats, MediaType.APPLICATION_JSON).build())
            .orElseGet(() -> Response.status(404, "No data available").build());
    }

    private Optional<SensorDataStatistics> computeStatistics(List<Map.Entry<ZonedDateTime, SensorDataAverage>> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        double mean = values
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(SensorDataAverage::getAverage)
            .average()
            .getAsDouble();
        double variance = values
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(SensorDataAverage::getAverage)
            .map(v -> v - mean)
            .map(v -> v * v)
            .average()
            .getAsDouble();

        return Optional.of(new SensorDataStatistics(
            values,
            mean,
            Math.sqrt(variance)
        ));
    }

}
