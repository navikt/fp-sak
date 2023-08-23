package no.nav.foreldrepenger.web.server.jetty;

import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import no.nav.vedtak.log.metrics.MetricsUtil;

import java.time.Duration;

@Provider
@Priority(Priorities.USER)
public class TimingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String STATUS = "status";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String METRIC_NAME = "rest";
    private static final String COUNTER_NAME = "restantall";
    private static final ThreadLocalTimer TIMER = new ThreadLocalTimer();

    public TimingFilter() {
        MetricsUtil.utvidMedPercentiler(METRIC_NAME, 0.5, 0.95, 0.99);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        Metrics.timer(METRIC_NAME, PATH, req.getUriInfo().getPath(), METHOD, req.getMethod()).record(Duration.ofMillis(TIMER.stop()));
        Metrics.counter(COUNTER_NAME, METHOD, req.getMethod(), STATUS, String.valueOf(res.getStatus())).increment();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        TIMER.start();
    }

    private static class ThreadLocalTimer extends ThreadLocal<Long> {
        public void start() {
            this.set(System.currentTimeMillis());
        }

        public long stop() {
            return System.currentTimeMillis() - get();
        }

        @Override
        protected Long initialValue() {
            return System.currentTimeMillis();
        }
    }
}
