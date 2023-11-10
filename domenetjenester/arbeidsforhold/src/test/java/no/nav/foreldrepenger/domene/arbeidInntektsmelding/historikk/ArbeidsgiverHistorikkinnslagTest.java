package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

class ArbeidsgiverHistorikkinnslagTest {

    @Test
    void tester_virksomhet_uten_ekstern_ref() {
        var opplysninger = new ArbeidsgiverOpplysninger("999999999", "Bedrift A/S");

        var tekst = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, Optional.empty());

        assertThat(tekst).isEqualTo("Bedrift A/S (999999999)");
    }

    @Test
    void tester_virksomhet_med_ekstern_ref() {
        var opplysninger = new ArbeidsgiverOpplysninger("999999999", "Bedrift A/S");

        var tekst = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, Optional.of(EksternArbeidsforholdRef.ref("ARB-0001")));

        assertThat(tekst).isEqualTo("Bedrift A/S (999999999) ...0001");
    }

    @Test
    void tester_privatperson_med_ekstern_ref() {
        var opplysninger = new ArbeidsgiverOpplysninger(new AktørId("9999999999999"), "12.12.1992", "Borghild", LocalDate.of(1992,12,12));

        var tekst = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, Optional.of(EksternArbeidsforholdRef.ref("ARB-0001")));

        assertThat(tekst).isEqualTo("Borghild (12.12.1992) ...0001");
    }

    @Test
    void tester_privatperson_med_kort_ekstern_ref() {
        var opplysninger = new ArbeidsgiverOpplysninger(new AktørId("9999999999999"), "12.12.1992", "Borghild", LocalDate.of(1992,12,12));

        var tekst = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, Optional.of(EksternArbeidsforholdRef.ref("1")));

        assertThat(tekst).isEqualTo("Borghild (12.12.1992) ...1");
    }

}
