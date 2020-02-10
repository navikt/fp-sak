package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.EndringsdatoRevurderingUtlederImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class ErEndringIBeregningTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndring;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private EndringsdatoRevurderingUtlederImpl endringsdatoRevurderingUtlederImpl = mock(EndringsdatoRevurderingUtlederImpl.class);

    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;

    @Before
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        RevurderingTjenesteFelles revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        RevurderingTjeneste revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
            iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL, Optional.empty());
        VirksomhetEntitet virksomhet = new VirksomhetEntitet.Builder().medOrgnr(KUNSTIG_ORG).medNavn("Virksomheten").oppdatertOpplysningerNå().build();
        repositoryProvider.getVirksomhetRepository().lagre(virksomhet);
        LocalDate endringsdato = LocalDate.now().minusMonths(3);
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
    }

    @Test
    public void skal_gi_ingen_endring_i_beregningsgrunnlag_ved_lik_dagsats_på_periodenoivå() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPeriode);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_når_vi_mangler_beregningsgrunnlag_på_en_av_behandlingene() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.empty());

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_mangler_begge_beregningsgrunnlag() {
        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.empty(), Optional.empty());

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_med_forskjellige_fom_og_tom() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPerioderNyttGrunnlag = List.of(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(35)),
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(36), null));
        List<ÅpenDatoIntervallEntitet> bgPerioderOriginaltGrunnlag = List.of(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(40)),
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(41), null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPerioderOriginaltGrunnlag);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPerioderNyttGrunnlag);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_forskjellig_startdato() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPerioderNyttGrunnlag = List.of(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(35)),
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(36), null));
        List<ÅpenDatoIntervallEntitet> bgPerioderOriginaltGrunnlag = List.of(
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(40)),
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(41), null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPerioderOriginaltGrunnlag);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPerioderNyttGrunnlag);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_i_beregningsgrunnlag_ved_ulik_dagsats_på_periodenoivå() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPeriode);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlagForBehandling(revurdering, true, true, bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isTrue();
    }


    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling, boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker, List<ÅpenDatoIntervallEntitet> perioder) {
        return byggBeregningsgrunnlagForBehandling(behandling, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder, new LagEnAndelTjeneste());
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling, boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker, List<ÅpenDatoIntervallEntitet> perioder, LagAndelTjeneste lagAndelTjeneste) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = LagBeregningsgrunnlagTjeneste.lagBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder, lagAndelTjeneste);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
        return beregningsgrunnlag;
    }
}
