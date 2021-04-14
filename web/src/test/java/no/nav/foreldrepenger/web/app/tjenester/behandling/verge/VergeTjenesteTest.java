package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
public class VergeTjenesteTest extends EntityManagerAwareTest {

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VergeRepository vergeRepository;
    private HistorikkRepository historikkRepository;

    private VergeTjeneste vergeTjeneste;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        historikkRepository = new HistorikkRepository(entityManager);
        vergeTjeneste = new VergeTjeneste(behandlingskontrollTjeneste, behandlingProsesseringTjeneste, vergeRepository,
            historikkRepository, behandlingRepository);
    }

    @Test
    public void skal_utlede_behandlingsmeny_skjul_når_behandlingen_står_før_kofak() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Act
        var resultat = vergeTjeneste.utledBehandlingsmeny(behandling.getId());

        // Assert
        assertThat(resultat.getVergeBehandlingsmeny()).isEqualTo(VergeBehandlingsmenyEnum.SKJUL);
    }

    @Test
    public void skal_utlede_behandlingsmeny_skjul_når_behandlingen_ikke_er_en_ytelsesbehandling() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.KLAGE).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Act
        var resultat = vergeTjeneste.utledBehandlingsmeny(behandling.getId());

        // Assert
        assertThat(resultat.getVergeBehandlingsmeny()).isEqualTo(VergeBehandlingsmenyEnum.SKJUL);
    }

    @Test
    public void skal_utlede_behandlingsmeny_opprett_når_behandlingen_ikke_har_registrert_verge_og_ikke_har_verge_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Act
        var resultat = vergeTjeneste.utledBehandlingsmeny(behandling.getId());

        // Assert
        assertThat(resultat.getVergeBehandlingsmeny()).isEqualTo(VergeBehandlingsmenyEnum.OPPRETT);
    }

    @Test
    public void skal_utlede_behandlingsmeny_fjern_når_behandlingen_har_verge_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);

        // Act
        var resultat = vergeTjeneste.utledBehandlingsmeny(behandling.getId());

        // Assert
        assertThat(resultat.getVergeBehandlingsmeny()).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
    }

    @Test
    public void skal_utlede_behandlingsmeny_fjern_når_behandlingen_har_registrert_verge() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var vergeBuilder = new VergeBuilder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Act
        var resultat = vergeTjeneste.utledBehandlingsmeny(behandling.getId());

        // Assert
        assertThat(resultat.getVergeBehandlingsmeny()).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
    }

    @Test
    public void skal_opprette_verge_aksjonspunkt_og_hoppe_tilbake() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Act
        vergeTjeneste.opprettVergeAksjonspunktOgHoppTilbakeTilKofakHvisSenereSteg(behandling);

        // Assert
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterFunnet(any(),
            eq(List.of(AksjonspunktDefinisjon.AVKLAR_VERGE)));
        verify(behandlingProsesseringTjeneste).reposisjonerBehandlingTilbakeTil(behandling,
            BehandlingStegType.KONTROLLER_FAKTA);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    public void skal_fjerne_verge_grunnlag_og_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);
        var vergeBuilder = new VergeBuilder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Act
        vergeTjeneste.fjernVergeGrunnlagOgAksjonspunkt(behandling);

        // Assert
        assertThat(vergeRepository.hentAggregat(behandling.getId()).get().getVerge()).isNotPresent();
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_VERGE);
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterAvbrutt(any(), any(), eq(List.of(ap)));
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
        var historikkinnslag = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikkinnslag.stream().map(Historikkinnslag::getType).collect(Collectors.toList())).contains(
            HistorikkinnslagType.FJERNET_VERGE);
    }

    private Fagsak opprettFagsak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()),
            RelasjonsRolleType.MORA, new Saksnummer("123"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }
}
