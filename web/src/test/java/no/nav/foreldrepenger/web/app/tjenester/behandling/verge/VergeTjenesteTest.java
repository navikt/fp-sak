package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class VergeTjenesteTest extends EntityManagerAwareTest {

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkRepository;
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;

    private VergeTjeneste vergeTjeneste;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        historikkRepository = new HistorikkinnslagRepository(entityManager);
        vergeTjeneste = new VergeTjeneste(behandlingskontrollTjeneste, behandlingProsesseringTjeneste, vergeRepository,
            historikkRepository, behandlingRepository, personopplysningTjeneste);
    }

    @Test
    void skal_utlede_behandlingsmeny_skjul_når_erUnder18år_erIForelsåVedtak_ogHarRegistrertVerge() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var vergeBuilder = new VergeEntitet.Builder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        var personopplysningGrunnlagEntitet = opprettPersonopplysningGrunnlag(behandling.getAktørId(),
            LocalDate.now().minusYears(15));
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagEntitet, behandling.getAktørId());
        when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any(), any())).thenReturn(Optional.of(personopplysningerAggregat));
        when(behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);

        // Act
        var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling.getId());

        // Assert
        assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.SKJUL);
    }

    @Test
    void skal_utlede_behandlingsmeny_opprett_når_erUnder18år() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var personopplysningGrunnlagEntitet = opprettPersonopplysningGrunnlag(behandling.getAktørId(),
            LocalDate.now().minusYears(15));
        var personopplysningerAggregat = new PersonopplysningerAggregat(personopplysningGrunnlagEntitet, behandling.getAktørId());
        lenient().when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any())).thenReturn(Optional.of(personopplysningerAggregat));

        // Act
        var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling.getId());

        // Assert
        assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.OPPRETT);
    }

    @Test
    void skal_utlede_behandlingsmeny_opprett_når_behandlingen_ikke_har_registrert_verge_og_ikke_har_verge_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        // Act
        var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling.getId());

        // Assert
        assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.OPPRETT);
    }

    @Test
    void skal_utlede_behandlingsmeny_fjern_når_behandlingen_har_verge_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);

        // Act
        var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling.getId());

        // Assert
        assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
    }

    @Test
    void skal_utlede_behandlingsmeny_fjern_når_behandlingen_har_registrert_verge() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var vergeBuilder = new VergeEntitet.Builder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Act
        var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling.getId());

        // Assert
        assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
    }

    @Test
    void skal_opprette_verge_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);

        // Act
        vergeTjeneste.opprettVergeAksjonspunktOgHoppTilbakeTilFORVEDSTEGHvisSenereSteg(behandling);

        // Assert
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterFunnet(any(),
            eq(List.of(AksjonspunktDefinisjon.AVKLAR_VERGE)));
        verify(behandlingProsesseringTjeneste).reposisjonerBehandlingTilbakeTil(behandling,
            BehandlingStegType.FORESLÅ_VEDTAK);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_fjerne_verge_grunnlag_og_aksjonspunkt() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);
        var vergeBuilder = new VergeEntitet.Builder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Act
        vergeTjeneste.fjernVergeGrunnlagOgAksjonspunkt(behandling);

        // Assert
        assertThat(vergeRepository.hentAggregat(behandling.getId()).get().getVerge()).isNotPresent();
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_VERGE);
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterAvbrutt(any(), any(), eq(List.of(ap)));
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
        var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getTittel()).contains("verge", "fjernet");
    }

    private Fagsak opprettFagsak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()),
            RelasjonsRolleType.MORA, new Saksnummer("0123"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }
    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(AktørId aktørId, LocalDate fødselsdato) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(aktørId).medFødselsdato(fødselsdato));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
