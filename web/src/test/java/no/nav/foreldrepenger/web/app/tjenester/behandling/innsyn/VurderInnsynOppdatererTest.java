package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.vedtak.innsyn.InnsynTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt.VurderInnsynDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt.VurderInnsynOppdaterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class VurderInnsynOppdatererTest {

    private LocalDate idag = LocalDate.now();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private InnsynRepository innsynRepository;
    @Inject
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private HistorikkRepository historikkRepository = Mockito.mock(HistorikkRepository.class);
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);
    private InnsynTjeneste innsynTjeneste;
    private VurderInnsynOppdaterer oppdaterer;

    @BeforeEach
    public void konfigurerMocker() {
        var enhet = new OrganisasjonsEnhet("enhetId", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        var oppretter = new BehandlingOpprettingTjeneste(behandlingskontrollTjeneste, behandlendeEnhetTjeneste, historikkRepository,
                mock(ProsessTaskTjeneste.class));
        innsynTjeneste = new InnsynTjeneste(oppretter, fagsakRepository, behandlingRepository, behandlingsresultatRepository, innsynRepository);
        oppdaterer = new VurderInnsynOppdaterer(behandlingskontrollTjeneste, innsynTjeneste, behandlingRepository);
    }

    @Test
    void skal_sette_innsynsbehandling_på_vent() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(idag);
        scenario.medDefaultBekreftetTerminbekreftelse();
        var behandling = scenario.lagre(repositoryProvider);

        var innsynbehandling = innsynTjeneste.opprettManueltInnsyn(behandling.getFagsak().getSaksnummer());
        var lås = behandlingRepository.taSkriveLås(innsynbehandling);
        behandlingRepository.lagre(innsynbehandling, lås);

        // Act
        var sattPåVent = true;

        var nå = LocalDateTime.now();
        var frist = nå.toLocalDate().plusDays(3);
        var dto = new VurderInnsynDto("grunn", InnsynResultatType.INNVILGET, frist, sattPåVent, Collections.emptyList(), frist);

        var oppdateringResultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(innsynbehandling), dto));

        // Assert
        Assertions.assertThat(oppdateringResultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        Assertions.assertThat(behandlingRepository.hentBehandling(innsynbehandling.getId()).isBehandlingPåVent()).isTrue();
    }

}
