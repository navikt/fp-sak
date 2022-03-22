package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
public class UtsettelseBehandling2021 {

    private UtsettelseCore2021 utsettelseCore;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    UtsettelseBehandling2021() {
        // CDI
    }

    @Inject
    public UtsettelseBehandling2021(UtsettelseCore2021 utsettelseCore, BehandlingRepositoryProvider repositoryProvider) {
        this.utsettelseCore = utsettelseCore;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public boolean kreverSammenhengendeUttak(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandling))
            .map(utsettelseCore::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    public boolean endringAvSammenhengendeUttak(BehandlingReferanse ref, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag1, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag2) {
        var sammenhengendeGrunnlag1 = utsettelseCore.kreverSammenhengendeUttak(familieHendelseGrunnlag1);
        var sammenhengendeGrunnlag2 = utsettelseCore.kreverSammenhengendeUttak(familieHendelseGrunnlag2);
        var sammenhengendeBehandling = kreverSammenhengendeUttak(ref.getBehandlingId());
        return sammenhengendeGrunnlag1 != sammenhengendeGrunnlag2 || sammenhengendeBehandling != sammenhengendeGrunnlag1;
    }

    boolean kreverSammenhengendeUttak(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingRepository.hentBehandling(behandlingId)))
            .map(utsettelseCore::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> vedtattFamilieHendelseRelatertFagsak(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling).map(Behandling::getId)
            .flatMap(familieHendelseRepository::hentAggregatHvisEksisterer);
    }
}
