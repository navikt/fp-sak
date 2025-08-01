package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseHåndteringRepository;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.freg.DødForretningshendelse;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.DØD)
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
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new EnumMap<>(BehandlingÅrsakType.class);

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, forretningshendelse.aktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> YTELSE_TYPER.contains(fagsak.getYtelseType()) && fagsak.erÅpen())
            .toList());

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, forretningshendelse.aktørIdListe().stream()
            .flatMap(aktørId -> hendelseHåndteringRepository.hentFagsakerSomHarAktørIdSomBarn(aktørId).stream())
            .filter(fagsak -> FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()) && fagsak.erÅpen())
            .toList());

        if (Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.endringstype())) {
            resultat.values().stream().flatMap(Collection::stream)
                .forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om død i folkeregisteret"));
        }

        return resultat;
    }
}

