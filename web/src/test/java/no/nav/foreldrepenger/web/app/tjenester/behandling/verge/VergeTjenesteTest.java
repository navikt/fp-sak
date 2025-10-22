package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.OpprettVergeTjeneste;
import no.nav.foreldrepenger.domene.person.verge.VergeDtoTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
class VergeTjenesteTest extends EntityManagerAwareTest {

    @Mock
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    @Mock
    private BehandlingLås skriveLås;
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
    @Mock
    private BehandlingEventPubliserer behandlingEventPubliserer;

    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        var fagsakRepository = new FagsakRepository(entityManager);
        vergeRepository = new VergeRepository(entityManager);
        historikkRepository = new HistorikkinnslagRepository(entityManager);

        var opprettVergeTjeneste = new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkRepository);
        vergeTjeneste = new VergeTjeneste(aksjonspunktkontrollTjeneste, behandlingProsesseringTjeneste, vergeRepository, historikkRepository, personopplysningTjeneste,
            opprettVergeTjeneste, vergeDtoTjeneste, behandlingEventPubliserer);

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
            when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any())).thenReturn(Optional.of(pa));
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(false);
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK)).thenReturn(true);

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
            when(personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(any())).thenReturn(Optional.of(pa));
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK)).thenReturn(true);

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
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK)).thenReturn(true);

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
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK)).thenReturn(true);

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
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FORESLÅ_VEDTAK)).thenReturn(true);
            when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.FATTE_VEDTAK)).thenReturn(true);

            // Act
            var behandlingOperasjon = vergeTjeneste.utledBehandlingOperasjon(behandling);

            // Assert
            assertThat(behandlingOperasjon).isEqualTo(VergeBehandlingsmenyEnum.FJERN);
        }
    }

    @Nested
    class OpprettVerge {

        @Test
        void skal_opprette_verge() {
            // Arrange
            var opprettVergeDto = VergeDto.person(VergeType.BARN, LocalDate.of(2022, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 1),
                "Jenny", "12345678901");

            var vergeAktørId = AktørId.dummy();
            when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(vergeAktørId));
            when(brukerTjeneste.hentEllerOpprettFraAktørId(any())).thenReturn(NavBruker.opprettNyNB(vergeAktørId));

            // Act
            vergeTjeneste.opprettVerge(behandling, opprettVergeDto, null);

            // Assert
            var verge = vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getVerge);
            assertThat(verge).isPresent();
            assertThat(verge.get().getBruker()).isPresent();
            assertThat(verge.get().getBruker().get().getAktørId()).isEqualTo(vergeAktørId);
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTekstLinjer().getLast()).isEqualTo("Opplysninger om verge/fullmektig er registrert.");
        }

        @Test
        void skal_opprette_verge_organisasjon() {
            // Arrange
            var opprettVergeDto = VergeDto.organisasjon(VergeType.ADVOKAT, LocalDate.of(2022, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 1),
                "Kunstig virksomhet", OrgNummer.KUNSTIG_ORG);

            // Act
            vergeTjeneste.opprettVerge(behandling, opprettVergeDto, null);

            // Assert
            var verge = vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getVerge);
            assertThat(verge).isPresent();
            assertThat(verge.get().getVergeOrganisasjon()).isPresent();
            assertThat(verge.get().getVergeOrganisasjon().get().getOrganisasjonsnummer()).isEqualTo(OrgNummer.KUNSTIG_ORG);
            assertThat(verge.get().getBruker()).isEmpty();
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTekstLinjer().getLast()).isEqualTo("Opplysninger om verge/fullmektig er registrert.");
        }
    }

    @Nested
    class FjernVerge {
        @Test
        void skal_feile_når_det_ikke_finnes_verge() {
            // Arrange
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

            // Assert
            assertThatExceptionOfType(TekniskException.class).isThrownBy(() -> vergeTjeneste.fjernVerge(behandling, skriveLås))
                .withMessage("FP-199772: Kan ikke fjerne verge fra eksisterende grunnlag som ikke finnes");

            assertThat(vergeRepository.hentAggregat(behandling.getId()).flatMap(VergeAggregat::getVerge)).isEmpty();
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).isEmpty();
        }

        @Test
        void skal_fjerne_verge() {
            // Arrange
            vergeRepository.lagreOgFlush(behandling.getId(), opprettVergeBuilder());

            // Act
            vergeTjeneste.fjernVerge(behandling, skriveLås);

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
            vergeTjeneste.fjernVerge(behandling, skriveLås);

            // Assert
            assertThat(vergeRepository.hentAggregat(behandling.getId())).isEmpty();
            var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_VERGE);
            verify(aksjonspunktkontrollTjeneste).lagreAksjonspunkterAvbrutt(any(), any(), eq(List.of(ap)));
            var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());
            assertThat(historikkinnslag).hasSize(1);
            assertThat(historikkinnslag.getFirst().getTittel()).contains("verge", "fjernet");
        }
    }


    private VergeEntitet.Builder opprettVergeBuilder() {
        return new VergeEntitet.Builder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .medBruker(NavBruker.opprettNyNB(AktørId.dummy()));
    }

    private PersonopplysningerAggregat opprettPersonopplysningAggregatForPersonUnder18(AktørId aktørId) {
        var builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(aktørId).medFødselsdato(LocalDate.now().minusYears(15)));
        var entitet = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).build();

        return new PersonopplysningerAggregat(entitet, aktørId);
    }
}
