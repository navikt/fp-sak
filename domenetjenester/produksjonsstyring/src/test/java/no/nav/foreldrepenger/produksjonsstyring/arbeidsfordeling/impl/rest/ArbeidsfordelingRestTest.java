package no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.impl.rest;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest.ArbeidsfordelingResponse;

public class ArbeidsfordelingRestTest {

    private static final ObjectWriter WRITER = TestJsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private static final ObjectReader READER = TestJsonMapper.getMapper().reader();

    @Test
    public void test_request() throws Exception {
        var request = ArbeidsfordelingRequest.ny()
            .medTemagruppe("FMLI")
            .medTema("FOR")
            .medOppgavetype("BEH_SAK_VL")
            .medBehandlingstype(BehandlingType.REVURDERING.getKode())
            .build();

        String json = WRITER.writeValueAsString(request);
        System.out.println(json);

        ArbeidsfordelingRequest roundTripped = READER.forType(ArbeidsfordelingRequest.class).readValue(json);

        assertThat(roundTripped).isNotNull();
        assertThat(roundTripped.getTema()).isEqualTo("FOR");
        assertThat(roundTripped.getBehandlingstype()).isEqualTo(BehandlingType.REVURDERING.getKode());
        validateResult(roundTripped);
    }

    @Test
    public void  test_response() throws Exception {
        var respons = new ArbeidsfordelingResponse("4806", "Drammen", "Aktiv", "FPY");

        String json = WRITER.writeValueAsString(respons);
        System.out.println(json);

        ArbeidsfordelingResponse roundTripped = READER.forType(ArbeidsfordelingResponse.class).readValue(json);

        assertThat(roundTripped).isNotNull();
        assertThat(roundTripped.getEnhetNr()).isEqualTo("4806");
        validateResult(roundTripped);
    }

    private void validateResult(Object roundTripped) {
        assertThat(roundTripped).isNotNull();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var violations = validator.validate(roundTripped);
            assertThat(violations).isEmpty();
        }
    }
}
