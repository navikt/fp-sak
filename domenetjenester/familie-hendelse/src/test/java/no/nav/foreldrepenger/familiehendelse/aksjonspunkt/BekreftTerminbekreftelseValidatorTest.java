package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.BeforeClass;
import org.junit.Test;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;

public class BekreftTerminbekreftelseValidatorTest {

    private static Period tidlistUtstedelseAvTerminBekreftelse = Period.parse("P18W3D");
    private static BekreftTerminbekreftelseValidator validator;

    @BeforeClass
    public static void setup() {
        validator = new BekreftTerminbekreftelseValidator(tidlistUtstedelseAvTerminBekreftelse);
    }

    @Test
    public void skal_ikke_validare_ok_når_utstedtdato_er_før_22_svangerskapsuke() {
        LocalDate utstedtdato = LocalDate.now().minusWeeks(18).minusDays(4);
        LocalDate termindato = LocalDate.now();
        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);
        boolean feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isTrue();

    }

    @Test
    public void skal_validare_ok_når_utstedtdato_er_10_uker_og_2_dager_før_termindato() {
        LocalDate utstedtdato = LocalDate.now().minusWeeks(10).minusDays(2);
        LocalDate termindato = LocalDate.now();
        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);

        boolean feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isFalse();

    }

    @Test
    public void skal_validare_ok_når_utstedtdato_er_10_uker_og_1_dager_før_termindato() {
        LocalDate utstedtdato = LocalDate.now().minusWeeks(10).minusDays(1);
        LocalDate termindato = LocalDate.now();
        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto("begrunnelse", termindato, utstedtdato,
            1);

        boolean feltFeil = validator.validerUtstedtdato(dto);
        assertThat(feltFeil).isFalse();
    }
}
