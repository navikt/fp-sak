package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoForBackendTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingÅrsakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;

public class BehandlingDtoForBackendTjenesteTest {

    private static final String ANSVARLIG_SAKSBEHANDLER = "ABCD";
    private static final BehandlingÅrsakType BEHANDLING_ÅRSAK_TYPE = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    private static final BehandlingResultatType BEHANDLING_RESULTAT_TYPE = BehandlingResultatType.FORELDREPENGER_ENDRET;
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste = new BehandlingDtoForBackendTjeneste(repositoryProvider);
    private LocalDateTime now = LocalDateTime.now();


    @Test
    public void skal_lage_BehandlingDto() {
        Behandling behandling = lagBehandling();
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

    private Behandling lagBehandling() {
        NavBruker navBruker = NavBruker.opprettNyNB(AktørId.dummy());
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, RelasjonsRolleType.MORA, new Saksnummer("12345"));
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);

        Behandling behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BEHANDLING_ÅRSAK_TYPE))
            .build();
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BEHANDLING_RESULTAT_TYPE)
            .buildFor(behandling);
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
