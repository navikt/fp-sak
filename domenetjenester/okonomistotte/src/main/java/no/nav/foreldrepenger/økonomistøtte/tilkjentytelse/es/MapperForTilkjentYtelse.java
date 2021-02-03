package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse.es;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseAndelV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

public class MapperForTilkjentYtelse {

    private MapperForTilkjentYtelse() {
        //hindrer instansiering, som gjør sonarqube glad
    }

    public static List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(LegacyESBeregning resultat, LocalDate vedtaksdato) {
        TilkjentYtelseAndelV1 andel = TilkjentYtelseAndelV1.tilBruker(
            TilkjentYtelseV1.Inntektskategori.IKKE_RELEVANT,
            resultat.getBeregnetTilkjentYtelse(),
            TilkjentYtelseV1.SatsType.ENGANGSUTBETALING);
        andel.setUtbetalingsgrad(BigDecimal.valueOf(100));
        TilkjentYtelsePeriodeV1 periode = new TilkjentYtelsePeriodeV1(vedtaksdato, vedtaksdato, Collections.singleton(andel));
        return Collections.singletonList(periode);
    }
}
