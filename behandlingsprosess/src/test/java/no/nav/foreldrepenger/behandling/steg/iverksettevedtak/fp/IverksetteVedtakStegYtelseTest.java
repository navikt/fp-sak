package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.fp.OpprettProsessTaskIverksettImpl;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class IverksetteVedtakStegYtelseTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private Behandling behandling;

    @Mock
    private OpprettProsessTaskIverksettImpl opprettProsessTaskIverksett;

    private Repository repository = repoRule.getRepository();
    private BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private HistorikkRepository historikkRepository = repositoryProvider.getHistorikkRepository();

    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    @Mock
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste iverksettingSkalIkkeStoppesAvOverlappendeYtelse;

    private IverksetteVedtakStegFørstegang iverksetteVedtakSteg;

    @Before
    public void setup() {
        iverksetteVedtakSteg = new IverksetteVedtakStegFørstegang(repositoryProvider, opprettProsessTaskIverksett, vurderBehandlingerUnderIverksettelse, iverksettingSkalIkkeStoppesAvOverlappendeYtelse);
        behandling = opprettBehandling();
    }

    @Test
    public void vurder_gitt_venterPåInfotrygd_venterTidligereBehandling_skal_VENT_TIDLIGERE_BEHANDLING() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(true);
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurderOmOverlappInfotrygd(eq(behandling))).thenReturn(Collections.emptyList());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        Historikkinnslag historikkinnslag = historikkRepository.hentHistorikk(behandling.getId()).get(0);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        HistorikkinnslagDel del1 = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del1.getHendelse()).hasValueSatisfying(hendelse ->
            assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.IVERKSETTELSE_VENT.getKode()));
        assertThat(del1.getAarsak().get()).isEqualTo(Venteårsak.VENT_TIDLIGERE_BEHANDLING.getKode());
    }


    @Test
    public void vurder_gitt_venterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty_og_lagre_overlapp() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);
        List<BehandlingOverlappInfotrygd> overlappIt = new ArrayList();
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurderOmOverlappInfotrygd(eq(behandling))).thenReturn(overlappIt);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
            verify(iverksettingSkalIkkeStoppesAvOverlappendeYtelse).vurderOglagreEventueltOverlapp(any());
    }

    @Test
    public void vurder_gitt_ikkeVenterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurderOmOverlappInfotrygd(eq(behandling))).thenReturn(Collections.emptyList());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        verify(iverksettingSkalIkkeStoppesAvOverlappendeYtelse).vurderOglagreEventueltOverlapp(any());
    }

    @Test
    public void sender_riktige_initielle_tasks() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        verify(opprettProsessTaskIverksett).opprettIverksettingstasker(behandling, List.of(StartBerørtBehandlingTask.TASKTYPE, VurderOpphørAvYtelserTask.TASKTYPE));
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås));
    }


    private Behandling opprettBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);

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
