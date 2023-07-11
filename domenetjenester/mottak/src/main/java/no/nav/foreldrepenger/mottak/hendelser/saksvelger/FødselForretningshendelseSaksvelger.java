package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.freg.FødselForretningshendelse;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.FØDSEL)
public class FødselForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<FødselForretningshendelse> {

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

        var saker = forretningshendelse.aktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(fagsak -> fagsakErRelevantForHendelse(fagsak, forretningshendelse))
            .filter(f -> Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())
                || erFagsakPassendeForFamilieHendelse(forretningshendelse.fødselsdato(), f))
            .toList();

        if (Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.endringstype())) {
            saker.forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om fødsel i folkeregisteret"));
        }

        return Map.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, saker);
    }

    private boolean fagsakErRelevantForHendelse(Fagsak fagsak, FødselForretningshendelse forretningshendelse) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsak.getYtelseType())) {
            if (Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())) {
                // ANNULLERT-hendelser inneholder ikke fødselsdato og videre sjekk er derfor unødvendig
                return false;
            }
            var tilkjentYtelseTom = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
                .flatMap(b -> beregningsresultatRepository.hentUtbetBeregningsresultat(b.getId()))
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                .max(Comparator.naturalOrder()).orElse(Tid.TIDENES_BEGYNNELSE);
            return forretningshendelse.fødselsdato().minusDays(1).isBefore(tilkjentYtelseTom);
        }
        return fagsak.erÅpen() || FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) && behandlingRepository.finnSisteInnvilgetBehandling(
            fagsak.getId()).isPresent();

    }

    private boolean erFagsakPassendeForFamilieHendelse(LocalDate fødsel, Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(b -> familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(b.getId(), fødsel))
            .orElse(Boolean.FALSE);
    }

}

