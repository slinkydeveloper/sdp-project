package com.slinkydeveloper.sdp.gateway;

import com.slinkydeveloper.sdp.gateway.model.Node;
import com.slinkydeveloper.sdp.gateway.model.SensorDataAverage;
import com.slinkydeveloper.sdp.gateway.model.SensorDataStatistics;

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

@Path("client")
public class ClientResource {

    @GET
    @Path("nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Node> nodes() {
        return DataRepository.getNodesSet();
    }

    @GET
    @Path("data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response data(@QueryParam("n") Integer n) {
        List<Map.Entry<ZonedDateTime, SensorDataAverage>> values = (n == null || n == 0) ?
            DataRepository.getSensorData().getCopy() :
            DataRepository.getSensorData().getLast(n);

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
