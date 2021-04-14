package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

public class MapperForTilkjentYtelseTest {

    @Test
    public void skal_mappe_beregningsresulat_es() {
        long satsVerdi = 70000;
        long antallBarn = 2;
        long beregnetTilkjentYtelse = 140000;
        var beregnetTidspunkt = LocalDateTime.now();
        var overstyrt = false;
        Long opprinneligBeregnetTilkjentYtelse = null;

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();

        var beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(satsVerdi, antallBarn, beregnetTilkjentYtelse, beregnetTidspunkt, overstyrt,
                        opprinneligBeregnetTilkjentYtelse))
                .buildFor(behandling, null);

        var vedtaksdato = LocalDate.of(2018, 5, 4);
        var ty = MapperForTilkjentYtelse.mapTilkjentYtelse(beregningResultat.getSisteBeregning().get(), vedtaksdato);
        assertThat(ty).hasSize(1);
        var periode = ty.get(0);
        assertThat(periode.getFom()).isEqualTo(vedtaksdato);
        assertThat(periode.getTom()).isEqualTo(vedtaksdato);
        assertThat(periode.getAndeler()).hasSize(1);
        var andel = periode.getAndeler().iterator().next();
        assertThat(andel.getUtbetalesTilBruker()).isTrue();
        assertThat(andel.getSatsBeløp()).isEqualTo(beregnetTilkjentYtelse);
        assertThat(andel.getSatsType()).isEqualTo(TilkjentYtelseV1.SatsType.ENGANGSUTBETALING);
        assertThat(andel.getInntektskategori()).isEqualTo(TilkjentYtelseV1.Inntektskategori.IKKE_RELEVANT);
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100));
    }

}
