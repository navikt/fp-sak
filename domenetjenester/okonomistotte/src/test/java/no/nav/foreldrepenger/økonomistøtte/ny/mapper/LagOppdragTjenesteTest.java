package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.DelytelseId;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Sats;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Utbetalingsgrad;


public class LagOppdragTjenesteTest {
    Saksnummer saksnummer = new Saksnummer("FooBAR");
    Long enBehandlingId = 1000001L;
    boolean vanligInntrekkbeslutning = true;
    LocalDate dag1 = LocalDate.of(2020, 11, 9);
    Periode periode = Periode.of(dag1, dag1.plusDays(3));
    Periode nesteMai = Periode.of(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 31));
    AktørId brukerAktørId = AktørId.dummy();
    LocalDate vedtaksdato = dag1;

    @Test
    public void skal_få_ingen_oppdrag_for_tomt_førstegangsvedtak() {
        List<Oppdrag> resultat = LagOppdragTjeneste.lagOppdrag(Collections.emptyList(), lagTomInput());
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_selvstendig_næringsdrivende() {
        BeregningsresultatEntitet tilkjentYtelse = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        BeregningsresultatPeriode tyPeriode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(periode.getFom(), periode.getTom()).build(tilkjentYtelse);
        BeregningsresultatAndel.builder().medBrukerErMottaker(true).medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE).medDagsatsFraBg(1000).medDagsats(1000).medUtbetalingsgrad(BigDecimal.valueOf(100)).medStillingsprosent(BigDecimal.valueOf(100)).build(tyPeriode);

        Input input = lagInput(FagsakYtelseType.FORELDREPENGER, FamilieYtelseType.FØDSEL, tilkjentYtelse);
        //act
        List<Oppdrag> resultat = LagOppdragTjeneste.lagOppdrag(Collections.emptyList(), input);
        //assert
        assertThat(resultat).hasSize(1);
        Oppdrag oppdrag = resultat.get(0);
        assertThat(oppdrag.getBetalingsmottaker()).isEqualTo(Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getFagsystemId().getSaksnummer()).isEqualTo(saksnummer.getVerdi());
        assertThat(oppdrag.getØkonomiFagområde()).isEqualTo(ØkonomiKodeFagområde.FP);

        KjedeNøkkel nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPSND-OP"), Betalingsmottaker.BRUKER);
        assertThat(oppdrag.getKjeder().keySet()).containsOnly(nøkkelYtelse);

        OppdragKjedeFortsettelse kjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(kjede.getOppdragslinjer()).hasSize(1);
        assertLik(kjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-100")).medPeriode(periode).medSats(Sats.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());
    }

    @Test
    public void skal_få_oppdrag_for_førstegangsvedtak_til_bruker_som_er_arbeidstaker_og_får_feriepenger() {
        BeregningsresultatEntitet tilkjentYtelse = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        BeregningsresultatPeriode tyPeriode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(periode.getFom(), periode.getTom()).build(tilkjentYtelse);
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder().medBrukerErMottaker(true).medInntektskategori(Inntektskategori.ARBEIDSTAKER).medDagsatsFraBg(1000).medDagsats(1000).medUtbetalingsgrad(BigDecimal.valueOf(100)).medStillingsprosent(BigDecimal.valueOf(100)).build(tyPeriode);
        BeregningsresultatFeriepenger feriepenger = BeregningsresultatFeriepenger.builder().medFeriepengerRegelInput("foo").medFeriepengerRegelSporing("bar").medFeriepengerPeriodeFom(nesteMai.getFom()).medFeriepengerPeriodeTom(nesteMai.getTom()).build(tilkjentYtelse);
        BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(200).medOpptjeningsår(dag1.getYear()).build(feriepenger, andel);

        Input input = lagInput(FagsakYtelseType.FORELDREPENGER, FamilieYtelseType.FØDSEL, tilkjentYtelse);
        //act
        List<Oppdrag> resultat = LagOppdragTjeneste.lagOppdrag(Collections.emptyList(), input);
        //assert
        assertThat(resultat).hasSize(1);
        Oppdrag oppdrag = resultat.get(0);

        KjedeNøkkel nøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATORD"), Betalingsmottaker.BRUKER);
        KjedeNøkkel nøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.fraKode("FPATFER"), Betalingsmottaker.BRUKER, 2020);
        assertThat(oppdrag.getKjeder().keySet()).containsOnly(nøkkelYtelse, nøkkelFeriepenger);

        OppdragKjedeFortsettelse ytelsekjede = oppdrag.getKjeder().get(nøkkelYtelse);
        assertThat(ytelsekjede.getOppdragslinjer()).hasSize(1);
        assertLik(ytelsekjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-100")).medPeriode(periode).medSats(Sats.dagsats(1000)).medUtbetalingsgrad(new Utbetalingsgrad(100)).build());

        OppdragKjedeFortsettelse feriepengeKjede = oppdrag.getKjeder().get(nøkkelFeriepenger);
        assertThat(feriepengeKjede.getOppdragslinjer()).hasSize(1);
        assertLik(feriepengeKjede.getOppdragslinjer().get(0), OppdragLinje.builder().medDelytelseId(delytelseId("1-101")).medPeriode(nesteMai).medSats(Sats.engang(200)).build());
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

    Input lagTomInput() {
        BeregningsresultatEntitet tomtResultat = BeregningsresultatEntitet.builder().medRegelInput("foo").medRegelSporing("bar").build();
        return lagInput(FagsakYtelseType.FORELDREPENGER, FamilieYtelseType.FØDSEL, tomtResultat);
    }

    Input lagInput(FagsakYtelseType ytelseType, FamilieYtelseType familieYtelseType, BeregningsresultatEntitet tilkjentYtelse) {
        return Input.builder()
            .medFagsakYtelseType(ytelseType)
            .medFamilieYtelseType(familieYtelseType)
            .medTilkjentYtelse(tilkjentYtelse)
            .medSaksnummer(saksnummer)
            .medBehandlingId(enBehandlingId)
            .medBruker(brukerAktørId)
            .medBrukInntrekk(vanligInntrekkbeslutning)
            .medVedtaksdato(vedtaksdato)
            .build();
    }

}
