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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.RepositoryProvider;
import no.nav.foreldrepenger.domene.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
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
        ScenarioForeldrepenger scenario = ScenarioForeldrepenger.nyttScenario();
        RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);
        BehandlingReferanse referanse = scenario.lagre(repositoryProvider);
        var opptjeningsperioderTjeneste = mock(OpptjeningsperioderUtenOverstyringTjeneste.class);
        behandlingReferanse = referanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        when(opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(any(), any(), any())).thenReturn(opptjeningsperioder);
        Opptjening opptjening = new Opptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        when(opptjeningsperioderTjeneste.hentOpptjeningHvisFinnes(any())).thenReturn(Optional.of(opptjening));
        opptjeningForBeregningTjeneste = new OpptjeningForBeregningTjeneste(opptjeningsperioderTjeneste);
    }

    @Test
    public void skal_returnere_empty() {
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, null);
        Assertions.assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_ikkje_filtrere_ut_frilanser_om_oppgitt_i_søknaden() {
        // Arrange
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = lagFrilansOppgittOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1));
        iayTjeneste.lagreOppgittOpptjening(behandlingReferanse.getBehandlingId(), oppgittOpptjeningBuilder);
        leggTilFrilansOpptjeningsperiode();

        // Act
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, iayTjeneste.hentGrunnlag(behandlingReferanse.getId()));

        // Assert
        Assertions.assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
    }

    @Test
    public void skal_filtrere_ut_frilanser_om_ikkje_oppgitt_i_søknaden() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();

        // Act
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        Assertions.assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_ikkje_filtrere_ut_andre_aktiviteter_enn_frilans_om_frilans_ikkje_oppgitt_i_søknaden() {
        // Arrange
        leggTilFrilansOpptjeningsperiode();
        leggTilArbeidOpptjeningsperiode();

        // Act
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        Assertions.assertThat(relevante).hasSize(1);
        assertThat(relevante.get(0).getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
    }

    @Test
    public void skal_filtrere_ut_utenlandskArbeid() {
        // Arrange
        leggTilUtenlandsArbeidOpptjeningsperiode();

        // Act
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, null);

        // Assert
        Assertions.assertThat(relevante).isEmpty();
    }

    @Test
    public void skal_filtrere_ut_videre_etterutdanning() {
        // Arrange
        leggTilVidereEtterutdanningOpptjeningsperiode();

        // Act
        List<OpptjeningsperiodeForSaksbehandling> relevante = opptjeningForBeregningTjeneste
            .hentRelevanteOpptjeningsaktiviteterForBeregning(behandlingReferanse, null);

        // Assert
        Assertions.assertThat(relevante).isEmpty();
    }

    private void leggTilVidereEtterutdanningOpptjeningsperiode() {
        OpptjeningsperiodeForSaksbehandling frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.VIDERE_ETTERUTDANNING).build();
        opptjeningsperioder.add(frilansOpptjening);
    }

    private void leggTilUtenlandsArbeidOpptjeningsperiode() {
        OpptjeningsperiodeForSaksbehandling frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD).build();
        opptjeningsperioder.add(frilansOpptjening);
    }


    private void leggTilFrilansOpptjeningsperiode() {
        OpptjeningsperiodeForSaksbehandling frilansOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.FRILANS).build();
        opptjeningsperioder.add(frilansOpptjening);
    }

    private void leggTilArbeidOpptjeningsperiode() {
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("123");
        OpptjeningsperiodeForSaksbehandling arbeidOpptjening = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1)))
            .medOpptjeningsnøkkel(Opptjeningsnøkkel.forArbeidsgiver(virksomhet))
            .medArbeidsgiver(virksomhet)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID).build();

        opptjeningsperioder.add(arbeidOpptjening);
    }


    private OppgittOpptjeningBuilder lagFrilansOppgittOpptjening(LocalDate fom, LocalDate tom) {
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        OppgittAnnenAktivitet annenAktivitet = new OppgittAnnenAktivitet(
            tom == null ? DatoIntervallEntitet.fraOgMed(fom) : DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), ArbeidType.FRILANSER);
        oppgittOpptjeningBuilder.leggTilAnnenAktivitet(annenAktivitet);
        return oppgittOpptjeningBuilder;
    }

}
