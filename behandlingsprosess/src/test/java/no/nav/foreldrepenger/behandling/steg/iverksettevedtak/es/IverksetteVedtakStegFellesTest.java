package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class IverksetteVedtakStegFellesTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private Behandling behandling;

    private IverksetteVedtakStegFelles iverksetteVedtakSteg;

    @Mock
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    private Repository repository = repoRule.getRepository();
    private BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();

    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    @Before
    public void setup() {
        iverksetteVedtakSteg = new IverksetteVedtakStegFelles(repositoryProvider, opprettProsessTaskIverksett, vurderBehandlingerUnderIverksettelse);
    }

    @Test
    public void skalKasteFeilNårIngenBehandlingVedtak() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        behandling = opprettBehandling();

        // Assert
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Utviklerfeil: Kan ikke iverksette, behandling mangler vedtak " + behandling.getId());

        // Act
        utførSteg(behandling);
    }

    @Test
    public void skalOppretteHistorikkinnslagHvisVenterTidligereBehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        behandling = opprettBehandling();
        @SuppressWarnings("unused")
        var vedtak = opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(true);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();

        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        BehandleStegResultat resultat2 = utførSteg(behandling);

        // Assert
        assertThat(resultat2.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat2.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skalOppretteIverksettingProsessTaskerHvisBehandlingKanIverksettes() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        verify(opprettProsessTaskIverksett).opprettIverksettingTasks(eq(behandling));
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId());
        assertThat(behandlingVedtakOpt).hasValueSatisfying(behandlingVedtak ->
            assertThat(behandlingVedtak.getIverksettingStatus()).isEqualTo(IverksettingStatus.IKKE_IVERKSATT)
        );
    }

    @Test
    public void skalSettePåVentHvisVedtakErUnderIverksettelse() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skalReturnereUtenAksjonspunkterNårVedtakErIverksatt() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IVERKSATT);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås));
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        repository.lagre(behandlingsresultat);

        repository.flush();

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType, IverksettingStatus iverksettingStatus) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(resultatType)
            .medIverksettingStatus(iverksettingStatus)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        return behandlingVedtak;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
