package no.nav.foreldrepenger.web.app.tjenester.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.batch.feil.InvalidArgumentsVLBatchException;
import no.nav.foreldrepenger.web.app.tjenester.batch.args.BatchArgumentsDto;

class BatchRestTjenesteTest {

    private BatchRestTjeneste tjeneste;

    private BatchSupportTjeneste batchSupportTjeneste;

    @BeforeEach
    public void setUp() {
        batchSupportTjeneste = mock(BatchSupportTjeneste.class);
        tjeneste = new BatchRestTjeneste(batchSupportTjeneste);
    }

    @Test
    void skal_gi_status_400_ved_ukjent_batchname() {
        when(batchSupportTjeneste.finnBatchTjenesteForNavn(any())).thenReturn(null);
        @SuppressWarnings("resource")
        final var response = tjeneste.startBatch(new BatchNameDto("asdf"), null);
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
    }

    @Test
    void skal_gi_exception_ved_ugyldig_job_parametere() {
        final var stringBatchTjenesteHashMap = new HashMap<String, BatchTjeneste>();
        final var value = mock(BatchTjeneste.class);
        final var args = new BatchArgumentsDto();
        final var key = "mock";

        Map<String, String> arguments = new HashMap<>();

        when(value.getBatchName()).thenReturn(key);
        when(value.createArguments(any())).thenReturn(new UgyldigeBatchArguments(arguments));
        stringBatchTjenesteHashMap.put(key, value);

        when(batchSupportTjeneste.finnBatchTjenesteForNavn(any())).thenReturn(value);

        args.setJobParameters("asdf=1");
        assertThrows(InvalidArgumentsVLBatchException.class, () -> tjeneste.startBatch(new BatchNameDto(key), args));
    }

    @Test
    void skal_kalle_paa_tjeneste_ved_gyldig_() {
        final var stringBatchTjenesteHashMap = new HashMap<String, BatchTjeneste>();
        Map<String, String> arguments = new HashMap<>();
        final var gyldigeBatchArguments = new GyldigeBatchArguments(arguments);
        final var args = new BatchArgumentsDto();
        final var value = mock(BatchTjeneste.class);
        final var key = "mock";
        when(value.getBatchName()).thenReturn(key);
        when(value.createArguments(any())).thenReturn(gyldigeBatchArguments);
        stringBatchTjenesteHashMap.put(key, value);

        when(batchSupportTjeneste.finnBatchTjenesteForNavn(any())).thenReturn(value);

        args.setJobParameters("asdf=1");
        tjeneste.startBatch(new BatchNameDto(key), args);

        verify(value).launch(gyldigeBatchArguments);
    }

    public static class UgyldigeBatchArguments extends BatchArguments {

        UgyldigeBatchArguments(Map<String, String> arguments) {
            super(arguments);
        }

        @Override
        public boolean settParameterVerdien(String key, String value) {
            return true;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String toString() {
            return "UgyldigeBatchArguments{}";
        }
    }

    public static class GyldigeBatchArguments extends BatchArguments {

        GyldigeBatchArguments(Map<String, String> arguments) {
            super(arguments);
        }

        @Override
        public boolean settParameterVerdien(String key, String value) {
            return true;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String toString() {
            return "GyldigeBatchArguments{}";
        }
    }
}
