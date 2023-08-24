package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

class BekreftTerminbekreftelseValidatorTest {

    private static Period tidlistUtstedelseAvTerminBekreftelse = Period.parse("P18W3D");
    private static BekreftTerminbekreftelseValidator validator;

    @BeforeAll
    public static void setup() {
        validator = new BekreftTerminbekreftelseValidator(tidlistUtstedelseAvTerminBekreftelse);
    }

    @Test
    void skal_ikke_validare_ok_når_utstedtdato_er_før_22_svangerskapsuke() {
        var utstedtdato = LocalDate.now().minusWeeks(18).minusDays(4);
        var termindato = LocalDate.now();
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);
        var feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isTrue();

    }

    @Test
    void skal_validare_ok_når_utstedtdato_er_10_uker_og_2_dager_før_termindato() {
        var utstedtdato = LocalDate.now().minusWeeks(10).minusDays(2);
        var termindato = LocalDate.now();
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);

        var feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isFalse();

    }

    @Test
    void skal_validare_ok_når_utstedtdato_er_10_uker_og_1_dager_før_termindato() {
        var utstedtdato = LocalDate.now().minusWeeks(10).minusDays(1);
        var termindato = LocalDate.now();
        var dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);

        var feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isFalse();
    }
}
