package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
public class UtsettelseBehandling2021 {

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;

    UtsettelseBehandling2021() {
        // CDI
    }

    @Inject
    public UtsettelseBehandling2021(BehandlingRepositoryProvider repositoryProvider,
                                    FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public boolean kreverSammenhengendeUttak(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandling))
            .map(UtsettelseCore2021::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    public boolean kreverSammenhengendeUttakV2(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandling))
            .map(UtsettelseCore2021::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    public boolean endringAvSammenhengendeUttak(BehandlingReferanse ref, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag1, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag2) {
        var sammenhengendeGrunnlag1 = UtsettelseCore2021.kreverSammenhengendeUttak(familieHendelseGrunnlag1);
        var sammenhengendeGrunnlag2 = UtsettelseCore2021.kreverSammenhengendeUttak(familieHendelseGrunnlag2);
        var sammenhengendeBehandling = kreverSammenhengendeUttak(ref.behandlingId());
        return sammenhengendeGrunnlag1 != sammenhengendeGrunnlag2 || sammenhengendeBehandling != sammenhengendeGrunnlag1;
    }

    boolean kreverSammenhengendeUttak(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .or(() -> vedtattFamilieHendelseRelatertFagsak(behandlingRepository.hentBehandling(behandlingId)))
            .map(UtsettelseCore2021::kreverSammenhengendeUttak)
            .orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> vedtattFamilieHendelseRelatertFagsak(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsak(fagsak)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling).map(Behandling::getId)
            .flatMap(familieHendelseRepository::hentAggregatHvisEksisterer);
    }
}
