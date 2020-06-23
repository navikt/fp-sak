package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseHåndteringRepository;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødForretningshendelse;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.DØD_HENDELSE)
public class DødForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<DødForretningshendelse> {

    private static final Set<FagsakYtelseType> YTELSE_TYPER = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakRepository fagsakRepository;
    private HendelseHåndteringRepository hendelseHåndteringRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public DødForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider,
                                            HendelseHåndteringRepository hendelseHåndteringRepository,
                                            HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.hendelseHåndteringRepository = hendelseHåndteringRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(DødForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> YTELSE_TYPER.contains(fagsak.getYtelseType()) && fagsak.erÅpen())
            .collect(Collectors.toList()));

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> hendelseHåndteringRepository.hentFagsakerSomHarAktørIdSomBarn(aktørId).stream())
            .filter(fagsak -> FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()) && fagsak.erÅpen())
            .collect(Collectors.toList()));

        if (Endringstype.ANNULLERT.equals(forretningshendelse.getEndringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.getEndringstype())) {
            resultat.values().stream().flatMap(Collection::stream)
                .forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om død i folkeregisteret"));
        }

        return resultat;
    }
}

