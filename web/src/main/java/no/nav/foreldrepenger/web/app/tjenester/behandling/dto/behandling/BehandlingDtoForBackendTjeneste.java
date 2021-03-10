package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.get;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
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
    private SøknadRepository søknadRepository;

    public BehandlingDtoForBackendTjeneste() {
        //for CDI proxy
    }

    @Inject
    public BehandlingDtoForBackendTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    public UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, AsyncPollingStatus taskStatus) {
        Optional<BehandlingVedtak> behandlingVedtak = vedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());

        return lagBehandlingDto(behandling, behandlingVedtak, taskStatus);
    }

    private UtvidetBehandlingDto lagBehandlingDto(Behandling behandling, Optional<BehandlingVedtak> behandlingVedtak, AsyncPollingStatus asyncStatus) {
        UtvidetBehandlingDto dto = new UtvidetBehandlingDto();
        var vedtaksDato = behandlingVedtak.map(BehandlingVedtak::getVedtaksdato).orElse(null);
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, erBehandlingGjeldendeVedtak(behandling), vedtaksDato);
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }

        UuidDto uuidDto = new UuidDto(behandling.getUuid());

        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_BACKEND_PATH, "fagsak-backend", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.PERSONOPPLYSNINGER_TILBAKE_PATH, "personopplysninger-tilbake", uuidDto));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidDto));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VARSELTEKST_PATH, "tilbakekrevingsvarsel-fritekst", uuidDto));
        dto.leggTil(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekreving-valg", uuidDto));

        setVedtakDato(dto, behandlingVedtak);
        setBehandlingsresultat(dto, behandlingVedtak);
        dto.setSpråkkode(getSpråkkode(behandling));

        return dto;
    }

    private boolean erBehandlingGjeldendeVedtak(Behandling behandling) {
        Optional<BehandlingVedtak> gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(behandling.getFagsak());
        return gjeldendeVedtak
            .filter(v -> v.getBehandlingsresultat().getBehandlingId().equals(behandling.getId()))
            .isPresent();
    }

    private void setVedtakDato(UtvidetBehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        behandlingsVedtak.ifPresent(behandlingVedtak -> dto.setOriginalVedtaksDato(behandlingVedtak.getVedtaksdato()));
    }

    private void setBehandlingsresultat(BehandlingDto dto, Optional<BehandlingVedtak> behandlingsVedtak) {
        if (behandlingsVedtak.isPresent()) {
            Behandlingsresultat behandlingsresultat = behandlingsVedtak.get().getBehandlingsresultat();
            BehandlingsresultatDto behandlingsresultatDto = new BehandlingsresultatDto();
            behandlingsresultatDto.setType(behandlingsresultat.getBehandlingResultatType());
            behandlingsresultatDto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
            dto.setBehandlingsresultat(behandlingsresultatDto);
        }
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            Behandling sisteYtelsesBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(behandling.getFagsakId()).orElse(null);
            if (sisteYtelsesBehandling==null) {
                return behandling.getFagsak().getNavBruker().getSpråkkode();
            } else {
                return søknadRepository.hentSøknadHvisEksisterer(sisteYtelsesBehandling.getId()).map(SøknadEntitet::getSpråkkode).orElse(behandling.getFagsak().getNavBruker().getSpråkkode());
            }
        } else {
            return søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).map(SøknadEntitet::getSpråkkode).orElse(behandling.getFagsak().getNavBruker().getSpråkkode());
        }
    }
}
