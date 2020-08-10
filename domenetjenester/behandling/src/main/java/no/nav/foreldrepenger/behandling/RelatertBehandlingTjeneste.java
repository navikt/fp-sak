package no.nav.foreldrepenger.behandling;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class RelatertBehandlingTjeneste {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    RelatertBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public RelatertBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.fagsakRelasjonRepository = behandlingRepositoryProvider.getFagsakRelasjonRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
    }

    public Optional<Behandling> hentAnnenPartsGjeldendeVedtattBehandling(Saksnummer saksnummer) {
        Optional<Fagsak> annenPartsFagsak = hentAnnenPartsFagsak(saksnummer);

        if(annenPartsFagsak.isPresent()) {
            Optional<BehandlingVedtak> vedtakAnnenpart = behandlingVedtakRepository.hentGjeldendeVedtak(annenPartsFagsak.get());
            return vedtakAnnenpart.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId).map(behandlingRepository::hentBehandling);
        }
        return Optional.empty();
    }

    public List<Fagsak> hentAnnenPartsInnvilgeteFagsakerMedYtelseType(AktørId aktørId, FagsakYtelseType ytelseType) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(f -> ytelseType.equals(f.getYtelseType()))
            .filter(f -> behandlingVedtakRepository.hentGjeldendeVedtak(f).map(BehandlingVedtak::getVedtakResultatType).filter(VedtakResultatType.INNVILGET::equals).isPresent())
            .collect(Collectors.toList());
    }

    public Optional<Behandling> hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(Behandling behandling) {
        Optional<Fagsak> annenPartsFagsak = hentAnnenPartsFagsak(behandling.getFagsak().getSaksnummer());
        if (annenPartsFagsak.isEmpty()) {
            return Optional.empty();
        }
        List<Behandling> alleAvsluttedeIkkeHenlagteBehandlingerAnnenPart = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(annenPartsFagsak.get().getId());
        List<BehandlingVedtak> annenpartVedtak = sortertPåVedtakstidspunkt(alleAvsluttedeIkkeHenlagteBehandlingerAnnenPart);
        BehandlingVedtak behandlingVedtak = vedtakForBehandling(behandling);
        if (annenpartVedtak.size() == 1) {
            if (harVedtakFør(annenpartVedtak.get(0), behandlingVedtak)) {
                return Optional.of(behandlingRepository.hentBehandling(annenpartVedtak.get(0).getBehandlingsresultat().getBehandlingId()));
            } else {
                return Optional.empty();
            }
        }

        Optional<Behandling> resultat = Optional.empty();
        for (BehandlingVedtak apVedtak : annenpartVedtak) {
            if (harVedtakFør(apVedtak, behandlingVedtak)) {
                resultat = Optional.of(behandlingRepository.hentBehandling(apVedtak.getBehandlingsresultat().getBehandlingId()));
            }
        }
        return resultat;
    }

    private BehandlingVedtak vedtakForBehandling(Behandling behandling) {
        return behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId()).orElseThrow();
    }

    /**
     * Sorterer først på vedtakstidspunkt. Hvis like så sorteres det på vedtak opprettet tidspunkt
     * Vedtak hadde bare dato og ikke klokkeslett før PFP-8620
     */
    private List<BehandlingVedtak> sortertPåVedtakstidspunkt(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(b -> vedtakForBehandling(b))
            .sorted((v1, v2) -> compare(v1, v2))
            .collect(Collectors.toList());
    }

    private boolean harVedtakFør(BehandlingVedtak vedtak1, BehandlingVedtak vedtak2) {
        return compare(vedtak1, vedtak2) < 0;
    }

    private int compare(BehandlingVedtak vedtak1, BehandlingVedtak vedtak2) {
        int vedtakstidspunktCompared = vedtak1.getVedtakstidspunkt().compareTo(vedtak2.getVedtakstidspunkt());
        if (vedtakstidspunktCompared == 0) {
            return vedtak1.getOpprettetTidspunkt().compareTo(vedtak2.getOpprettetTidspunkt());
        }
        return vedtakstidspunktCompared;
    }

    private Optional<Fagsak> hentAnnenPartsFagsak(Saksnummer saksnummer) {
        return fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer)
            .flatMap(r -> saksnummer.equals(r.getFagsakNrEn().getSaksnummer()) ? r.getFagsakNrTo() : Optional.of(r.getFagsakNrEn()));
    }
}
