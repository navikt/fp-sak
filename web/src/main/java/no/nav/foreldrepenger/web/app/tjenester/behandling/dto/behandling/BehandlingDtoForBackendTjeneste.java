package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag.OppdragRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

/**
 * Returnerer behandlingsinformasjon og lenker for en behandling.
 * <p>
 * Tilsvarende tjeneste for front-end er @{@link BehandlingDtoTjeneste}
 * <p>
 * Det er valgt å skille de i to i håp om enklere vedlikehold av tjenesten for frontend.
 */

@ApplicationScoped
public class BehandlingDtoForBackendTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SøknadRepository søknadRepository;

    @Inject
    public BehandlingDtoForBackendTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    BehandlingDtoForBackendTjeneste() {
        //for CDI proxy
    }

    public UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, AsyncPollingStatus taskStatus, Optional<OrganisasjonsEnhet> endretEnhet) {
        var behandlingVedtak = vedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());

        return lagBehandlingDto(behandling, behandlingVedtak, taskStatus, endretEnhet);
    }

    private UtvidetBehandlingDto lagBehandlingDto(Behandling behandling,
                                                  Optional<BehandlingVedtak> behandlingVedtak,
                                                  AsyncPollingStatus asyncStatus,
                                                  Optional<OrganisasjonsEnhet> endretEnhet) {
        var dto = new UtvidetBehandlingDto();
        var vedtaksDato = behandlingVedtak.map(BehandlingVedtak::getVedtaksdato).orElse(null);
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, behandlingsresultat, dto, erBehandlingGjeldendeVedtak(behandling), vedtaksDato);
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }
        endretEnhet.ifPresent(e -> {
            dto.setBehandlendeEnhetId(e.enhetId());
            dto.setBehandlendeEnhetNavn(e.enhetNavn());
        });

        var uuidDto = new UuidDto(behandling.getUuid());

        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", new SaksnummerDto(behandling.getSaksnummer())));
        dto.leggTil(get(VergeRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.PERSONOPPLYSNINGER_TILBAKE_PATH, "personopplysninger-tilbake", uuidDto));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VARSELTEKST_PATH, "tilbakekrevingsvarsel-fritekst", uuidDto));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekreving-valg", uuidDto));
        dto.leggTil(get(OppdragRestTjeneste.OPPDRAGINFO_PATH, "oppdrag-info", uuidDto));

        setVedtakDato(dto, behandlingVedtak);
        setBehandlingsresultat(dto, behandlingVedtak);
        dto.setSpråkkode(getSpråkkode(behandling));

        return dto;
    }

    private boolean erBehandlingGjeldendeVedtak(Behandling behandling) {
        var gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(behandling.getFagsak());
        return gjeldendeVedtak.filter(v -> v.getBehandlingsresultat().getBehandlingId().equals(behandling.getId())).isPresent();
    }

    private void setVedtakDato(UtvidetBehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        behandlingsVedtak.ifPresent(behandlingVedtak -> dto.setOriginalVedtaksDato(behandlingVedtak.getVedtaksdato()));
    }

    private void setBehandlingsresultat(BehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        if (behandlingsVedtak.isPresent()) {
            var behandlingsresultat = behandlingsVedtak.get().getBehandlingsresultat();
            var behandlingsresultatDto = new BehandlingsresultatDto();
            behandlingsresultatDto.setType(behandlingsresultat.getBehandlingResultatType());
            behandlingsresultatDto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
            dto.setBehandlingsresultat(behandlingsresultatDto);
        }
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(behandling.getFagsakId())
                .flatMap(s -> søknadRepository.hentSøknadHvisEksisterer(s.getId()))
                .map(SøknadEntitet::getSpråkkode)
                .orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
        }
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(SøknadEntitet::getSpråkkode)
            .orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
    }
}
