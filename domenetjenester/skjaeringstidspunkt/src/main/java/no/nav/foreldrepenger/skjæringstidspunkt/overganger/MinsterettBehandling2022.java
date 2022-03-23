package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;

/*
 * Metode for å vurdere om en behandling skal vurderes etter nye eller gamle regler. Sjekker evt koblet fagsak.
 */
@ApplicationScoped
public class MinsterettBehandling2022 {

    private MinsterettCore2022 minsterettCore;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    MinsterettBehandling2022() {
        // CDI
    }

    @Inject
    public MinsterettBehandling2022(MinsterettCore2022 minsterettCore, BehandlingRepositoryProvider repositoryProvider) {
        this.minsterettCore = minsterettCore;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public boolean utenMinsterett(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandling))
            .map(minsterettCore::utenMinsterett)
            .orElse(MinsterettCore2022.DEFAULT_SAK_UTEN_MINSTERETT);
    }

    boolean utenMinsterett(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingRepository.hentBehandling(behandlingId)))
            .map(minsterettCore::utenMinsterett)
            .orElse(MinsterettCore2022.DEFAULT_SAK_UTEN_MINSTERETT);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> vedtattFamilieHendelseRelatertFagsak(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling).map(Behandling::getId)
            .flatMap(familieHendelseRepository::hentAggregatHvisEksisterer);
    }

}
