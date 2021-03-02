package no.nav.foreldrepenger.web.app.tjenester.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
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
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoForBackendTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingÅrsakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BehandlingDtoForBackendTjenesteTest extends EntityManagerAwareTest {

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
    public void skal_lage_BehandlingDto() {
        Saksnummer saksnummer = new Saksnummer("12345");
        Fagsak fagsak = byggFagsak(aktørId, saksnummer);
        Behandling behandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        lagBehandligVedtak(behandling);
        avsluttBehandling(behandling);

        UtvidetBehandlingDto utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, null);
        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.isBehandlingPåVent()).isFalse();

        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);
        BehandlingÅrsakDto behandlingÅrsak = utvidetBehandlingDto.getBehandlingÅrsaker().get(0);
        assertThat(behandlingÅrsak.getBehandlingArsakType()).isEqualByComparingTo(BEHANDLING_ÅRSAK_TYPE);

        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NB);
        assertThat(utvidetBehandlingDto.getOriginalVedtaksDato()).isEqualTo(now.toLocalDate().toString());
        assertThat(utvidetBehandlingDto.getBehandlingsresultat().getType()).isEqualByComparingTo(BEHANDLING_RESULTAT_TYPE);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    @Test
    public void skal_lage_BehandlingDto_og_finne_opprinnelig_søknad_med_språkvalgNN() {
        Saksnummer saksnummer = new Saksnummer("111112");
        Fagsak fagsak = byggFagsak(aktørId, saksnummer);
        Behandling førstegangsBehandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        SøknadEntitet søknad = lagSøknadMedNynorskSpråk();
        repositoryProvider.getSøknadRepository().lagreOgFlush(førstegangsBehandling, søknad);
        lagBehandligVedtak(førstegangsBehandling);
        avsluttBehandling(førstegangsBehandling);

        Behandling innsyn = lagBehandling(fagsak, BehandlingType.INNSYN);
        lagBehandligVedtak(innsyn);
        avsluttBehandling(innsyn);

        UtvidetBehandlingDto utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(innsyn, null);

        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);

        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NN);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    @Test
    public void skal_lage_BehandlingDto_og_hente_språkvalg_fra_navbruker() {
        Saksnummer saksnummer = new Saksnummer("111113");
        Fagsak fagsak = byggFagsak(aktørId, saksnummer);
        Behandling førstegangsBehandling = lagBehandling(fagsak, BehandlingType.FØRSTEGANGSSØKNAD);
        lagBehandligVedtak(førstegangsBehandling);
        avsluttBehandling(førstegangsBehandling);

        Behandling innsyn = lagBehandling(fagsak, BehandlingType.INNSYN);
        lagBehandligVedtak(innsyn);
        avsluttBehandling(innsyn);

        UtvidetBehandlingDto utvidetBehandlingDto = behandlingDtoForBackendTjeneste.lagBehandlingDto(innsyn, null);

        assertThat(utvidetBehandlingDto.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).isNotEmpty();
        assertThat(utvidetBehandlingDto.getBehandlingÅrsaker()).hasSize(1);

        assertThat(utvidetBehandlingDto.getSpråkkode()).isEqualByComparingTo(Språkkode.NB);
        assertThat(utvidetBehandlingDto.getLinks()).isNotEmpty();
    }

    private Fagsak byggFagsak(AktørId aktørId, Saksnummer saksnummer) {
        NavBruker navBruker = NavBruker.opprettNyNB(aktørId);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, RelasjonsRolleType.MORA, saksnummer);
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        return fagsak;
    }

    private SøknadEntitet lagSøknadMedNynorskSpråk() {
        return new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(LocalDate.now().minusWeeks(3))
            .medElektroniskRegistrert(true)
            .medSpråkkode(Språkkode.NN)
            .build();

    }

    private Behandling lagBehandling(Fagsak fagsak, BehandlingType behandlingType) {

        Behandling behandling = Behandling.nyBehandlingFor(fagsak, behandlingType)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BEHANDLING_ÅRSAK_TYPE))
            .build();
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BEHANDLING_RESULTAT_TYPE)
            .build();

        behandling.setBehandlingresultat(behandlingsresultat);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        Long behandlingId = behandlingRepository.lagre(behandling, behandlingLås);

        return behandlingRepository.hentBehandling(behandlingId);
    }

    private void lagBehandligVedtak(Behandling behandling) {
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medVedtakstidspunkt(now)
            .medBeslutning(true).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, behandlingLås);
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }
}
