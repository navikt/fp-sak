package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.FagområdeMapper;

class OppdragFactoryTest {

    Saksnummer saksnummer = new Saksnummer("FooBAR");
    LocalDate dag1 = LocalDate.of(2020, 11, 9);
    Periode periode = Periode.of(dag1, dag1.plusDays(3));
    Periode nesteMai = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));

    OppdragFactory oppdragFactory = new OppdragFactory(FagområdeMapper::tilFagområde, FagsakYtelseType.FORELDREPENGER, saksnummer);

    @Test
    void skal_få_ingen_oppdrag_for_tomt_førstegangsvedtak() {
        var resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, GruppertYtelse.TOM);
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_selvstendig_næringsdrivende() {
        var nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_SELVSTENDIG, Betalingsmottaker.BRUKER);
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelse, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .build();

        //act
        var resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(1);
        var oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getFagsystemId().getSaksnummer()).isEqualTo(saksnummer.getVerdi());
        assertThat(oppdrag.getKodeFagområde()).isEqualTo(KodeFagområde.FP);

        assertThat(oppdrag.getKjeder()).containsOnlyKeys(nøkkelYtelse);

        var kjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-100")).medPeriode(periode).medSats(Satsen.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());
    }

    @Test
    void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_arbeidstaker_og_får_feriepenger() {
        var nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2020);
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelse, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .leggTilKjede(nøkkelFeriepenger, Ytelse.builder().leggTilPeriode(new YtelsePeriode(nesteMai, Satsen.engang(200))).build())
            .build();
        //act
        var resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(1);
        var oppdrag = resultat.get(0);

        assertThat(oppdrag.getKjeder()).containsOnlyKeys(nøkkelYtelse, nøkkelFeriepenger);

        var ytelsekjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(ytelsekjede.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-100")).medPeriode(periode).medSats(Satsen.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        var feriepengeKjede = oppdrag.getKjeder().get(nøkkelFeriepenger);
        assertThat(feriepengeKjede.getOppdragslinjer()).hasSize(1);
        assertLik(feriepengeKjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-101")).medPeriode(nesteMai).medSats(Satsen.engang(200)).build());
    }

    @Test
    void skal_lage_ett_oppdrag_til_hver_mottaker() {
        var nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelYtelseTilArbeidsgiver1 = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("000000001"));
        var nøkkelYtelseTilArbeidsgiver2 = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("000000002"));
        var nøkkelYtelseTilArbeidsgiver3 = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("000000003"));
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver1, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(101))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver2, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(102))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver3, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Satsen.dagsats(103))).build())
            .build();
        //act
        var resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(4);
        var ytelsekjedeBruker = resultat.get(0).getKjeder().get(nøkkelYtelseTilBruker);
        assertThat(ytelsekjedeBruker.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeBruker.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-100")).medPeriode(periode).medSats(Satsen.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        var ytelsekjedeArbeidsgiver1 = resultat.get(1).getKjeder().get(nøkkelYtelseTilArbeidsgiver1);
        assertThat(ytelsekjedeArbeidsgiver1.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver1.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("101-100")).medPeriode(periode).medSats(Satsen.dagsats(101)).build());

        var ytelsekjedeArbeidsgiver2 = resultat.get(2).getKjeder().get(nøkkelYtelseTilArbeidsgiver2);
        assertThat(ytelsekjedeArbeidsgiver2.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver2.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("102-100")).medPeriode(periode).medSats(Satsen.dagsats(102)).build());

        var ytelsekjedeArbeidsgiver3 = resultat.get(3).getKjeder().get(nøkkelYtelseTilArbeidsgiver3);
        assertThat(ytelsekjedeArbeidsgiver3.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver3.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("103-100")).medPeriode(periode).medSats(Satsen.dagsats(103)).build());
    }

    @Test
    void skal_bare_lage_oppdrag_for_mottaker_med_endring() {
        var arbeidsgiver = Betalingsmottaker.forArbeidsgiver("000000001");
        var nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelYtelseTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, arbeidsgiver);
        var brukersYtelsePeriode = new YtelsePeriode(this.periode, Satsen.dagsats(1000), Utbetalingsgrad.prosent(100));
        var arbeidsgiversYtelsePeriode = new YtelsePeriode(this.periode, Satsen.dagsats(101));
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(brukersYtelsePeriode).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver, Ytelse.builder().leggTilPeriode(arbeidsgiversYtelsePeriode).build())
            .build();

        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(nøkkelYtelseTilBruker, OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-101-101")).medYtelsePeriode(brukersYtelsePeriode).build())
            .build()));

        var resultat = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);

        assertThat(resultat).hasSize(1);
        var oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(arbeidsgiver);
        assertThat(oppdrag.getKjeder()).containsOnlyKeys(nøkkelYtelseTilArbeidsgiver);
        var kjede = oppdrag.getKjeder().get(nøkkelYtelseTilArbeidsgiver);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(arbeidsgiversYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-102-100")).build());
    }

    @Test
    void skal_bare_lage_oppdrag_for_alle_mottakere_når_det_er_satt_flagg_for_dette() {

        var arbeidsgiver = Betalingsmottaker.forArbeidsgiver("000000001");
        var nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelYtelseTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, arbeidsgiver);
        var brukersYtelsePeriode = new YtelsePeriode(this.periode, Satsen.dagsats(1000), Utbetalingsgrad.prosent(100));
        var arbeidsgiversYtelsePeriode = new YtelsePeriode(this.periode, Satsen.dagsats(101));
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(brukersYtelsePeriode).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver, Ytelse.builder().leggTilPeriode(arbeidsgiversYtelsePeriode).build())
            .build();

        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(nøkkelYtelseTilBruker, OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-101-101")).medYtelsePeriode(brukersYtelsePeriode).build())
            .build()));

        oppdragFactory.setFellesEndringstidspunkt(periode.getFom());
        var resultat = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);

        assertThat(resultat).hasSize(2);

        var oppdragBruker = resultat.get(0);
        assertThat(oppdragBruker.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdragBruker.getKjeder()).containsOnlyKeys(nøkkelYtelseTilBruker);
        var kjede = oppdragBruker.getKjeder().get(nøkkelYtelseTilBruker);
        assertThat(kjede.getOppdragslinjer()).hasSize(2);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(brukersYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-101-101")).medOpphørFomDato(periode.getFom()).build());
        assertLik(kjede.getOppdragslinjer().get(1), OppdragLinje.builder().medYtelsePeriode(brukersYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-101-102")).medRefDelytelseId(DelytelseId.parse("FooBar-101-101")).build());

        var oppdragArbeidsgiver = resultat.get(1);
        assertThat(oppdragArbeidsgiver.getBetalingsmottaker()).isEqualTo(arbeidsgiver);
        assertThat(oppdragArbeidsgiver.getKjeder()).containsOnlyKeys(nøkkelYtelseTilArbeidsgiver);
        var kjedeArbg = oppdragArbeidsgiver.getKjeder().get(nøkkelYtelseTilArbeidsgiver);
        assertThat(kjedeArbg.getOppdragslinjer()).hasSize(1);
        assertLik(kjedeArbg.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(arbeidsgiversYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-102-100")).build());
    }

    @Test
    void skal_ikke_endre_periode_for_feriepenger_hvis_felles_endringsdato_er_i_mai() {

        var ytelseFom = LocalDate.of(2020, 4, 1);
        var ytelseTom = LocalDate.of(2021, 6, 1);
        var fellesEndringsdato = LocalDate.of(2020, 5, 15);

        var arbeidsgiver = Betalingsmottaker.forArbeidsgiver("000000001");
        var nøkkelYtelseTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, arbeidsgiver);
        var nøkkelFeriepengerTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, arbeidsgiver, 2020);
        var arbeidsgiversYtelsePeriode1 = new YtelsePeriode(Periode.of(fellesEndringsdato, ytelseTom), Satsen.dagsats(102));
        var arbeidsgiversYtelsePeriode2 = new YtelsePeriode(Periode.of(ytelseFom, fellesEndringsdato.minusDays(1)), Satsen.dagsats(101));
        var arbeidsgiversFeriepengerPeriode = new YtelsePeriode(Periode.of(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)), Satsen.engang(101101));
        var målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelFeriepengerTilArbeidsgiver, Ytelse.builder().leggTilPeriode(arbeidsgiversFeriepengerPeriode).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver,
                Ytelse.builder()
                    .leggTilPeriode(arbeidsgiversYtelsePeriode1)
                    .leggTilPeriode(arbeidsgiversYtelsePeriode2).build())
            .build();

        var tidligereOppdrag = new OverordnetOppdragKjedeOversikt(
            Map.of(
                nøkkelYtelseTilArbeidsgiver, OppdragKjede.builder()
                    .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-101-101"))
                        .medYtelsePeriode(new YtelsePeriode(Periode.of(ytelseFom, ytelseTom), Satsen.dagsats(101))).build())
                    .build(),
                nøkkelFeriepengerTilArbeidsgiver, OppdragKjede.builder()
                    .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-101-102"))
                        .medYtelsePeriode(arbeidsgiversFeriepengerPeriode).build())
                    .build()
            ));

        oppdragFactory.setFellesEndringstidspunkt(fellesEndringsdato);
        var resultat = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);

        assertThat(resultat).hasSize(1);
    }

    private DelytelseId delytelseId(String suffix) {
        return DelytelseId.parse(saksnummer.getVerdi() + "-" + suffix);
    }

    private static void assertLik(OppdragLinje input, OppdragLinje fasit) {
        assertThat(input.getPeriode()).isEqualTo(fasit.getPeriode());
        assertThat(input.getOpphørFomDato()).isEqualTo(fasit.getOpphørFomDato());
        assertThat(input.getSats()).isEqualTo(fasit.getSats());
        assertThat(input.getUtbetalingsgrad()).isEqualTo(fasit.getUtbetalingsgrad());
        assertThat(input.getRefDelytelseId()).isEqualTo(fasit.getRefDelytelseId());
        assertThat(input.getDelytelseId()).isEqualTo(fasit.getDelytelseId());
    }

}
