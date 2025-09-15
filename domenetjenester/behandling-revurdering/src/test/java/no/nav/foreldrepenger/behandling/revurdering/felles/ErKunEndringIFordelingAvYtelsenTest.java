package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

class ErKunEndringIFordelingAvYtelsenTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    private static final String ORGNR = KUNSTIG_ORG;
    private static final InternArbeidsforholdRef REF = InternArbeidsforholdRef.nyRef();

    @Test
    void skal_gi_endring_i_ytelse_ved_forskjellig_fordeling_av_andeler() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1000, 1000));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1000, 1000));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var p2Rev = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(bgRev),
                Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var p2Rev = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING,Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(bgRev),
                Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void case_fra_prod_skal_oppdage_endring_fordeling() {
        // Arrange
        var basis = LocalDate.of(2020, 4, 18);
        var tom1 = LocalDate.of(2020, 4, 22);
        var tom2 = LocalDate.of(2020, 6, 10);

        // Originalgrunnlag
        var p1Org = byggBGPeriode(basis, Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2304, 0));
        var bgOrg = byggBeregningsgrunnlag(basis, Collections.singletonList(p1Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(basis, tom1, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2304, 0));
        var p2Rev = byggBGPeriode(tom1.plusDays(1), tom2, byggAndel(AktivitetStatus.ARBEIDSTAKER, 0, 2304));
        var p3Rev = byggBGPeriode(tom2.plusDays(1), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2304, 0));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev, p3Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var p2Org = byggBGPeriode(etterSTP(11), etterSTP(20), byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var p3Org = byggBGPeriode(etterSTP(21), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org, p3Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var p2Rev = byggBGPeriode(etterSTP(11), etterSTP(15), byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var p3Rev = byggBGPeriode(etterSTP(16), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev, p3Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void skal_gi_endring_i_ytelse_ved_ulik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var p2Org = byggBGPeriode(etterSTP(11), etterSTP(20), byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var p3Org = byggBGPeriode(etterSTP(21), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org, p3Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var p2Rev = byggBGPeriode(etterSTP(11), etterSTP(15), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1300, 1300));
        var p3Rev = byggBGPeriode(etterSTP(16), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 2400, 2400));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev, p3Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_sendt() {
        // Act
        var behandlingsresultat = ErKunEndringIFordelingAvYtelsen.fastsett(lagBehandling(),
            Behandlingsresultat.builder().build(), true);

        // Assert
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(behandlingsresultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
    }

    @Test
    void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_ikke_sendt() {
        // Act
        var behandlingsresultat = ErKunEndringIFordelingAvYtelsen.fastsett(lagBehandling(),
            Behandlingsresultat.builder().build(), false);

        // Assert
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).hasSize(1);
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen().get(0)).isEqualTo(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(behandlingsresultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
    }

    @Test
    void skal_gi_ingen_endring_dersom_begge_grunnlag_mangler() {
        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.empty(), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    void skal_gi_endring_dersom_et_grunnlag_mangler() {
        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(p1Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag() {
        // Originalgrunnlag
        var p1Org = byggBGPeriode(etterSTP(1), etterSTP(11), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var bgOrg = byggBeregningsgrunnlag(etterSTP(1), Collections.singletonList(p1Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(p1Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    void skal_gi_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag_men_erEndringISkalHindreTilbakketrekk_true() {
        // Originalgrunnlag
        var p1Org = byggBGPeriode(etterSTP(1), etterSTP(11), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var bgOrg = byggBeregningsgrunnlag(etterSTP(1), Collections.singletonList(p1Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10), byggAndel(AktivitetStatus.ARBEIDSTAKER, 123, 123));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(p1Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), true);

        // Assert
        assertThat(endringIBeregning).isTrue();
    }

    private Behandling lagBehandling() {
        var terminDato = LocalDate.now().minusDays(70);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medSøknadDato(terminDato.minusDays(20));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        return scenario.lagMocked();
    }

    private LocalDate etterSTP(int dagerEtter) {
        return SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(dagerEtter);
    }

    private BeregningsgrunnlagPrStatusOgAndel byggAndel(AktivitetStatus aktivitetStatus, int dagsatsBruker, int dagsatsAG) {
        var andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medBruttoPrÅr(BigDecimal.valueOf(240000))
            .medKilde(AndelKilde.PROSESS_START)
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(dagsatsBruker * 260L))
            .medDagsatsBruker((long) dagsatsBruker);
        if (aktivitetStatus.erArbeidstaker()) {
            var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(REF)
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            andelBuilder
                .medBGAndelArbeidsforhold(bga)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(dagsatsAG * 260L))
                .medDagsatsArbeidsgiver((long) dagsatsAG);
        }
        return andelBuilder.build();
    }

    public static Beregningsgrunnlag byggBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning, List<BeregningsgrunnlagPeriode> perioder,  AktivitetStatus... statuser) {
        var bgBuilder = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(skjæringstidspunktBeregning)
            .medGrunnbeløp(BigDecimal.valueOf(91425L));
        Arrays.asList(statuser).forEach(status -> bgBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(status)
            .build()));
        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);
        return bgBuilder.build();
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(LocalDate fom, LocalDate tom, BeregningsgrunnlagPrStatusOgAndel... andeler) {
        var periodeBuilder = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(fom, tom);
        Arrays.asList(andeler).forEach(periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder.build();
    }
}
