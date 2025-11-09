package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;


class LagOppdragTjenesteTest {
    Saksnummer saksnummer = new Saksnummer("FooBAR");
    Long enBehandlingId = 1000001L;
    boolean vanligInntrekkbeslutning = true;
    LocalDate dag1 = LocalDate.of(2020, 11, 9);
    Periode periode = Periode.of(dag1, dag1.plusDays(3));
    Periode nesteMai = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));
    String brukerFnr = "33333333333";
    LocalDate vedtaksdato = dag1;

    @Test
    void skal_få_ingen_oppdrag_for_tomt_førstegangsvedtak() {
        var resultat = LagOppdragTjeneste.lagOppdrag(lagTomInput());
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_selvstendig_næringsdrivende() {
        var tilkjentYtelse = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        var tyPeriode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(periode.getFom(), periode.getTom()).build(tilkjentYtelse);
        BeregningsresultatAndel.builder().medBrukerErMottaker(true)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medDagsatsFraBg(1000).medDagsats(1000).medUtbetalingsgrad(BigDecimal.valueOf(100)).medStillingsprosent(BigDecimal.valueOf(100))
            .build(tyPeriode);

        var tilkjentYtelseMapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = tilkjentYtelseMapper.fordelPåNøkler(tilkjentYtelse);

        var input = lagInput(FagsakYtelseType.FORELDREPENGER, gruppertYtelse);
        //act
        var resultat = LagOppdragTjeneste.lagOppdrag(input);
        //assert
        assertThat(resultat).hasSize(1);
        var oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getFagsystemId().getSaksnummer()).isEqualTo(saksnummer.getVerdi());
        assertThat(oppdrag.getKodeFagområde()).isEqualTo(KodeFagområde.FP);

        var nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_SELVSTENDIG, Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getKjeder()).containsOnlyKeys(nøkkelYtelse);

        var kjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-100")).medPeriode(periode).medSats(Satsen.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());
    }

    @Test
    void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_arbeidstaker_og_får_feriepenger() {
        var tilkjentYtelse = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        var tyPeriode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(periode.getFom(), periode.getTom()).build(tilkjentYtelse);
        var andel = BeregningsresultatAndel.builder().medBrukerErMottaker(true).medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medInntektskategori(Inntektskategori.ARBEIDSTAKER).medDagsatsFraBg(1000).medDagsats(1000).medUtbetalingsgrad(BigDecimal.valueOf(100)).medStillingsprosent(BigDecimal.valueOf(100)).build(tyPeriode);
        var feriepenger = BeregningsresultatFeriepenger.builder().medFeriepengerRegelInput("foo").medFeriepengerRegelSporing("bar").medFeriepengerPeriodeFom(nesteMai.getFom()).medFeriepengerPeriodeTom(nesteMai.getTom()).build();
        BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(andel.getAktivitetStatus()).medBrukerErMottaker(andel.erBrukerMottaker())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null)).medArbeidsforholdRef(andel.getArbeidsforholdRef())
            .medÅrsbeløp(200).medOpptjeningsår(dag1.getYear()).build(feriepenger);

        var grunnlag = BehandlingBeregningsresultatBuilder.ny().medBgBeregningsresultatFP(tilkjentYtelse)
            .medBeregningsresultatFeriepenger(feriepenger).build(1L);

        var tilkjentYtelseMapper = TilkjentYtelseMapper.lagFor(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = tilkjentYtelseMapper.fordelPåNøkler(grunnlag);

        var input = lagInput(FagsakYtelseType.FORELDREPENGER, gruppertYtelse);
        //act
        var resultat = LagOppdragTjeneste.lagOppdrag(input);
        //assert
        assertThat(resultat).hasSize(1);
        var oppdrag = resultat.get(0);

        var nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        var nøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.FERIEPENGER_BRUKER, Betalingsmottaker.BRUKER, 2020);
        assertThat(oppdrag.getKjeder()).containsOnlyKeys(nøkkelYtelse, nøkkelFeriepenger);

        var ytelsekjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(ytelsekjede.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-100")).medPeriode(periode).medSats(Satsen.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        var feriepengeKjede = oppdrag.getKjeder().get(nøkkelFeriepenger);
        assertThat(feriepengeKjede.getOppdragslinjer()).hasSize(1);
        assertLik(feriepengeKjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("100-101")).medPeriode(nesteMai).medSats(Satsen.engang(200)).build());
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

    OppdragInput lagTomInput() {
        var tomtResultat = GruppertYtelse.builder().build();
        return lagInput(FagsakYtelseType.FORELDREPENGER, tomtResultat);
    }

    OppdragInput lagInput(FagsakYtelseType ytelseType, GruppertYtelse tilkjentYtelse) {
        return OppdragInput.builder()
            .medFagsakYtelseType(ytelseType)
            .medTilkjentYtelse(tilkjentYtelse)
            .medTidligereOppdrag(OverordnetOppdragKjedeOversikt.TOM)
            .medSaksnummer(saksnummer)
            .medBehandlingId(enBehandlingId)
            .medBrukInntrekk(vanligInntrekkbeslutning)
            .medVedtaksdato(vedtaksdato)
            .medBrukerFnr(brukerFnr)
            .build();
    }

}
