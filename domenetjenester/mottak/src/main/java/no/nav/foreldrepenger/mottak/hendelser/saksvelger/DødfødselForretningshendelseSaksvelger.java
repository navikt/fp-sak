package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.DØDFØDSEL_HENDELSE)
public class DødfødselForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<DødfødselForretningshendelse> {

    private static final Set<FagsakYtelseType> YTELSE_TYPER = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    public DødfødselForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(DødfødselForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> YTELSE_TYPER.contains(fagsak.getYtelseType()) && fagsak.erÅpen())
            .filter(fagsak -> erFagsakPassendeForFamilieHendelse(forretningshendelse.getDødfødselsdato(), fagsak))
            .collect(Collectors.toList()));

        return resultat;
    }

    private boolean erFagsakPassendeForFamilieHendelse(LocalDate fødsel, Fagsak fagsak) {
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()));
        return FødselForretningshendelseSaksvelger.erFødselPassendeForFamilieHendelseGrunnlag(fødsel, fhGrunnlag);
    }
}
