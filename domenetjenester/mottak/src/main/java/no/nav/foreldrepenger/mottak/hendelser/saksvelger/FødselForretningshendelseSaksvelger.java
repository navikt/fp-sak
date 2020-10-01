package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.FØDSEL_HENDELSE)
public class FødselForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<FødselForretningshendelse> {

    private static final TemporalAmount UKER_FH_SAMME = Period.ofWeeks(5);
    private static final TemporalAmount UKER_FH_ULIK = Period.ofWeeks(20);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public FødselForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider,
                                               FamilieHendelseTjeneste familieHendelseTjeneste,
                                               HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(FødselForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> fagsakErRelevantForeldrepengesak(fagsak)
                || fagsakErRelevantEngangsstønadsak(fagsak)
                || fagsakErRelevantSvangerskapspengersak(fagsak, forretningshendelse))
            .filter(f -> Endringstype.ANNULLERT.equals(forretningshendelse.getEndringstype())
                || erFagsakPassendeForFamilieHendelse(forretningshendelse.getFødselsdato(), f))
            .collect(Collectors.toList()));

        if (Endringstype.ANNULLERT.equals(forretningshendelse.getEndringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.getEndringstype())) {
            resultat.values().stream().flatMap(Collection::stream)
                .forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om fødsel i folkeregisteret"));
        }

        return resultat;
    }

    private boolean fagsakErRelevantForeldrepengesak(Fagsak fagsak) {
        return FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()) && fagsak.erÅpen();
    }

    private boolean fagsakErRelevantEngangsstønadsak(Fagsak fagsak) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) &&
            (fagsak.erÅpen() || behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()).isPresent());
    }

    private boolean fagsakErRelevantSvangerskapspengersak(Fagsak fagsak, FødselForretningshendelse forretningshendelse) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType())) {
            if (Endringstype.ANNULLERT.equals(forretningshendelse.getEndringstype())) {
                // ANNULLERT-hendelser inneholder ikke fødselsdato og videre sjekk er derfor unødvendig
                return true;
            }
            Optional<Behandling> behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId());
            LocalDate fødselsdato = forretningshendelse.getFødselsdato();

            Optional<BeregningsresultatEntitet> beregningsresultat = behandling.flatMap(b -> beregningsresultatRepository
                .hentBeregningsresultat(b.getId()));
            Optional<LocalDate> tilkjentYtelseTom = beregningsresultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                    .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                    .max(Comparator.naturalOrder());
            return tilkjentYtelseTom.map(d -> fødselsdato.minusDays(1).isBefore(d)).orElse(Boolean.FALSE);
        }
        return false;
    }

    private boolean erFagsakPassendeForFamilieHendelse(LocalDate fødsel, Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(b -> familieHendelseTjeneste.erFødselsHendelseRelevantFor(b.getId(), fødsel))
            .orElse(Boolean.FALSE);
    }

}

