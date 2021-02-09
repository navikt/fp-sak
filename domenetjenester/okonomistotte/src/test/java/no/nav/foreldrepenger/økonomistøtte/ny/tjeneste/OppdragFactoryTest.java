package no.nav.foreldrepenger.økonomistøtte.ny.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Sats;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.ØkonomiFagområdeMapper;

public class OppdragFactoryTest {

    Saksnummer saksnummer = new Saksnummer("FooBAR");
    LocalDate dag1 = LocalDate.of(2020, 11, 9);
    Periode periode = Periode.of(dag1, dag1.plusDays(3));
    Periode nesteMai = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));

    OppdragFactory oppdragFactory = new OppdragFactory(ØkonomiFagområdeMapper::tilFagområde, FagsakYtelseType.FORELDREPENGER, saksnummer);

    @Test
    public void skal_få_ingen_oppdrag_for_tomt_førstegangsvedtak() {
        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, GruppertYtelse.TOM);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_selvstendig_næringsdrivende() {
        KjedeNøkkel nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_SELVSTENDIG, Betalingsmottaker.BRUKER);
        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelse, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .build();

        //act
        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(1);
        Oppdrag oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getFagsystemId().getSaksnummer()).isEqualTo(saksnummer.getVerdi());
        assertThat(oppdrag.getØkonomiFagområde()).isEqualTo(ØkonomiKodeFagområde.FP);

        assertThat(oppdrag.getKjeder().keySet()).containsOnly(nøkkelYtelse);

        OppdragKjedeFortsettelse kjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-100")).medPeriode(periode).medSats(Sats.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());
    }

    @Test
    public void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_arbeidstaker_og_får_feriepenger() {
        KjedeNøkkel nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATORD"), Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATFER"), Betalingsmottaker.BRUKER, 2020);
        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelse, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .leggTilKjede(nøkkelFeriepenger, Ytelse.builder().leggTilPeriode(new YtelsePeriode(nesteMai, Sats.engang(200))).build())
            .build();
        //act
        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(1);
        Oppdrag oppdrag = resultat.get(0);

        assertThat(oppdrag.getKjeder().keySet()).containsOnly(nøkkelYtelse, nøkkelFeriepenger);

        OppdragKjedeFortsettelse ytelsekjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(ytelsekjede.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-100")).medPeriode(periode).medSats(Sats.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        OppdragKjedeFortsettelse feriepengeKjede = oppdrag.getKjeder().get(nøkkelFeriepenger);
        assertThat(feriepengeKjede.getOppdragslinjer()).hasSize(1);
        assertLik(feriepengeKjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-101")).medPeriode(nesteMai).medSats(Sats.engang(200)).build());
    }

    @Test
    public void skal_lage_ett_oppdrag_til_hver_mottaker() {
        KjedeNøkkel nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATORD"), Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelYtelseTilArbeidsgiver1 = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPREFAG-IOP"), Betalingsmottaker.forArbeidsgiver("000000001"));
        KjedeNøkkel nøkkelYtelseTilArbeidsgiver2 = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPREFAG-IOP"), Betalingsmottaker.forArbeidsgiver("000000002"));
        KjedeNøkkel nøkkelYtelseTilArbeidsgiver3 = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPREFAG-IOP"), Betalingsmottaker.forArbeidsgiver("000000003"));
        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(1000), Utbetalingsgrad.prosent(100))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver1, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(101))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver2, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(102))).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver3, Ytelse.builder().leggTilPeriode(new YtelsePeriode(periode, Sats.dagsats(103))).build())
            .build();
        //act
        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(OverordnetOppdragKjedeOversikt.TOM, målbilde);
        //assert
        assertThat(resultat).hasSize(4);
        OppdragKjedeFortsettelse ytelsekjedeBruker = resultat.get(0).getKjeder().get(nøkkelYtelseTilBruker);
        assertThat(ytelsekjedeBruker.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeBruker.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-100")).medPeriode(periode).medSats(Sats.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        OppdragKjedeFortsettelse ytelsekjedeArbeidsgiver1 = resultat.get(1).getKjeder().get(nøkkelYtelseTilArbeidsgiver1);
        assertThat(ytelsekjedeArbeidsgiver1.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver1.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("2-100")).medPeriode(periode).medSats(Sats.dagsats(101)).build());

        OppdragKjedeFortsettelse ytelsekjedeArbeidsgiver2 = resultat.get(2).getKjeder().get(nøkkelYtelseTilArbeidsgiver2);
        assertThat(ytelsekjedeArbeidsgiver2.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver2.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("3-100")).medPeriode(periode).medSats(Sats.dagsats(102)).build());

        OppdragKjedeFortsettelse ytelsekjedeArbeidsgiver3 = resultat.get(3).getKjeder().get(nøkkelYtelseTilArbeidsgiver3);
        assertThat(ytelsekjedeArbeidsgiver3.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjedeArbeidsgiver3.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("4-100")).medPeriode(periode).medSats(Sats.dagsats(103)).build());
    }

    @Test
    public void skal_bare_lage_oppdrag_for_mottaker_med_endring() {
        Betalingsmottaker arbeidsgiver = Betalingsmottaker.forArbeidsgiver("000000001");
        KjedeNøkkel nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATORD"), Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelYtelseTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPREFAG-IOP"), arbeidsgiver);
        YtelsePeriode brukersYtelsePeriode = new YtelsePeriode(this.periode, Sats.dagsats(1000), Utbetalingsgrad.prosent(100));
        YtelsePeriode arbeidsgiversYtelsePeriode = new YtelsePeriode(this.periode, Sats.dagsats(101));
        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(brukersYtelsePeriode).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver, Ytelse.builder().leggTilPeriode(arbeidsgiversYtelsePeriode).build())
            .build();

        OverordnetOppdragKjedeOversikt tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(nøkkelYtelseTilBruker, OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-1-1")).medYtelsePeriode(brukersYtelsePeriode).build())
            .build()));

        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);

        assertThat(resultat).hasSize(1);
        Oppdrag oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(arbeidsgiver);
        assertThat(oppdrag.getKjeder().keySet()).containsOnly(nøkkelYtelseTilArbeidsgiver);
        OppdragKjedeFortsettelse kjede = oppdrag.getKjeder().get(nøkkelYtelseTilArbeidsgiver);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(arbeidsgiversYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-2-100")).build());
    }

    @Test
    public void skal_bare_lage_oppdrag_for_alle_mottakere_når_det_er_satt_flagg_for_dette() {

        Betalingsmottaker arbeidsgiver = Betalingsmottaker.forArbeidsgiver("000000001");
        KjedeNøkkel nøkkelYtelseTilBruker = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATORD"), Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelYtelseTilArbeidsgiver = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPREFAG-IOP"), arbeidsgiver);
        YtelsePeriode brukersYtelsePeriode = new YtelsePeriode(this.periode, Sats.dagsats(1000), Utbetalingsgrad.prosent(100));
        YtelsePeriode arbeidsgiversYtelsePeriode = new YtelsePeriode(this.periode, Sats.dagsats(101));
        GruppertYtelse målbilde = GruppertYtelse.builder()
            .leggTilKjede(nøkkelYtelseTilBruker, Ytelse.builder().leggTilPeriode(brukersYtelsePeriode).build())
            .leggTilKjede(nøkkelYtelseTilArbeidsgiver, Ytelse.builder().leggTilPeriode(arbeidsgiversYtelsePeriode).build())
            .build();

        OverordnetOppdragKjedeOversikt tidligereOppdrag = new OverordnetOppdragKjedeOversikt(Map.of(nøkkelYtelseTilBruker, OppdragKjede.builder()
            .medOppdragslinje(OppdragLinje.builder().medDelytelseId(DelytelseId.parse("FooBar-1-1")).medYtelsePeriode(brukersYtelsePeriode).build())
            .build()));

        oppdragFactory.setFellesEndringstidspunkt(periode.getFom());
        List<Oppdrag> resultat = oppdragFactory.lagOppdrag(tidligereOppdrag, målbilde);

        assertThat(resultat).hasSize(2);

        Oppdrag oppdragBruker = resultat.get(0);
        assertThat(oppdragBruker.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdragBruker.getKjeder().keySet()).containsOnly(nøkkelYtelseTilBruker);
        OppdragKjedeFortsettelse kjede = oppdragBruker.getKjeder().get(nøkkelYtelseTilBruker);
        assertThat(kjede.getOppdragslinjer()).hasSize(2);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(brukersYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-1-1")).medOpphørFomDato(periode.getFom()).build());
        assertLik(kjede.getOppdragslinjer().get(1), OppdragLinje.builder().medYtelsePeriode(brukersYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-1-2")).medRefDelytelseId(DelytelseId.parse("FooBar-1-1")).build());

        Oppdrag oppdragArbeidsgiver = resultat.get(1);
        assertThat(oppdragArbeidsgiver.getBetalingsmottaker()).isEqualTo(arbeidsgiver);
        assertThat(oppdragArbeidsgiver.getKjeder().keySet()).containsOnly(nøkkelYtelseTilArbeidsgiver);
        OppdragKjedeFortsettelse kjedeArbg = oppdragArbeidsgiver.getKjeder().get(nøkkelYtelseTilArbeidsgiver);
        assertThat(kjedeArbg.getOppdragslinjer()).hasSize(1);
        assertLik(kjedeArbg.getOppdragslinjer().get(0), OppdragLinje.builder().medYtelsePeriode(arbeidsgiversYtelsePeriode).medDelytelseId(DelytelseId.parse("FooBar-2-100")).build());
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
