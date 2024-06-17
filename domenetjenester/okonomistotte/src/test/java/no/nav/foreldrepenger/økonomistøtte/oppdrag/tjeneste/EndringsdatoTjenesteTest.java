package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;

class EndringsdatoTjenesteTest {

    LocalDate nå = LocalDate.of(2020, 11, 23);
    Periode p1 = Periode.of(nå, nå.plusDays(5));
    Periode p2 = Periode.of(nå.plusDays(6), nå.plusDays(10));
    Periode p3 = Periode.of(nå.plusDays(11), nå.plusDays(11));

    Periode p2Start = Periode.of(p2.getFom(), p2.getTom().minusDays(2));
    Periode p2Slutt = Periode.of(p2Start.getTom().plusDays(1), p2.getTom());

    @Test
    void skal_ikke_finne_endringsdato_ved_likhet() {
        var y0 = Ytelse.builder().build();
        var y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();
        var y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2Start, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p2Slutt, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y0, y0)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y1)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y2)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isNull();
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isNull();
    }

    @Test
    void skal_finne_endringsdato_i_start_av_periode() {
        var y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();
        var y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1300)))
            .build();

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p3.getFom());
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p3.getFom());
    }

    @Test
    void skal_finne_endringsdato_i_starten() {
        var y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();
        var y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1300)))
            .build();

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p1.getFom());
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p1.getFom());
    }

    @Test
    void skal_finne_endringsdato_i_periode() {
        var y1 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1200)))
            .build();
        var y2 = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(1000)))
            .leggTilPeriode(new YtelsePeriode(p2Start, Satsen.dagsats(1100)))
            .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(1300)))
            .build();

        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y1, y2)).isEqualTo(p2Start.getTom().plusDays(1));
        Assertions.assertThat(EndringsdatoTjeneste.normal().finnEndringsdato(y2, y1)).isEqualTo(p2Start.getTom().plusDays(1));
    }

    @Test
    void skal_ignorere_helger_for_satstype_dagsats() {
        var forrigeSøndag = LocalDate.of(2020, 11, 22);
        var mandag = forrigeSøndag.plusDays(1);
        var fredag = mandag.plusDays(4);
        var nesteMandag = mandag.plusDays(7);

        var ytelseKontinuerlig = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(mandag, fredag), Satsen.dagsats(100)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(nesteMandag, nesteMandag), Satsen.dagsats(100)))
            .build();
        var ytelseSplittet = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, nesteMandag), Satsen.dagsats(100))).build();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseKontinuerlig, ytelseSplittet)).isNull();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseSplittet, ytelseKontinuerlig)).isNull();
    }

    @Test
    void skal_finne_differanse_ved_endringer_i_dagytelse_i_tilknytning_til_helg() {
        var forrigeSøndag = LocalDate.of(2020, 11, 22);
        var mandag = forrigeSøndag.plusDays(1);
        var lørdag = mandag.plusDays(5);
        var søndag = mandag.plusDays(6);
        var nesteMandag = mandag.plusDays(7);

        var ytelse1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, mandag), Satsen.dagsats(100))).build();

        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(Ytelse.EMPTY, ytelse1)).isEqualTo(mandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse1, Ytelse.EMPTY)).isEqualTo(mandag);

        var ytelse1SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Satsen.dagsats(100))).build();
        var ytelse2SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Satsen.dagsats(200))).build();
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse1SøndagSøndag, ytelse2SøndagSøndag)).isEqualTo(mandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelse2SøndagSøndag, ytelse1SøndagSøndag)).isEqualTo(mandag);

        var ytelseUke1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, lørdag), Satsen.dagsats(100))).build();
        var ytelseUke1OgNesteMandag = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(mandag, nesteMandag), Satsen.dagsats(100)))
            .build();

        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseUke1, ytelseUke1OgNesteMandag)).isEqualTo(nesteMandag);
        assertThat(EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdato(ytelseUke1OgNesteMandag, ytelseUke1)).isEqualTo(nesteMandag);
    }

    @Test
    void skal_ikke_finne_noen_endringsdato_når_det_ikke_er_noen_endringer() {
        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Collections.emptyMap());
        var målbilde = GruppertYtelse.TOM;
        var tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);
        Assertions.assertThat(tidligsteEndringsdato).isNull();
    }

    @Test
    void skal_finne_tidligste_endringsdato_på_tvers_av_oppdrag() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("000000000"));

        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Collections.emptyMap());

        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelBruker, Ytelse.builder().leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(100))).build())
            .leggTilKjede(nøkkelArbeidsgiver, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(100)))
                .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(100)))
                .build())
            .build();

        var tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);

        Assertions.assertThat(tidligsteEndringsdato).isEqualTo(p1.getFom());
    }

    @Test
    void skal_finne_tidligste_endringsdato_på_tvers_av_oppdrag_for_revurdering() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("000000000"));

        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(nøkkelBruker, OppdragKjede.builder()
            .medOppdragslinje(
                OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
            .build()));

        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelBruker, Ytelse.builder().leggTilPeriode(new YtelsePeriode(p1, Satsen.dagsats(100))).build())
            .leggTilKjede(nøkkelArbeidsgiver, Ytelse.builder()
                .leggTilPeriode(new YtelsePeriode(p2, Satsen.dagsats(100)))
                .leggTilPeriode(new YtelsePeriode(p3, Satsen.dagsats(100)))
                .build())
            .build();

        var tidligsteEndringsdato = EndringsdatoTjeneste.normal().finnTidligsteEndringsdato(målbilde, tidligereOppdrag);

        Assertions.assertThat(tidligsteEndringsdato).isEqualTo(p2.getFom());
    }
}
