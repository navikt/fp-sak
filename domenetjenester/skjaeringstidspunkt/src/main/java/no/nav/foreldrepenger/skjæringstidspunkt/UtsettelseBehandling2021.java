package no.nav.foreldrepenger.skjæringstidspunkt;

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
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.utsettelseCore = utsettelseCore;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public boolean kreverSammenhengendeUttak(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingId))
            .map(utsettelseCore::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    public boolean usikkertFrittUttak(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingId))
            .map(utsettelseCore::usikkertFrittUttak)
            .orElse(true);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> vedtattFamilieHendelseRelatertFagsak(Long behandlingId) {
        var fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling).map(Behandling::getId)
            .flatMap(familieHendelseRepository::hentAggregatHvisEksisterer);
    }

    public boolean endringAvSammenhengendeUttak(BehandlingReferanse ref, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag1, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag2) {
        var sammenhengendeGrunnlag1 = utsettelseCore.kreverSammenhengendeUttak(familieHendelseGrunnlag1);
        var sammenhengendeGrunnlag2 = utsettelseCore.kreverSammenhengendeUttak(familieHendelseGrunnlag2);
        var sammenhengendeBehandling = kreverSammenhengendeUttak(ref.getBehandlingId());
        return sammenhengendeGrunnlag1 != sammenhengendeGrunnlag2 || sammenhengendeBehandling != sammenhengendeGrunnlag1;
    }

}
