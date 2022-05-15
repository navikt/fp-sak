package no.nav.foreldrepenger.mottak.sakskompleks;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.ytelse.beregning.fp.BeregnFeriepenger;

/*
 *
 * Denne tjenesten kalles når en behandling avsluttes ved vedtak eller manuell henleggelse
 * - Sjekker om det skal opprettes berørt behandling på annen part
 * - Håndterer behandlingskøen i sakskomplekset
 *
 */
@ApplicationScoped
public class BerørtBehandlingKontroller {

    private static final Logger LOG = LoggerFactory.getLogger(BerørtBehandlingKontroller.class);

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingRepository behandlingRepository;
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository tilkjentRepository;
    private BeregnFeriepenger beregnFeriepenger;
    private FagsakLåsRepository fagsakLåsRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private KøKontroller køKontroller;

    @Inject
    public BerørtBehandlingKontroller(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      BerørtBehandlingTjeneste berørtBehandlingTjeneste,
                                      Behandlingsoppretter behandlingsoppretter,
                                      @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) BeregnFeriepenger beregnFeriepenger,
                                      KøKontroller køKontroller) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRevurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.berørtBehandlingTjeneste = berørtBehandlingTjeneste;
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.behandlingsoppretter = behandlingsoppretter;
        this.tilkjentRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.beregnFeriepenger = beregnFeriepenger;
        this.køKontroller = køKontroller;
    }

    BerørtBehandlingKontroller() {
        // NOSONAR
    }

    public void vurderNesteOppgaveIBehandlingskø(Long behandlingId) {
        var fagsakBruker = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        var fagsakPåMedforelder = behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsakBruker);
        if (fagsakPåMedforelder.isPresent()) {
            var kobletFagsak = fagsakPåMedforelder.get();
            håndterKøForMedforelder(kobletFagsak, fagsakBruker, behandlingId);
        } else {
            håndterKø(fagsakBruker);
        }
    }

    private void håndterKøForMedforelder(Fagsak fagsakMedforelder, Fagsak fagsakBruker, Long behandlingIdBruker) {
        var sistVedtatteYtelsesbehandlingMedforelder = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())
            .filter(this::ikkeAvslått);

        if (sistVedtatteYtelsesbehandlingMedforelder.isPresent()) {
            LOG.info("Siste vedtatte ytelse for annen part: behandlingId {}", sistVedtatteYtelsesbehandlingMedforelder.get().getId());
            var avsluttetBehandlingsresultatBruker = behandlingsresultatRepository.hent(behandlingIdBruker);
            var skalBerørtBehandlingOpprettes = berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(avsluttetBehandlingsresultatBruker,
                behandlingIdBruker, sistVedtatteYtelsesbehandlingMedforelder.get().getId());
            if (skalBerørtBehandlingOpprettes) {
                opprettBerørtBehandling(fagsakMedforelder, avsluttetBehandlingsresultatBruker);
            } else {
                if (skalFeriepengerReberegnesForMedForelder(fagsakMedforelder, sistVedtatteYtelsesbehandlingMedforelder.get(), behandlingIdBruker)) {
                    LOG.info("REBEREGN FERIEPENGER oppretter reberegning av sak {} pga behandling {}", fagsakMedforelder.getSaksnummer(), behandlingIdBruker);
                    opprettFerieBerørtBehandling(fagsakMedforelder, avsluttetBehandlingsresultatBruker);
                } else {
                    håndterKø(fagsakMedforelder);
                }
            }
        } else {
            håndterKø(fagsakBruker);
        }
    }

    private boolean ikkeAvslått(Behandling b) {
        // For tilfelle der saker er koblet - men ene partens er avslått, fx pga innvilget ES.
        return behandlingsresultatRepository.hentHvisEksisterer(b.getId())
            .map(Behandlingsresultat::getBehandlingResultatType)
            .filter(BehandlingResultatType.AVSLÅTT::equals)
            .isEmpty();
    }

    /**
     * Oppretter historikkinnslag på medforelders behandling. Type innslag baserer seg på brukers behandlingsresultat
     */
    private void opprettHistorikkinnslag(Behandling behandlingMedForelder,
                                         Behandlingsresultat behandlingsresultatBruker) {
        if (behandlingsresultatBruker.isEndretStønadskonto()) {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder, HistorikkBegrunnelseType.BERORT_BEH_ENDRING_DEKNINGSGRAD);
        } else if (berørtBehandlingTjeneste.harKonsekvens(behandlingsresultatBruker, KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER)) {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder, HistorikkBegrunnelseType.BERORT_BEH_OPPHOR);
        } else {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        }
    }

    private void opprettBerørtBehandling(Fagsak fagsakMedforelder, Behandlingsresultat behandlingsresultatBruker) {
        fagsakLåsRepository.taLås(fagsakMedforelder.getId());
        // Låse behandling som potensielt skal settes på vent
        behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakMedforelder.getId()).ifPresent(behandlingRepository::taSkriveLås);
        var åpenTømUttak = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId()).stream()
            .anyMatch(SpesialBehandling::erOppsagtUttak);
        if (åpenTømUttak) {
            // Holder på å tømme uttaket. Ingen vits med berørt behandling
            LOG.info("OPPRETT BERØRT finnes allerede åpen behandling som tømmer uttak for sak {}", fagsakMedforelder.getSaksnummer());
            return;
        }
        // Hvis det nå allerede skulle være en åpen behandling (ikke i kø) så legg den i kø før oppretting av berørt.
        var åpenBerørtBehandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId()).stream()
            .filter(SpesialBehandling::erBerørtBehandling)
            .findFirst();
        if (åpenBerørtBehandling.isPresent()) {
            LOG.info("OPPRETT BERØRT finnes allerede åpen berørt for sak {}  behandling {}", fagsakMedforelder.getSaksnummer(), åpenBerørtBehandling.get().getId());
            var origBehandlingId = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId()).orElseThrow().getId();
            if (åpenBerørtBehandling.flatMap(Behandling::getOriginalBehandlingId).filter(origBehandlingId::equals).isPresent()) {
                // Allerede opprettet berørt med samme originalbehandling. Kafka-issue.
                LOG.info("OPPRETT BERØRT oppretter ikke ny berørt for sak {}", fagsakMedforelder.getSaksnummer());
                return;
            }
        }
        // Stans eventuelle åpne feriepengereberegninger - de håndteres av ny berørt behandling
        behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId()).stream()
            .filter(SpesialBehandling::erJusterFeriepenger)
            .forEach(behandlingsoppretter::henleggBehandling);

        var åpenBehandling = behandlingRevurderingRepository.finnÅpenYtelsesbehandling(fagsakMedforelder.getId());
        var berørtBehandling = behandlingsoppretter.opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        opprettHistorikkinnslag(berørtBehandling, behandlingsresultatBruker);
        køKontroller.submitBerørtBehandling(berørtBehandling, åpenBehandling);
    }

    private void opprettFerieBerørtBehandling(Fagsak fagsakMedforelder, Behandlingsresultat behandlingsresultatBruker) {
        fagsakLåsRepository.taLås(fagsakMedforelder.getId());
        var berørtBehandling = behandlingsoppretter.opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.REBEREGN_FERIEPENGER);
        opprettHistorikkinnslag(berørtBehandling, behandlingsresultatBruker);
        køKontroller.submitBerørtBehandling(berørtBehandling, Optional.empty());
    }

    private boolean skalFeriepengerReberegnesForMedForelder(Fagsak fagsakMedforelder, Behandling sisteVedtatteMedForelder, Long behandlingIdBruker) {
        var avsluttetErReberegn = behandlingRepository.hentBehandling(behandlingIdBruker).harBehandlingÅrsak(BehandlingÅrsakType.REBEREGN_FERIEPENGER);
        if (!behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId()).isEmpty()) return false;
        var gjeldendeTilkjent = tilkjentRepository.hentUtbetBeregningsresultat(sisteVedtatteMedForelder.getId()).orElse(null);
        if (gjeldendeTilkjent == null) return false;
        var harAvvikFeriepenger = beregnFeriepenger.avvikBeregnetFeriepengerBeregningsresultat(sisteVedtatteMedForelder, gjeldendeTilkjent);
        if (avsluttetErReberegn && harAvvikFeriepenger) {
            LOG.warn("REBEREGN FERIEPENGER potensiell cascade avsluttet behandlingId {} sak medforelder {}", behandlingIdBruker, fagsakMedforelder.getSaksnummer());
            return false;
        }
        return harAvvikFeriepenger;
    }

    private void håndterKø(Fagsak fagsak) {
        køKontroller.håndterSakskompleks(fagsak);
    }


}
