package no.nav.foreldrepenger.domene.opptjening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.foreldrepenger.domene.prosess.RepositoryProvider;
import no.nav.foreldrepenger.domene.prosess.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class OpptjeningForBeregningTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 12, 12);

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final List<OpptjeningsperiodeForSaksbehandling> opptjeningsperioder = new ArrayList<>();
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var scenario = ScenarioForeldrepenger.nyttScenario();
        var repositoryProvider = new RepositoryProvider(entityManager);
        var behandling = scenario.lagre(repositoryProvider);
        var opptjeningsperioderTjeneste = mock(OpptjeningsperioderUtenOverstyringTjeneste.class);
        behandlingReferanse = BehandlingReferanse.fra(behandling).medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        when(opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(any(), any(), any())).thenReturn(
            opptjeningsperioder);
        var opptjening = new Opptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        when(opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(any())).thenReturn(Optional.of(opptjening));
        opptjeningForBeregningTjeneste = new OpptjeningForBeregningTjeneste(opptjeningsperioderTjeneste);
    }

    @Test
    public void skal_returnere_empty() {
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, null);
        assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_ikkje_filtrere_ut_frilanser_om_oppgitt_i_søknaden() {
        // Arrange
        var oppgittOpptjeningBuilder = lagFrilansOppgittOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        iayTjeneste.lagreOppgittOpptjening(behandlingReferanse.getBehandlingId(), oppgittOpptjeningBuilder);
        leggTilFrilansOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, iayTjeneste.hentGrunnlag(behandlingReferanse.getId()));

        // Assert
        assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
    }

    @Test
    public void skal_filtrere_ut_frilanser_om_ikkje_oppgitt_i_søknaden() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_ikkje_filtrere_ut_andre_aktiviteter_enn_frilans_om_frilans_ikkje_oppgitt_i_søknaden() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();
        leggTilArbeidOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
    }

    @Test
    public void skal_filtrere_ut_utenlandskArbeid() {
        // Arrange
        leggTilUtenlandsArbeidOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, null);

        // Assert
        assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_filtrere_ut_videre_etterutdanning() {
        // Arrange
        leggTilVidereEtterutdanningOpptjeningsperiode();

        // Act
        var relevante = opptjeningForBeregningTjeneste.hentRelevanteOpptjeningsaktiviteterForBeregning(
            behandlingReferanse, null);

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
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
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
