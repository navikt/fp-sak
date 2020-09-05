package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.vedtak.innsyn.InnsynTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt.VurderInnsynDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt.VurderInnsynOppdaterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class VurderInnsynOppdatererTest {

    private LocalDate idag = LocalDate.now();

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

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
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste = Mockito.mock(BehandlingskontrollTjeneste.class);

    private HistorikkRepository historikkRepository = Mockito.mock(HistorikkRepository.class);
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);
    private InnsynTjeneste innsynTjeneste;
    private VurderInnsynOppdaterer oppdaterer;

    @Before
    public void konfigurerMocker() {
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("enhetId", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        var oppretter = new BehandlingOpprettingTjeneste(behandlingskontrollTjeneste, behandlendeEnhetTjeneste, historikkRepository, mock(ProsessTaskRepository.class));
        innsynTjeneste = new InnsynTjeneste(oppretter, fagsakRepository,behandlingRepository, behandlingsresultatRepository, innsynRepository);
        oppdaterer = new VurderInnsynOppdaterer(behandlingskontrollTjeneste, innsynTjeneste);
    }

    @Test
    public void skal_sette_innsynsbehandling_på_vent() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(idag);
        scenario.medDefaultBekreftetTerminbekreftelse();
        var behandling = scenario.lagre(repositoryProvider);

        var innsynbehandling = innsynTjeneste.opprettManueltInnsyn(behandling.getFagsak().getSaksnummer());
        var lås = behandlingRepository.taSkriveLås(innsynbehandling);
        behandlingRepository.lagre(innsynbehandling, lås);

        // Act
        boolean sattPåVent = true;

        LocalDateTime nå = LocalDateTime.now();
        LocalDate frist = nå.toLocalDate().plusDays(3);
        var dto = new VurderInnsynDto("grunn", InnsynResultatType.INNVILGET, frist, sattPåVent, Collections.emptyList(), frist);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        var oppdateringResultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(innsynbehandling, aksjonspunkt, dto));

        // Assert
        Assertions.assertThat(oppdateringResultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        Assertions.assertThat(behandlingRepository.hentBehandling(innsynbehandling.getId()).isBehandlingPåVent()).isTrue();
    }

}
