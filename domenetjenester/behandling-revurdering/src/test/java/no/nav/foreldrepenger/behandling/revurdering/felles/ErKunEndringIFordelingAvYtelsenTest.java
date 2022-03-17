package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

public class ErKunEndringIFordelingAvYtelsenTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    private static final String ORGNR = KUNSTIG_ORG;
    private static final InternArbeidsforholdRef REF = InternArbeidsforholdRef.nyRef();

    @Test
    public void skal_gi_endring_i_ytelse_ved_forskjellig_fordeling_av_andeler() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1000, 1000);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1000, 1000);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Rev = byggBGPeriode(bgRev, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(bgRev),
                Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Rev = byggBGPeriode(bgRev, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false, Optional.of(bgRev),
                Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void case_fra_prod_skal_oppdage_endring_fordeling() {
        // Arrange
        var basis = LocalDate.of(2020, 4, 18);
        var tom1 = LocalDate.of(2020, 4, 22);
        var tom2 = LocalDate.of(2020, 6, 10);

        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(basis, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, basis, Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 2304, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, basis, tom1);
        var p2Rev = byggBGPeriode(bgRev, tom1.plusDays(1), tom2);
        var p3Rev = byggBGPeriode(bgRev, tom2.plusDays(1), Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 2304, 0);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 0, 2304);
        byggAndel(p3Rev, AktivitetStatus.ARBEIDSTAKER, 2304, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p3Rev).build(bgRev);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_i_ytelse_ved_lik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(11), etterSTP(20));
        var p3Org = byggBGPeriode(bgOrg, etterSTP(21), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        byggAndel(p3Org, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p3Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10));
        var p2Rev = byggBGPeriode(bgRev, etterSTP(11), etterSTP(15));
        var p3Rev = byggBGPeriode(bgRev, etterSTP(16), Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        byggAndel(p3Rev, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p3Rev).build(bgRev);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_i_ytelse_ved_ulik_fordeling_av_andeler_ved_ulike_perioder() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(11), etterSTP(20));
        var p3Org = byggBGPeriode(bgOrg, etterSTP(21), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        byggAndel(p3Org, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p3Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Rev = byggBGPeriode(bgRev, etterSTP(11), etterSTP(15));
        var p3Rev = byggBGPeriode(bgRev, etterSTP(16), Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1300, 1300);
        byggAndel(p3Rev, AktivitetStatus.ARBEIDSTAKER, 2400, 2400);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p3Rev).build(bgRev);

        // Act
        var endring = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_sendt() {
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
    public void skal_teste_fastsettelse_av_behandlingsresultatet_ved_varsel_om_revurdering_ikke_sendt() {
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
    public void skal_gi_ingen_endring_dersom_begge_grunnlag_mangler() {
        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.empty(), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    public void skal_gi_endring_dersom_et_grunnlag_mangler() {
        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, Tid.TIDENES_ENDE);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.empty(), false);

        // Assert
        assertThat(endringIBeregning).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag() {
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(etterSTP(1), AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, etterSTP(1), etterSTP(11));
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10));
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);

        // Act
        var endringIBeregning = ErKunEndringIFordelingAvYtelsen.vurder(false, false,
                Optional.of(bgRev), Optional.of(bgOrg), false);

        // Assert
        assertThat(endringIBeregning).isFalse();
    }

    @Test
    public void skal_gi_endring_når_revurderingsgrunnlag_starter_før_originalgrunnlag_men_erEndringISkalHindreTilbakketrekk_true() {
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(etterSTP(1), AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, etterSTP(1), etterSTP(11));
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(10));
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 123, 123);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);

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

    private void byggAndel(BeregningsgrunnlagPeriode periode, AktivitetStatus aktivitetStatus, int dagsatsBruker, int dagsatsAG) {
        var andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(dagsatsBruker * 260L));
        if (aktivitetStatus.erArbeidstaker()) {
            var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(REF)
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            andelBuilder
                .medBGAndelArbeidsforhold(bga)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(dagsatsAG * 260L));
        }
        andelBuilder.build(periode);
    }

    public static BeregningsgrunnlagEntitet byggBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning, AktivitetStatus... statuser) {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(skjæringstidspunktBeregning)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .build();
        Arrays.asList(statuser).forEach(status -> BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(status)
            .build(beregningsgrunnlag));
        return beregningsgrunnlag;
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(BeregningsgrunnlagEntitet bg, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(bg);
    }
}
