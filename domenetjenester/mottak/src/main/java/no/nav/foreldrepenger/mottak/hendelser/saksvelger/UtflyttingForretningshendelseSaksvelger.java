package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
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
import no.nav.foreldrepenger.mottak.hendelser.freg.UtflyttingForretningshendelse;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.UTFLYTTING)
public class UtflyttingForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<UtflyttingForretningshendelse> {

    private static final Logger LOG = LoggerFactory.getLogger(UtflyttingForretningshendelseSaksvelger.class);



    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public UtflyttingForretningshendelseSaksvelger(BehandlingRepositoryProvider repositoryProvider,
                                                   FamilieHendelseTjeneste familieHendelseTjeneste,
                                                   HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(UtflyttingForretningshendelse forretningshendelse) {

        var saker = forretningshendelse.aktørIdListe().stream()
            .flatMap(aktørId -> fagsakRepository.hentForBruker(aktørId).stream())
            .filter(f -> erFagsakPassendeForUtflyttingHendelse(forretningshendelse, f))
            .toList();

        if (Endringstype.ANNULLERT.equals(forretningshendelse.endringstype())
            || Endringstype.KORRIGERT.equals(forretningshendelse.endringstype())) {
            saker.forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, "Endrede opplysninger om utflytting i Folkeregisteret"));
        } else if (Endringstype.OPPRETTET.equals(forretningshendelse.endringstype())) {
            var begrunnelse = String.format("Folkeregisteret har registrert utflyttingsdato %s",
                HistorikkinnslagLinjeBuilder.format(forretningshendelse.utflyttingsdato()));
            saker.forEach(f -> historikkinnslagTjeneste.opprettHistorikkinnslagForEndringshendelse(f, begrunnelse));
        }

        return Map.of(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING, saker);
    }


    private boolean erFagsakPassendeForUtflyttingHendelse(UtflyttingForretningshendelse forretningshendelse, Fagsak fagsak) {
        if (forretningshendelse.utflyttingsdato() == null) {
            LOG.info("Hendelser: Utflyttingshendelse uten utflyttingsdato {}", forretningshendelse);
            return false;
        }
        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsak.getId())) return true;
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            // Utflytting før fødsel
            return behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId())
                .filter(b -> familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(b.getId(), forretningshendelse.utflyttingsdato()))
                .isPresent();
        }

        // Utflytting mens det finnes innvilget ytelse
        var ytelseTom = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> beregningsresultatRepository.hentUtbetBeregningsresultat(b.getId()))
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder()).orElse(Tid.TIDENES_BEGYNNELSE);
        return forretningshendelse.utflyttingsdato().minusDays(1).isBefore(ytelseTom);
    }
}

