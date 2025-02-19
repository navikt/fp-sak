package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.OpprettVergeTjeneste;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;

import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.dto.NyVergeDto;

import no.nav.vedtak.exception.TekniskException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
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
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkRepository;
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private NavBrukerTjeneste brukerTjeneste;

    private VergeTjeneste vergeTjeneste;

    @Mock
    private VergeDtoTjeneste vergeDtoTjeneste;

    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        var fagsakRepository = new FagsakRepository(entityManager);
        vergeRepository = new VergeRepository(entityManager);
        historikkRepository = new HistorikkinnslagRepository(entityManager);

        var nyVergeTjeneste = new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkRepository);
        vergeTjeneste = new VergeTjeneste(behandlingskontrollTjeneste, behandlingProsesseringTjeneste, vergeRepository, historikkRepository,
            behandlingRepository, personopplysningTjeneste, nyVergeTjeneste, vergeDtoTjeneste);

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()), RelasjonsRolleType.MORA,
            new Saksnummer("0123"));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Nested
    class UtledBehandlingmenyOperasjon {
        @Test
        void skal_utlede_SKJUL_når_erUnder18år_erIForeslåVedtak_og_harRegistrertVerge() {
            // Arrange

            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

            var pa = opprettPersonopplysningAggregatForPersonUnder18(behandling.getAktørId());
            when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any(), any())).thenReturn(Optional.of(pa));
            when(behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.SKJUL);
        }

        @Test
        void skal_utlede_OPPRETT_når_erUnder18år() {
            // Arrange
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

            var pa = opprettPersonopplysningAggregatForPersonUnder18(behandling.getAktørId());
            when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any(), any())).thenReturn(Optional.of(pa));

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.OPPRETT);
        }

        @Test
        void skal_utlede_OPPRETT_når_behandlingen_ikke_har_registrert_verge_og_ikke_har_verge_aksjonspunkt() {
            // Arrange
            behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.OPPRETT);
        }

        @Test
        void skal_utlede_SKJUL_når_behandlingen_har_verge_aksjonspunkt() {
            // Arrange
            behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.SKJUL);
        }

        @Test
        void skal_utlede_FJERN_når_behandlingen_har_registrert_verge() {
            // Arrange
            behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
        }
    }

    @Test
    void skal_opprette_verge_aksjonspunkt() {
        // Arrange
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);

        // Act
        vergeTjeneste.opprettVergeAksjonspunktOgHoppTilbakeTilFORVEDSTEGHvisSenereSteg(behandling);

        // Assert
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterFunnet(any(), eq(List.of(AksjonspunktDefinisjon.AVKLAR_VERGE)));
        verify(behandlingProsesseringTjeneste).reposisjonerBehandlingTilbakeTil(behandling, BehandlingStegType.FORESLÅ_VEDTAK);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_fjerne_verge_grunnlag_og_aksjonspunkt() {
        // Arrange
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);
        vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

        // Act
        vergeTjeneste.fjernVergeGrunnlagOgAksjonspunkt(behandling);

        // Assert
        assertThat(vergeRepository.hentAggregat(behandling.getId())).isNotPresent();
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_VERGE);
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterAvbrutt(any(), any(), eq(List.of(ap)));
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
        var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getTittel()).contains("verge", "fjernet");
    }

    @Nested
    class OpprettVerge {

        @Test
        void skal_opprette_verge() {
            // Arrange
            var opprettVergeDto = new NyVergeDto("Jenny", "12345678901", LocalDate.parse("2022-01-01"), LocalDate.parse("2024-01-01"), VergeType.BARN,
                null);

            var vergeAktørId = AktørId.dummy();
            when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(vergeAktørId));
            when(brukerTjeneste.hentEllerOpprettFraAktørId(any())).thenReturn(NavBruker.opprettNyNB(vergeAktørId));

            // Act
            vergeTjeneste.opprettVerge(behandling, opprettVergeDto);

            // Assert
            assertThat(vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getVerge)).isPresent();
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTekstLinjer().getLast()).isEqualTo("Registrering av opplysninger om verge/fullmektig.");
        }
    }

    @Nested
    class FjernVerge {
        @Test
        void skal_feile_når_det_ikke_finnes_verge() {
            // Arrange
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

            // Assert
            assertThatExceptionOfType(TekniskException.class).isThrownBy(() -> vergeTjeneste.fjernVerge(behandling))
                .withMessage("FP-199772:Kan ikke fjerne verge fra eksisterende grunnlag som ikke finnes");

            assertThat(vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getVerge)).isEmpty();
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).isEmpty();
        }

        @Test
        void skal_fjerne_verge() {
            // Arrange
            vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

            // Act
            vergeTjeneste.fjernVerge(behandling);

            // Assert
            assertThat(vergeRepository.hentAggregat(behandling.getId())).isEmpty();
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTittel()).contains("verge", "fjernet");
        }

        @Test
        void skal_fjerne_verge_med_AP() {
            // Arrange
            AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_VERGE);
            vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

            // Act
            vergeTjeneste.fjernVerge(behandling);

            // Assert
            assertThat(vergeRepository.hentAggregat(behandling.getId())).isEmpty();
            var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_VERGE);
            verify(behandlingskontrollTjeneste).lagreAksjonspunkterAvbrutt(any(), any(), eq(List.of(ap)));
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTittel()).contains("verge", "fjernet");
        }
    }


    private VergeEntitet.Builder opprettVergeBuilder() {
        return new VergeEntitet.Builder().medVergeType(VergeType.BARN).gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
    }

    private PersonopplysningerAggregat opprettPersonopplysningAggregatForPersonUnder18(AktørId aktørId) {
        var builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(aktørId).medFødselsdato(LocalDate.now().minusYears(15)));
        var entitet = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).build();

        return new PersonopplysningerAggregat(entitet, aktørId);
    }
}
