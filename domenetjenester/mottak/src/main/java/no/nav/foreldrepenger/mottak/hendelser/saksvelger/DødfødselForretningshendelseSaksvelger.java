package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.freg.DødfødselForretningshendelse;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.DØDFØDSEL)
public class DødfødselForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<DødfødselForretningshendelse> {

    private static final Set<FagsakYtelseType> YTELSE_TYPER = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public DødfødselForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider,
                                                  FamilieHendelseTjeneste familieHendelseTjeneste,
                                                  HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(DødfødselForretningshendelse forretningshendelse) {

        var saker = forretningshendelse.aktørIdListe()
            .stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> fagsakErRelevantForHendelse(fagsak, forretningshendelse))
            .filter(fagsak -> Endringstype.ANNULLERT.equals(forretningshendelse.endringstype()) || erFagsakPassendeForFamilieHendelse(
                forretningshendelse.dødfødselsdato(), fagsak))
            .toList();

        if (Endringstype.ANNULLERT.equals(forretningshendelse.endringstype()) || Endringstype.KORRIGERT.equals(forretningshendelse.endringstype())) {
            saker.forEach(
                f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om dødfødsel i folkeregisteret"));
        }

        return Map.of(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL, saker);
    }

    private boolean fagsakErRelevantForHendelse(Fagsak fagsak, DødfødselForretningshendelse forretningshendelse) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType()) && Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())) {
            // ANNULLERT-hendelser inneholder ikke fødselsdato og videre sjekk er derfor unødvendig
            return false;
        }
        return YTELSE_TYPER.contains(fagsak.getYtelseType()) && fagsak.erÅpen();

    }

    private boolean erFagsakPassendeForFamilieHendelse(LocalDate fødsel, Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(b -> familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(b.getId(), fødsel))
            .orElse(Boolean.FALSE);
    }
}
