package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.exception.FunksjonellException;

class ForespørselStatusRequestTest {

    private static final String SAKSNUMMER = "1234567890";
    private static final String ORGNR = "999999999";

    @Test
    void eneste_fagsak_saksnummer_returnerer_saksnummeret_naar_alle_forespoersler_gjelder_samme_sak() {
        var annetOrgnummer = "888888888";
        var request = new ForespørselStatusRequest(
            List.of(gyldigForespørsel(), new ForespørselStatusRequest.Forespørsel(SAKSNUMMER, annetOrgnummer)));

        assertThat(request.enesteFagsakSaksnummer()).isEqualTo(SAKSNUMMER);
    }

    @Test
    void eneste_fagsak_saksnummer_kaster_funksjonell_exception_ved_flere_ulike_saksnummer() {
        var annetSaksnummer = "2234567890";
        var request = new ForespørselStatusRequest(
            List.of(gyldigForespørsel(), new ForespørselStatusRequest.Forespørsel(annetSaksnummer, ORGNR)));

        assertThatThrownBy(request::enesteFagsakSaksnummer).isInstanceOf(FunksjonellException.class);
    }

    private static ForespørselStatusRequest.Forespørsel gyldigForespørsel() {
        return new ForespørselStatusRequest.Forespørsel(SAKSNUMMER, ORGNR);
    }
}
