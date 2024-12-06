package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.DATE_FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;

public class HistorikkinnslagLinjeBuilderTest {
    private static final String ARBEIDSFORHOLDINFO = "DYNAMISK OPPSTEMT HAMSTER KF (311343483)";

    @Test
    void skal_lage_bold_på_tekst_tall_dato() {
        var verdi = 5000;
        var idag = LocalDate.now();
        var tekst = new HistorikkinnslagLinjeBuilder().bold(ARBEIDSFORHOLDINFO).tilTekst();
        var tall = new HistorikkinnslagLinjeBuilder().bold(verdi).tilTekst();
        var dato = new HistorikkinnslagLinjeBuilder().bold(idag).tilTekst();

        assertThat(tekst).isEqualTo("__" + ARBEIDSFORHOLDINFO + "__");
        assertThat(tall).isEqualTo("__" + verdi + "__");
        assertThat(dato).isEqualTo("__" + DATE_FORMATTER.format(idag) + "__");
    }

    @Test
    void skal_lage_beløp_fra_til() {
        var fra = 20000;
        var til = 35000;
        var hva = "Frilansinntekt";
        var tall = new HistorikkinnslagLinjeBuilder().fraTil(hva, fra, til).tilTekst();

        assertThat(tall).isEqualTo("__" + hva + "__ er endret fra " + fra + " til __" + til + "__");
    }

    @Test
    void skal_lage_kodeverdi_fra_til() {
        var fra = OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
        var til = OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
        var hva = "Stønadskontotype";
        var forventet = new HistorikkinnslagLinjeBuilder().fraTil(hva, fra, til).tilTekst();

        assertThat(forventet).isEqualTo("__" + hva + "__ er endret fra " + fra.getNavn() + " til __" + til.getNavn() + "__");
    }

    @Test
    void skal_lage_boolean_fra_til() {
        var hva = "Mor mottar uføretrygd";
        var forventet = new HistorikkinnslagLinjeBuilder().fraTil(hva, true, false).tilTekst();

        assertThat(forventet).isEqualTo("__" + hva + "__ er endret fra Ja til __Nei__");
    }

    @Test
    void skal_lage_dato_til() {
        var hva = "Innflyttingsdato";
        var til = LocalDate.now();
        var forventet = new HistorikkinnslagLinjeBuilder().fraTil(hva, null, til).tilTekst();

        assertThat(forventet).isEqualTo("__" + hva + "__ er satt til __" + DATE_FORMATTER.format(til) + "__");
    }

    @Test
    void skal_kaste_exception_hvis_fra_og_til_er_like() {
        var hva = "Frilansinntekt";
        var tekst = "Frilanser";

        assertThrows(IllegalArgumentException.class, () -> new HistorikkinnslagLinjeBuilder().fraTil(hva, tekst, tekst).tilTekst());
    }

    @Test
    void skal_kaste_exception_ved_kombinasjon_av_linjeskift_og_tekst_i_builder() {
        var linjeskift = HistorikkinnslagLinjeBuilder.LINJESKIFT;
        linjeskift.tekst("Dette er en test");
        assertThrows(IllegalStateException.class, linjeskift::tilTekst);
    }

}
