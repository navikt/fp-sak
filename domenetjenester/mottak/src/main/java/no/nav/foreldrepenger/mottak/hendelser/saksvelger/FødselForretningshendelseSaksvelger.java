package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.FØDSEL_HENDELSE)
public class FødselForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<FødselForretningshendelse> {

    private static final TemporalAmount UKER_FH_SAMME = Period.ofWeeks(5);
    private static final TemporalAmount UKER_FH_ULIK = Period.ofWeeks(20);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    public FødselForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(FødselForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();

        resultat.put(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, forretningshendelse.getAktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> fagsakErRelevantForeldrepengesak(fagsak)
                || fagsakErRelevantEngangsstønadsak(fagsak)
                || fagsakErRelevantSvangerskapspengersak(fagsak, forretningshendelse))
            .filter(f -> erFagsakPassendeForFamilieHendelse(forretningshendelse.getFødselsdato(), f))
            .collect(Collectors.toList()));

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
        Optional<Behandling> behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId());
        LocalDate fødselsdato = forretningshendelse.getFødselsdato();
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType())) {
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
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()));
        return erFødselPassendeForFamilieHendelseGrunnlag(fødsel, fhGrunnlag);
    }

    static boolean erFødselPassendeForFamilieHendelseGrunnlag(LocalDate fødsel, Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag) {
        if (fhGrunnlag.isEmpty()) {
            return false;
        }
        FamilieHendelseType fhType = fhGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return false;
        }
        return erPassendeFødselsSak(fødsel, fhGrunnlag.get());
    }

    private static boolean erPassendeFødselsSak(LocalDate fødsel, FamilieHendelseGrunnlagEntitet grunnlag) {
        Optional<LocalDate> termindatoPåSak = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        Optional<LocalDate> fødselsdatoPåSak = grunnlag.getGjeldendeBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();

        return erDatoIPeriodeHvisBeggeErTilstede(fødsel, termindatoPåSak, UKER_FH_ULIK, UKER_FH_SAMME) ||
            erDatoIPeriodeHvisBeggeErTilstede(fødsel, fødselsdatoPåSak, UKER_FH_SAMME, UKER_FH_SAMME);
    }

    private static boolean erDatoIPeriodeHvisBeggeErTilstede(LocalDate fødsel, Optional<LocalDate> periodeDato, TemporalAmount førPeriode, TemporalAmount etterPeriode) {
        return periodeDato.isPresent()
            && !(fødsel.isBefore(periodeDato.get().minus(førPeriode)) || fødsel.isAfter(periodeDato.get().plus(etterPeriode)));
    }
}

