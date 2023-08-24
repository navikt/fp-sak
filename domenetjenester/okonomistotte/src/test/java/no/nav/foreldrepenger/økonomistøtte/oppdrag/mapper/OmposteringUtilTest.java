package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.*;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OmposteringUtilTest {

    LocalDate nå = LocalDate.of(2020, 11, 23);
    Periode p1 = Periode.of(nå, nå.plusDays(5));

    @Test
    @DisplayName("Skal returnere true hvis ikke alle kjeder er opphørt. Her er det bare feriepenger som har opphørt, men ikke foreldrepenger.")
    void harGjeldendeUtbetalingerFraTidligere_feriepenger_opphørt() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFp2021Bruker = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2021);

        var feriepengerFom = LocalDate.of(2021, 5, 1);
        var feriepengerPeriode = Periode.of(feriepengerFom, LocalDate.of(2021, 5, 31));
        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(
            nøkkelBruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
                .build(),
            nøkkelFp2021Bruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(feriepengerPeriode).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(feriepengerPeriode).medOpphørFomDato(feriepengerFom).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .build()));

        var resultat = OmposteringUtil.harGjeldendeUtbetalingerFraTidligere(tidligereOppdrag.filter(Betalingsmottaker.BRUKER));

        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("Skal returnere false hvis alle kjeder for mottaker er opphørt.")
    void harGjeldendeUtbetalingerFraTidligere_ikke_noe_utbetalinger_lenger() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFp2021Bruker = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2021);

        var fp1 = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));
        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(
            nøkkelBruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).medOpphørFomDato(p1.getFom()).build())
                .build(),
            nøkkelFp2021Bruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(fp1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(fp1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).medOpphørFomDato(
                    fp1.getFom()).build())
                .build()));

        var resultat = OmposteringUtil.harGjeldendeUtbetalingerFraTidligere(tidligereOppdrag.filter(Betalingsmottaker.BRUKER));

        assertThat(resultat).isFalse();
    }

    @Test
    @DisplayName("Skal returnere true hvis alle kjeder for mottaker er opphørt.")
    void erOpphørForMottaker_full_opphør() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFp2021Bruker = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2021);

        var fp1 = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));
        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(
            nøkkelBruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).medOpphørFomDato(p1.getFom()).build())
                .build(),
            nøkkelFp2021Bruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(fp1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(fp1).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).medOpphørFomDato(
                    fp1.getFom()).build())
                .build()));

        var resultat = OmposteringUtil.erOpphørForMottaker(tidligereOppdrag.filter(Betalingsmottaker.BRUKER));

        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("Skal returnere false hvis ikke alle kjeder er opphørt. Her er det bare feriepenger som har opphørt, men ikke foreldrepenger.")
    void erOpphørForMottaker_kun_feriepenger_opphør() {
        var nøkkelBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFp2021Bruker = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2021);

        var feriepengerFom = LocalDate.of(2021, 5, 1);
        var feriepengerPeriode = Periode.of(feriepengerFom, LocalDate.of(2021, 5, 31));
        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(
            nøkkelBruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(p1).medSats(Satsen.dagsats(100)).medDelytelseId(DelytelseId.parse("FooBAR-1-1")).build())
                .build(),
            nøkkelFp2021Bruker, OppdragKjede.builder()
                .medOppdragslinje(OppdragLinje.builder().medPeriode(feriepengerPeriode).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .medOppdragslinje(OppdragLinje.builder().medPeriode(feriepengerPeriode).medOpphørFomDato(feriepengerFom).medSats(Satsen.dagsats(1000)).medDelytelseId(DelytelseId.parse("Feriep-1-1")).build())
                .build()));

        var resultat = OmposteringUtil.erOpphørForMottaker(tidligereOppdrag.filter(Betalingsmottaker.BRUKER));

        assertThat(resultat).isFalse();
    }
}
