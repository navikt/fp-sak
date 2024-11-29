package no.nav.foreldrepenger.behandling;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class RelatertBehandlingTjeneste {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    RelatertBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public RelatertBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
    }

    public Optional<Behandling> hentAnnenPartsGjeldendeVedtattBehandling(Saksnummer saksnummer) {
        var annenPartsFagsak = hentAnnenPartsFagsak(saksnummer);

        if (annenPartsFagsak.isPresent()) {
            var vedtakAnnenpart = behandlingVedtakRepository.hentGjeldendeVedtak(annenPartsFagsak.get());
            return vedtakAnnenpart.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId)
                    .map(behandlingRepository::hentBehandling);
        }
        return Optional.empty();
    }

    public List<Fagsak> hentAnnenPartsInnvilgeteFagsakerMedYtelseType(AktørId aktørId, FagsakYtelseType ytelseType) {
        return fagsakRepository.hentForBruker(aktørId).stream()
                .filter(f -> ytelseType.equals(f.getYtelseType()))
                .filter(f -> behandlingVedtakRepository.hentGjeldendeVedtak(f).map(BehandlingVedtak::getVedtakResultatType)
                        .filter(VedtakResultatType.INNVILGET::equals).isPresent())
                .toList();
    }

    public Optional<Behandling> hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(Behandling behandling) {
        var annenPartsFagsak = hentAnnenPartsFagsak(behandling.getSaksnummer());
        if (annenPartsFagsak.isEmpty()) {
            return Optional.empty();
        }
        var alleAvsluttedeIkkeHenlagteBehandlingerAnnenPart = behandlingRepository
                .finnAlleAvsluttedeIkkeHenlagteBehandlinger(annenPartsFagsak.get().getId());
        var annenpartVedtak = sortertPåVedtakstidspunkt(alleAvsluttedeIkkeHenlagteBehandlingerAnnenPart);
        var behandlingVedtak = vedtakForBehandling(behandling);
        if (annenpartVedtak.size() == 1) {
            if (harVedtakFør(annenpartVedtak.get(0), behandlingVedtak)) {
                return Optional.of(behandlingRepository.hentBehandling(annenpartVedtak.get(0).getBehandlingsresultat().getBehandlingId()));
            }
            return Optional.empty();
        }

        Optional<Behandling> resultat = Optional.empty();
        for (var apVedtak : annenpartVedtak) {
            if (harVedtakFør(apVedtak, behandlingVedtak)) {
                resultat = Optional.of(behandlingRepository.hentBehandling(apVedtak.getBehandlingsresultat().getBehandlingId()));
            }
        }
        return resultat;
    }

    private BehandlingVedtak vedtakForBehandling(Behandling behandling) {
        return behandlingVedtakRepository.hentForBehandling(behandling.getId());
    }

    /**
     * Sorterer først på vedtakstidspunkt. Hvis like så sorteres det på vedtak
     * opprettet tidspunkt Vedtak hadde bare dato og ikke klokkeslett før PFP-8620
     */
    private List<BehandlingVedtak> sortertPåVedtakstidspunkt(List<Behandling> behandlinger) {
        return behandlinger.stream()
                .map(this::vedtakForBehandling)
                .sorted(this::compare)
                .toList();
    }

    private boolean harVedtakFør(BehandlingVedtak vedtak1, BehandlingVedtak vedtak2) {
        return compare(vedtak1, vedtak2) < 0;
    }

    private int compare(BehandlingVedtak vedtak1, BehandlingVedtak vedtak2) {
        var vedtakstidspunktCompared = vedtak1.getVedtakstidspunkt().compareTo(vedtak2.getVedtakstidspunkt());
        if (vedtakstidspunktCompared == 0) {
            return vedtak1.getOpprettetTidspunkt().compareTo(vedtak2.getOpprettetTidspunkt());
        }
        return vedtakstidspunktCompared;
    }

    private Optional<Fagsak> hentAnnenPartsFagsak(Saksnummer saksnummer) {
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer)
                .flatMap(r -> saksnummer.equals(r.getFagsakNrEn().getSaksnummer()) ? r.getFagsakNrTo() : Optional.of(r.getFagsakNrEn()));
    }
}
