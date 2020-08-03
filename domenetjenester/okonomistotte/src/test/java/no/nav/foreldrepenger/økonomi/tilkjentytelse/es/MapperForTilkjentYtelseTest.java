package no.nav.foreldrepenger.økonomi.tilkjentytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseAndelV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

public class MapperForTilkjentYtelseTest {

    @Test
    public void skal_mappe_beregningsresulat_es() {
        long satsVerdi = 70000;
        long antallBarn = 2;
        long beregnetTilkjentYtelse = 140000;
        LocalDateTime beregnetTidspunkt = LocalDateTime.now();
        boolean overstyrt = false;
        Long opprinneligBeregnetTilkjentYtelse = null;

        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();

        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
            .medBeregning(new LegacyESBeregning(satsVerdi, antallBarn, beregnetTilkjentYtelse, beregnetTidspunkt, overstyrt, opprinneligBeregnetTilkjentYtelse))
            .buildFor(behandling, null);

        LocalDate vedtaksdato = LocalDate.of(2018, 5, 4);
        List<TilkjentYtelsePeriodeV1> ty = MapperForTilkjentYtelse.mapTilkjentYtelse(beregningResultat.getSisteBeregning().get(), vedtaksdato);
        assertThat(ty).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = ty.get(0);
        assertThat(periode.getFom()).isEqualTo(vedtaksdato);
        assertThat(periode.getTom()).isEqualTo(vedtaksdato);
        assertThat(periode.getAndeler()).hasSize(1);
        TilkjentYtelseAndelV1 andel = periode.getAndeler().iterator().next();
        assertThat(andel.getUtbetalesTilBruker()).isTrue();
        assertThat(andel.getSatsBeløp()).isEqualTo(beregnetTilkjentYtelse);
        assertThat(andel.getSatsType()).isEqualTo(TilkjentYtelseV1.SatsType.ENGANGSUTBETALING);
        assertThat(andel.getInntektskategori()).isEqualTo(TilkjentYtelseV1.Inntektskategori.IKKE_RELEVANT);
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100));
    }

}
