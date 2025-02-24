package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class BehandlingDtoForBackendTjenesteTest extends EntityManagerAwareTest {

    private static final String ANSVARLIG_SAKSBEHANDLER = "ABCD";
    private static final BehandlingÅrsakType BEHANDLING_ÅRSAK_TYPE = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    private static final BehandlingResultatType BEHANDLING_RESULTAT_TYPE = BehandlingResultatType.FORELDREPENGER_ENDRET;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste;
    private final LocalDateTime now = LocalDateTime.now();
    private final AktørId aktørId = AktørId.dummy();

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingDtoForBackendTjeneste = new BehandlingDtoForBackendTjeneste(repositoryProvider);
    }

    @Test
    void skal_lage_BehandlingDto() {
        var saksnummer = new Saksnummer("12345");
        var fagsak = byggFagsak(aktørId, saksnummer);
        var behandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        lagBehandligVedtak(behandling);
        avsluttBehandling(behandling);

        var utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, null, Optional.empty());
        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.isBehandlingPåVent()).isFalse();

        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);
        var behandlingÅrsak = utvidetBehandlingDto.getBehandlingÅrsaker().get(0);
        assertThat(behandlingÅrsak.getBehandlingArsakType()).isEqualByComparingTo(BEHANDLING_ÅRSAK_TYPE);

        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NB);
        assertThat(utvidetBehandlingDto.getOriginalVedtaksDato()).isEqualTo(now.toLocalDate().toString());
        assertThat(utvidetBehandlingDto.getBehandlingsresultat().getType()).isEqualByComparingTo(BEHANDLING_RESULTAT_TYPE);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    @Test
    void skal_lage_BehandlingDto_og_finne_opprinnelig_søknad_med_språkvalgNN() {
        var saksnummer = new Saksnummer("111112");
        var fagsak = byggFagsak(aktørId, saksnummer);
        var førstegangsBehandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        var søknad = lagSøknadMedNynorskSpråk();
        repositoryProvider.getSøknadRepository().lagreOgFlush(førstegangsBehandling, søknad);
        lagBehandligVedtak(førstegangsBehandling);
        avsluttBehandling(førstegangsBehandling);

        var innsyn = lagBehandling(fagsak, BehandlingType.INNSYN);
        lagBehandligVedtak(innsyn);
        avsluttBehandling(innsyn);

        var utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(innsyn, null, Optional.empty());

        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);
        assertThat(utvidetBehandlingDto.getBehandlendeEnhetId()).isEqualTo("9999");
        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NN);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    @Test
    void skal_lage_BehandlingDto_og_hente_språkvalg_fra_navbruker() {
        var saksnummer = new Saksnummer("111113");
        var fagsak = byggFagsak(aktørId, saksnummer);
        var førstegangsBehandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        lagBehandligVedtak(førstegangsBehandling);
        avsluttBehandling(førstegangsBehandling);

        var innsyn = lagBehandling(fagsak, BehandlingType.INNSYN);
        lagBehandligVedtak(innsyn);
        avsluttBehandling(innsyn);

        var utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(innsyn, null,
            Optional.of(new OrganisasjonsEnhet("9000", "Spesifikk")));

        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);
        assertThat(utvidetBehandlingDto.getBehandlendeEnhetId()).isEqualTo("9000");
        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NB);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    @Test
    void alle_ressurslenker_skal_matche_annotert_restmetode() {
        var behandlinger = BehandlingDtoLenkeTestUtils.lagOgLagreBehandlinger(repositoryProvider);

        var routes = BehandlingDtoLenkeTestUtils.getRoutes();
        for (var behandling : behandlinger) {
            for (var dtoLink : behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, null, Optional.empty()).getLinks()) {
                assertThat(dtoLink).isNotNull();
                assertThat(routes.stream().anyMatch(route -> route.hasSameHttpMethod(dtoLink) && route.matchesUrlTemplate(dtoLink))).withFailMessage(
                    "Route " + dtoLink + " does not exist.").isTrue();
            }
        }
    }


    private Fagsak byggFagsak(AktørId aktørId, Saksnummer saksnummer) {
        var navBruker = NavBruker.opprettNyNB(aktørId);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, RelasjonsRolleType.MORA, saksnummer);
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        return fagsak;
    }

    private SøknadEntitet lagSøknadMedNynorskSpråk() {
        return new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now())
            .medMottattDato(LocalDate.now().minusWeeks(3))
            .medElektroniskRegistrert(true)
            .medSpråkkode(Språkkode.NN)
            .build();
    }

    private Behandling lagBehandling(Fagsak fagsak, BehandlingType behandlingType) {

        var behandling = Behandling.nyBehandlingFor(fagsak, behandlingType)
            .medBehandlendeEnhet(new OrganisasjonsEnhet("9999", "Generisk"))
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BEHANDLING_ÅRSAK_TYPE))
            .build();
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BEHANDLING_RESULTAT_TYPE).build();

        behandling.setBehandlingresultat(behandlingsresultat);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        var behandlingId = behandlingRepository.lagre(behandling, behandlingLås);

        return behandlingRepository.hentBehandling(behandlingId);
    }

    private void lagBehandligVedtak(Behandling behandling) {
        var behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(now)
            .medBeslutning(true)
            .build();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, behandlingLås);
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }
}
