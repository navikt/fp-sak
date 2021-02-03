package no.nav.foreldrepenger.økonomistøtte.ny.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Sats;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;

public class EndringsdatoTjenesteTest {

    LocalDate nå = LocalDate.of(2020, 11, 23);
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Periode p2Start = Periode.of(p2.getFom(), p2.getTom().minusDays(2));
    Periode p2Slutt = Periode.of(p2Start.getTom().plusDays(1), p2.getTom());

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

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y0, y0)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y1)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y2)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isNull();
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

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p3.getFom());
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p3.getFom());
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

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p1.getFom());
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p1.getFom());
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

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p2Start.getTom().plusDays(1));
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p2Start.getTom().plusDays(1));
    }

    @Test
    public void skal_ignorere_helger_for_satstype_dagsats() {
        LocalDate forrigeSøndag = LocalDate.of(2020, 11, 22);
        LocalDate mandag = forrigeSøndag.plusDays(1);
        LocalDate fredag = mandag.plusDays(4);
        LocalDate nesteMandag = mandag.plusDays(7);

        Ytelse ytelseKontinuerlig = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(mandag, fredag), Sats.dagsats(100)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(nesteMandag, nesteMandag), Sats.dagsats(100)))
            .build();
        Ytelse ytelseSplittet = Ytelse.builder().leggTilPeriode(
            new YtelsePeriode(Periode.of(mandag, nesteMandag), Sats.dagsats(100)))
            .build();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseKontinuerlig, ytelseSplittet)).isNull();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseSplittet, ytelseKontinuerlig)).isNull();
    }

    @Test
    public void skal_finne_differanse_ved_endringer_i_dagytelse_i_tilknytning_til_helg() {
        LocalDate forrigeSøndag = LocalDate.of(2020, 11, 22);
        LocalDate mandag = forrigeSøndag.plusDays(1);
        LocalDate lørdag = mandag.plusDays(5);
        LocalDate søndag = mandag.plusDays(6);
        LocalDate nesteMandag = mandag.plusDays(7);

        Ytelse ytelse1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, mandag), Sats.dagsats(100))).build();

        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(Ytelse.EMPTY, ytelse1)).isEqualTo(mandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse1, Ytelse.EMPTY)).isEqualTo(mandag);

        Ytelse ytelse1SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Sats.dagsats(100))).build();
        Ytelse ytelse2SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Sats.dagsats(200))).build();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse1SøndagSøndag, ytelse2SøndagSøndag)).isEqualTo(mandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse2SøndagSøndag, ytelse1SøndagSøndag)).isEqualTo(mandag);

        Ytelse ytelseUke1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, lørdag), Sats.dagsats(100))).build();
        Ytelse ytelseUke1OgNesteMandag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, nesteMandag), Sats.dagsats(100))).build();

        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseUke1, ytelseUke1OgNesteMandag)).isEqualTo(nesteMandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseUke1OgNesteMandag, ytelseUke1)).isEqualTo(nesteMandag);
    }

    @Test
    public void skal_ikke_finne_noen_endringsdato_når_det_ikke_er_noen_endringer() {
        OverordnetOppdragKjedeOversikt tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Collections.emptyMap());
        GruppertYtelse målbilde = GruppertYtelse.TOM;
        LocalDate tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);
        Assertions.assertThat(tidligsteEndringsdato).isNull();
    }

    @Test
    public void skal_finne_tidligste_endringsdato_på_tvers_av_oppdrag() {
        KjedeNøkkel nøkkelBruker = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPATORD, Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelArbeidsgiver = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPREFAG_IOP, Betalingsmottaker.forArbeidsgiver("000000000"));

        OverordnetOppdragKjedeOversikt tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Collections.emptyMap());

        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelBruker, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p1, Sats.dag7(100)))
                .build())
            .leggTilKjede(nøkkelArbeidsgiver, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p2, Sats.dag7(100)))
                .leggTilPeriode(new YtelsePeriode(p3, Sats.dag7(100)))
                .build())
            .build();

        LocalDate tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);

        Assertions.assertThat(tidligsteEndringsdato).isEqualTo(p1.getFom());
    }

    @Test
    public void skal_finne_tidligste_endringsdato_på_tvers_av_oppdrag_for_revurdering() {
        KjedeNøkkel nøkkelBruker = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPATORD, Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelArbeidsgiver = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPREFAG_IOP, Betalingsmottaker.forArbeidsgiver("000000000"));

        OverordnetOppdragKjedeOversikt tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(
            nøkkelBruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Sats.dag7(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
                .build()));

        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelBruker, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p1, Sats.dag7(100)))
                .build())
            .leggTilKjede(nøkkelArbeidsgiver, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p2, Sats.dag7(100)))
                .leggTilPeriode(new YtelsePeriode(p3, Sats.dag7(100)))
                .build())
            .build();

        LocalDate tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);

        Assertions.assertThat(tidligsteEndringsdato).isEqualTo(p2.getFom());
    }
}
