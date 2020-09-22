package no.nav.foreldrepenger.web.app.healthchecks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.vedtak.log.util.MemoryAppender;

public class SelftestServletTest {

    private static MemoryAppender logSniffer;
    private static Logger LOG;
    private HealthCheckRestService healthCheckRestService; // objektet vi tester

    private HttpServletRequest mockRequest;
    private Selftests mockSelftests;

    private static final String MSG_KRITISK_FEIL = "kritisk feil";
    private static final String MSG_IKKEKRITISK_FEIL = "ikke-kritisk feil";

    @BeforeAll
    public static void setUp() throws Exception {
        LOG = Logger.class.cast(LoggerFactory.getLogger(HttpServletRequest.class));
        LOG.setLevel(Level.INFO);
        logSniffer = new MemoryAppender(LOG.getName());
        LOG.addAppender(logSniffer);
        logSniffer.start();
    }

    @BeforeEach
    public void setup() {
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

        mockSelftests = mock(Selftests.class);

        healthCheckRestService = new HealthCheckRestService();
        healthCheckRestService.setSelftests(mockSelftests);
    }

    @AfterEach
    public void afterEach() {
        logSniffer.reset();
    }

    @Test
    public void test_doGet_alleDeltesterOk() {
        SelftestResultat resultat = lagSelftestResultat(true, true);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
        assertThat(logSniffer.countEventsForLogger()).isEqualTo(0);
    }

    @Test
    public void test_doGet_kritiskeDeltesterOkIkkeKritiskeDeltesterFeil() {
        SelftestResultat resultat = lagSelftestResultat(true, false);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
        // logSniffer.assertNoErrors();
        // logSniffer.assertHasWarnMessage(MSG_IKKEKRITISK_FEIL);
    }

    @Test
    public void test_doGet_kritiskeDeltesterFeilIkkeKritiskeDeltesterOk() {
        SelftestResultat resultat = lagSelftestResultat(false, true);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
        // logSniffer.assertHasErrorMessage(MSG_KRITISK_FEIL);
        // logSniffer.assertNoWarnings();
    }

    @Test
    public void test_doGet_kritiskeDeltesterFeilIkkeKritiskeDeltesterFeil() {
        SelftestResultat resultat = lagSelftestResultat(false, false);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
        // logSniffer.assertHasErrorMessage(MSG_KRITISK_FEIL);
        // logSniffer.assertHasWarnMessage(MSG_IKKEKRITISK_FEIL);
    }

    @Test
    public void test_doGet_html() {
        when(mockRequest.getContentType()).thenReturn(MediaType.TEXT_HTML);
        SelftestResultat resultat = lagSelftestResultat(true, true);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
    }

    @Test
    public void test_doGet_jsonAsHtml() {
        when(mockRequest.getContentType()).thenReturn(MediaType.TEXT_HTML);
        when(mockRequest.getParameter("json")).thenReturn("true");
        SelftestResultat resultat = lagSelftestResultat(true, true);
        when(mockSelftests.run()).thenReturn(resultat);

        healthCheckRestService.selftest();

        verify(mockSelftests, atLeast(1)).run();
    }

    // -------

    private SelftestResultat lagSelftestResultat(boolean kritiskeOk, boolean ikkeKritiskeOk) {
        SelftestResultat resultat = lagSelftestResultat();

        HealthCheck.Result delRes1 = kritiskeOk ? HealthCheck.Result.healthy() : HealthCheck.Result.unhealthy(MSG_KRITISK_FEIL);
        resultat.leggTilResultatForKritiskTjeneste(delRes1);

        HealthCheck.Result delRes2 = ikkeKritiskeOk ? HealthCheck.Result.healthy() : HealthCheck.Result.unhealthy(MSG_IKKEKRITISK_FEIL);
        resultat.leggTilResultatForIkkeKritiskTjeneste(delRes2);

        return resultat;
    }

    private SelftestResultat lagSelftestResultat() {
        SelftestResultat resultat = new SelftestResultat();
        resultat.setApplication("test-appl");
        resultat.setRevision("1");
        resultat.setVersion("2");
        resultat.setBuildTime("nu");
        resultat.setTimestamp(LocalDateTime.now());
        return resultat;
    }
}
