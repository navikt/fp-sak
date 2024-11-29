package no.nav.foreldrepenger.domene.vedtak.innsyn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class InnsynTjenesteTest {

    @Inject
    private BehandlingskontrollTjeneste behandlingKontrollTjeneste;
    @Inject
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Inject
    private HistorikkRepository historikkRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private InnsynRepository innsynRepository;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private InnsynTjeneste innsynTjeneste;
    private ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
        .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

    @BeforeEach
    public void oppsett() {
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(new OrganisasjonsEnhet("1234", ""));
        var oppretter = new BehandlingOpprettingTjeneste(behandlingKontrollTjeneste, behandlendeEnhetTjeneste, historikkRepository,
                mock(ProsessTaskTjeneste.class));
        innsynTjeneste = new InnsynTjeneste(oppretter, fagsakRepository, behandlingRepository, behandlingsresultatRepository, innsynRepository);
    }

    @Test
    void skal_opprette_innsynsbehandling_på_fagsak() {
        // arrange
        var opprinneligBehandling = scenario.lagre(repositoryProvider);
        var saksnummer = opprinneligBehandling.getSaksnummer();

        // act
        var nyBehandling = innsynTjeneste.opprettManueltInnsyn(saksnummer);

        // assert
        assertThat(nyBehandling.getType()).isEqualTo(BehandlingType.INNSYN);
    }

    @Test
    void skal_ikke_opprette_flere_behandlingsresultat_men_oppdatere_eksisterende_når_det_kommer_endringer() {
        // arrange
        var opprinneligBehandling = scenario.lagre(repositoryProvider);
        var saksnummer = opprinneligBehandling.getSaksnummer();

        var innsynbehandling = innsynTjeneste.opprettManueltInnsyn(saksnummer);

        var førsteResultat = lagResultat(innsynbehandling, InnsynResultatType.INNVILGET);
        innsynTjeneste.lagreVurderInnsynResultat(innsynbehandling, førsteResultat);
        var resultat1 = behandlingsresultatRepository.hent(innsynbehandling.getId());

        // act
        var oppdatertResultat = lagResultat(innsynbehandling, InnsynResultatType.DELVIS_INNVILGET);
        innsynTjeneste.lagreVurderInnsynResultat(innsynbehandling, oppdatertResultat);

        // assert
        var resultat2 = behandlingsresultatRepository.hent(innsynbehandling.getId());
        assertThat(resultat2.getId()).isEqualTo(resultat1.getId());
    }

    private InnsynEntitet lagResultat(Behandling innsynbehandling, InnsynResultatType delvisInnvilget) {
        return InnsynEntitet.InnsynBuilder.builder()
                .medMottattDato(LocalDate.of(2019, 7, 3))
                .medBehandlingId(innsynbehandling.getId())
                .medBegrunnelse("foo")
                .medInnsynResultatType(delvisInnvilget)
                .build();
    }
}
