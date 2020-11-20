package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Sats;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;

public class EndringsdatoTjenesteTest {

    LocalDate nå = LocalDate.now();
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Periode p2Start = Periode.of(p2.getFom(), p2.getTom().minusDays(2));
    Periode p2Slutt = Periode.of(p2Start.getTom().plusDays(1), p2.getTom());

    EndringsdatoTjeneste tjeneste = new EndringsdatoTjeneste();

    @Test
    public void skal_ikke_finne_endringsdato_ved_likhet() {
        Ytelse y0 = Ytelse.builder().build();
        Ytelse y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();
        Ytelse y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2Start, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p2Slutt, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();

        Assertions.assertThat(tjeneste.finnEndringsdato(y0, y0)).isNull();
        Assertions.assertThat(tjeneste.finnEndringsdato(y1, y1)).isNull();
        Assertions.assertThat(tjeneste.finnEndringsdato(y2, y2)).isNull();
        Assertions.assertThat(tjeneste.finnEndringsdato(y1, y2)).isNull();
        Assertions.assertThat(tjeneste.finnEndringsdato(y2, y1)).isNull();
    }

    @Test
    public void skal_finne_endringsdato_i_start_av_periode() {
        Ytelse y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();
        Ytelse y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1300)))
            .build();

        Assertions.assertThat(tjeneste.finnEndringsdato(y1, y2)).isEqualTo(p3.getFom());
        Assertions.assertThat(tjeneste.finnEndringsdato(y2, y1)).isEqualTo(p3.getFom());
    }

    @Test
    public void skal_finne_endringsdato_i_starten() {
        Ytelse y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();
        Ytelse y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1300)))
            .build();

        Assertions.assertThat(tjeneste.finnEndringsdato(y1, y2)).isEqualTo(p1.getFom());
        Assertions.assertThat(tjeneste.finnEndringsdato(y2, y1)).isEqualTo(p1.getFom());
    }

    @Test
    public void skal_finne_endringsdato_i_periode() {
        Ytelse y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1200)))
            .build();
        Ytelse y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Sats.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2Start, Sats.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Sats.dagsats(1300)))
            .build();

        Assertions.assertThat(tjeneste.finnEndringsdato(y1, y2)).isEqualTo(p2Start.getTom().plusDays(1));
        Assertions.assertThat(tjeneste.finnEndringsdato(y2, y1)).isEqualTo(p2Start.getTom().plusDays(1));
    }
}
