package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste.oppdaterFrilansInntektVedEndretVerdi;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjenesteTest {

    @Test
    void lik_verdi_frilansinntekt() {
        var builder = oppdaterFrilansInntektVedEndretVerdi(BigDecimal.valueOf(1000), 1000);
        assertThat(builder).hasSize(2);
        assertThat(builder.getFirst().tilTekst()).isEqualTo("__Frilansinntekt__ er satt til __1000__");
    }

    @Test
    void ulik_verdi_frilansinntekt() {
        var builder = oppdaterFrilansInntektVedEndretVerdi(BigDecimal.valueOf(1000), 2000);
        assertThat(builder).hasSize(2);
        assertThat(builder.getFirst().tilTekst()).isEqualTo("__Frilansinntekt__ er endret fra 1000 til __2000__");
    }
}
