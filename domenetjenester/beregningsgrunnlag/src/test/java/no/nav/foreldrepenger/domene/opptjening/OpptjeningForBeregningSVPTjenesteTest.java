package no.nav.foreldrepenger.domene.opptjening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class OpptjeningForBeregningSVPTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 12, 12);
    private static final Skjæringstidspunkt STP = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING).build();

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final List<OpptjeningsperiodeForSaksbehandling> opptjeningsperioder = new ArrayList<>();
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    void setUp() {
        var behandling= ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var opptjeningsperioderTjeneste = mock(OpptjeningsperioderUtenOverstyringTjeneste.class);
        behandlingReferanse = BehandlingReferanse.fra(behandling);
        when(opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(any(), any(), any(), any())).thenReturn(
            opptjeningsperioder);
        var opptjening = new Opptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(28),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        when(opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(any())).thenReturn(Optional.of(opptjening));
        opptjeningForBeregningTjeneste = new OpptjeningForBeregningTjeneste(opptjeningsperioderTjeneste);
    }

    @Test
    void skal_returnere_empty() {
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, null);
        assertThat(relevante).isEmpty();
    }

    @Test
    void skal_ikkje_filtrere_ut_frilanser_om_oppgitt_i_søknaden() {
        // Arrange
        var oppgittOpptjeningBuilder = lagFrilansOppgittOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        iayTjeneste.lagreOppgittOpptjening(behandlingReferanse.behandlingId(), oppgittOpptjeningBuilder);
        leggTilFrilansOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, iayTjeneste.hentGrunnlag(behandlingReferanse.behandlingId()));

        // Assert
        assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
    }

    @Test
    void skal_ta_med_frilanser_selv_om_ikkje_oppgitt_i_søknaden() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
    }

    @Test
    void skal_ikke_filtrere_ut_frilans_når_det_finnes_med_andre_aktiviteter() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();
        leggTilArbeidOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertThat(relevante).hasSize(2);
        assertThat(relevante.stream().filter(ra -> ra.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.ARBEID)).findFirst()).isPresent();
        assertThat(relevante.stream().filter(ra -> ra.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).findFirst()).isPresent();
    }

    @Test
    void skal_filtrere_ut_utenlandskArbeid() {
        // Arrange
        leggTilUtenlandsArbeidOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, null);

        // Assert
        assertThat(relevante).isEmpty();
    }

    @Test
    void skal_filtrere_ut_videre_etterutdanning() {
        // Arrange
        leggTilVidereEtterutdanningOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, STP, null);

        // Assert
        assertThat(relevante).isEmpty();
    }

    private void leggTilVidereEtterutdanningOpptjeningsperiode() {
        var frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.VIDERE_ETTERUTDANNING)
            .build();
        opptjeningsperioder.add(frilansOpptjening);
    }

    private void leggTilUtenlandsArbeidOpptjeningsperiode() {
        var frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)
            .build();
        opptjeningsperioder.add(frilansOpptjening);
    }


    private void leggTilFrilansOpptjeningsperiode() {
        var frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2).minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS)
            .build();
        opptjeningsperioder.add(frilansOpptjening);
    }

    private void leggTilArbeidOpptjeningsperiode() {
        var virksomhet = Arbeidsgiver.virksomhet("123");
        var arbeidOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningsnøkkel(Opptjeningsnøkkel.forArbeidsgiver(virksomhet))
            .medArbeidsgiver(virksomhet)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        opptjeningsperioder.add(arbeidOpptjening);
    }


    private OppgittOpptjeningBuilder lagFrilansOppgittOpptjening(LocalDate fom, LocalDate tom) {
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var annenAktivitet = new OppgittAnnenAktivitet(
            tom == null ? DatoIntervallEntitet.fraOgMed(fom) : DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            ArbeidType.FRILANSER);
        oppgittOpptjeningBuilder.leggTilAnnenAktivitet(annenAktivitet);
        return oppgittOpptjeningBuilder;
    }

}
