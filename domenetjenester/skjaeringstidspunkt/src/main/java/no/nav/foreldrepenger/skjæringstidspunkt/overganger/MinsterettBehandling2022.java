package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

/*
 * Metode for å vurdere om en behandling skal vurderes etter nye eller gamle regler. Sjekker evt koblet fagsak.
 */
@ApplicationScoped
public class MinsterettBehandling2022 {

    private final MinsterettCore2022 minsterettCore2022 = new MinsterettCore2022();
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;

    MinsterettBehandling2022() {
        // CDI
    }

    @Inject
    public MinsterettBehandling2022(BehandlingRepositoryProvider repositoryProvider, FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public boolean utenMinsterett(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandling))
            .map(minsterettCore2022::utenMinsterett)
            .orElse(MinsterettCore2022.DEFAULT_SAK_UTEN_MINSTERETT);
    }

    boolean utenMinsterett(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingRepository.hentBehandling(behandlingId)))
            .map(minsterettCore2022::utenMinsterett)
            .orElse(MinsterettCore2022.DEFAULT_SAK_UTEN_MINSTERETT);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> vedtattFamilieHendelseRelatertFagsak(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak))
            .map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling)
            .map(Behandling::getId)
            .flatMap(familieHendelseRepository::hentAggregatHvisEksisterer);
    }

}
