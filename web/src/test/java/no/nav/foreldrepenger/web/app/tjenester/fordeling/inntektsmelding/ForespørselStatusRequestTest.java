package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.exception.FunksjonellException;

class ForespørselStatusRequestTest {

    private static final String SAKSNUMMER = "1234567890";
    private static final String ORGNR = "999999999";

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void gyldig_request_gir_ingen_brudd() {
        var request = new ForespørselStatusRequest(List.of(gyldigForespørsel()));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void tom_liste_er_ugyldig() {
        var request = new ForespørselStatusRequest(List.of());

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void liste_over_maks_antall_er_ugyldig() {
        var forMange = java.util.Collections.nCopies(ForespørselStatusRequest.MAKS_ANTALL + 1, gyldigForespørsel());

        assertThat(validator.validate(new ForespørselStatusRequest(forMange))).isNotEmpty();
    }

    @Test
    void orgnummer_som_ikke_er_ni_siffer_er_ugyldig() {
        var request = new ForespørselStatusRequest(
            List.of(new ForespørselStatusRequest.Forespørsel(SAKSNUMMER, "12345", ForespørselStatusRequest.YtelseType.FORELDREPENGER)));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void toString_maskerer_orgnummer() {
        assertThat(gyldigForespørsel().toString()).doesNotContain(ORGNR);
    }

    @Test
    void eneste_fagsak_saksnummer_returnerer_saksnummeret_naar_alle_forespoersler_gjelder_samme_sak() {
        var annetOrgnummer = "888888888";
        var request = new ForespørselStatusRequest(
            List.of(gyldigForespørsel(), new ForespørselStatusRequest.Forespørsel(SAKSNUMMER, annetOrgnummer, ForespørselStatusRequest.YtelseType.FORELDREPENGER)));

        assertThat(request.enesteFagsakSaksnummer()).isEqualTo(SAKSNUMMER);
    }

    @Test
    void eneste_fagsak_saksnummer_kaster_funksjonell_exception_ved_flere_ulike_saksnummer() {
        var annetSaksnummer = "2234567890";
        var request = new ForespørselStatusRequest(
            List.of(gyldigForespørsel(), new ForespørselStatusRequest.Forespørsel(annetSaksnummer, ORGNR, ForespørselStatusRequest.YtelseType.FORELDREPENGER)));

        assertThatThrownBy(request::enesteFagsakSaksnummer).isInstanceOf(FunksjonellException.class);
    }

    private static ForespørselStatusRequest.Forespørsel gyldigForespørsel() {
        return new ForespørselStatusRequest.Forespørsel(SAKSNUMMER, ORGNR, ForespørselStatusRequest.YtelseType.FORELDREPENGER);
    }
}
