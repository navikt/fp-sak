package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
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
import no.nav.foreldrepenger.mottak.hendelser.freg.DødForretningshendelse;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.freg.UtflyttingForretningshendelse;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.UTFLYTTING_HENDELSE)
public class UtflyttingForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<UtflyttingForretningshendelse> {

    private static final Set<FagsakYtelseType> YTELSE_TYPER = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakRepository fagsakRepository;
    private HendelseHåndteringRepository hendelseHåndteringRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public UtflyttingForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider,
                                                   HendelseHåndteringRepository hendelseHåndteringRepository,
                                                   HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.hendelseHåndteringRepository = hendelseHåndteringRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(UtflyttingForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> YTELSE_TYPER.contains(fagsak.getYtelseType()) && fagsak.erÅpen())
            .collect(Collectors.toList()));

        if (Endringstype.ANNULLERT.equals(forretningshendelse.getEndringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.getEndringstype())) {
            resultat.values().stream().flatMap(Collection::stream)
                .forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om utflytting i Folkeregisteret"));
        }

        return resultat;
    }

    private boolean erFagsakPassendeForUtflyttingHendelse(LocalDate utflyttingsdato, Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(b -> familieHendelseTjeneste.erFødselsHendelseRelevantFor(b.getId(), fødsel))
            .orElse(Boolean.FALSE);
    }
}

