package no.nav.foreldrepenger.mottak.vedtak;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ApplicationScoped
public class BerørtBehandlingKontroller {

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingRepository behandlingRepository;
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FagsakLåsRepository fagsakLåsRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SøknadRepository søknadRepository;
    private Behandlingsoppretter behandlingsoppretter;

    @Inject
    public BerørtBehandlingKontroller(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                      BerørtBehandlingTjeneste berørtBehandlingTjeneste,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                      Behandlingsoppretter behandlingsoppretter) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRevurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.berørtBehandlingTjeneste = berørtBehandlingTjeneste;
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.ytelsesFordelingRepository = behandlingRepositoryProvider.getYtelsesFordelingRepository();
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
        this.behandlingsoppretter = behandlingsoppretter;
    }

    BerørtBehandlingKontroller() {
        // NOSONAR
    }

    public void vurderNesteOppgaveIBehandlingskø(Long behandlingId) {
        var fagsakBruker = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        var fagsakPåMedforelder = finnKobletFagsak(fagsakBruker);
        if (fagsakPåMedforelder.isPresent()) {
            var kobletFagsak = fagsakPåMedforelder.get();
            håndterKøForMedforelder(kobletFagsak, fagsakBruker, behandlingId);
        } else {
            håndterKøForBruker(fagsakBruker);
        }
    }

    private void håndterKøForMedforelder(Fagsak fagsakMedforelder, Fagsak fagsakBruker, Long behandlingIdBruker) {
        var innvilgetYtelsesbehandlingMedforelder = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(
            fagsakMedforelder.getId());

        if (innvilgetYtelsesbehandlingMedforelder.isPresent() && !finnesKøetBehandling(fagsakBruker)) {
            var behandlingsresultatBruker = behandlingsresultatRepository.hent(behandlingIdBruker);
            var skalBerørtBehandlingOpprettes = berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(
                behandlingsresultatBruker, behandlingIdBruker, innvilgetYtelsesbehandlingMedforelder.get().getId());
            if (skalBerørtBehandlingOpprettes) {
                opprettBerørtBehandling(fagsakMedforelder, behandlingsresultatBruker);
            } else {
                håndterKø(fagsakMedforelder);
            }
        } else {
            håndterKø(fagsakMedforelder);
        }
    }

    private boolean finnesKøetBehandling(Fagsak fagsak) {
        return finnKøetBehandling(fagsak.getId()).isPresent();
    }

    /**
     * Oppretter historikkinnslag på medforelders behandling. Type innslag baserer seg på brukers behandlingsresultat
     */
    private void opprettHistorikkinnslag(Behandling behandlingMedForelder,
                                         Behandlingsresultat behandlingsresultatBruker) {
        if (behandlingsresultatBruker.isEndretStønadskonto()) {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder,
                HistorikkBegrunnelseType.BERORT_BEH_ENDRING_DEKNINGSGRAD);
        } else if (berørtBehandlingTjeneste.harKonsekvens(behandlingsresultatBruker,
            KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER)) {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder,
                HistorikkBegrunnelseType.BERORT_BEH_OPPHOR);
        } else {
            berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(behandlingMedForelder,
                BehandlingÅrsakType.BERØRT_BEHANDLING);
        }
    }

    private void opprettBerørtBehandling(Fagsak fagsakMedforelder, Behandlingsresultat behandlingsresultatBruker) {
        fagsakLåsRepository.taLås(fagsakMedforelder.getId());
        // Hvis det nå allerede skulle være en åpen behandling (ikke i kø) så legg den i kø før oppretting av berørt.
        behandlingRevurderingRepository.finnÅpenYtelsesbehandling(fagsakMedforelder.getId())
            .ifPresent(
                b -> behandlingskontrollTjeneste.settBehandlingPåVent(b, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING,
                    null, null, Venteårsak.VENT_ÅPEN_BEHANDLING));
        var revurderingMedForelder = behandlingsoppretter.opprettRevurdering(fagsakMedforelder,
            BehandlingÅrsakType.BERØRT_BEHANDLING);
        opprettHistorikkinnslag(revurderingMedForelder, behandlingsresultatBruker);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurderingMedForelder);
    }

    private void håndterKø(Fagsak fagsak) {
        var køetBehandlingOpt = finnKøetBehandling(fagsak.getId());
        if (køetBehandlingOpt.isPresent()) {
            var køetBehandling = køetBehandlingOpt.get();
            if (harKøetBehandlingAksjonspunktForForTidligSøknadUtførtMenFristFremITid(køetBehandling)) {
                var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(køetBehandling);
                behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst,
                    køetBehandling.getAktivtBehandlingSteg(),
                    køetBehandling.getAksjonspunktFor(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING),
                    "Berørt behandling er fullført");
                behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst,
                    List.of(køetBehandling.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)), true,
                    false);
                berørtBehandlingTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(køetBehandling,
                    HistorikkinnslagType.BEH_VENT,
                    køetBehandling.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD).getFristTid(),
                    Venteårsak.FOR_TIDLIG_SOKNAD);
                behandlingProsesseringTjeneste.opprettTasksForStartBehandling(køetBehandling);
            } else {
                dekøBehandling(køetBehandling);
            }
        } else {
            // Ta fra kø til annen forelder når egen kø er tom
            finnKøetBehandlingMedforelder(fagsak).ifPresent(this::dekøBehandling);
        }
    }

    private boolean harKøetBehandlingAksjonspunktForForTidligSøknadUtførtMenFristFremITid(Behandling behandling) {
        return behandling.getAksjonspunkter()
            .stream()
            .anyMatch(a -> a.erUtført() && a.getAksjonspunktDefinisjon()
                .getKode()
                .equals(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD.getKode()) && a.getFristTid()
                .isAfter(LocalDateTime.now()));
    }

    private void håndterKøForBruker(Fagsak fagsak) {
        var køetBehandling = finnKøetBehandling(fagsak.getId());
        køetBehandling.ifPresent(this::dekøBehandling);
    }

    //OBS: Endrer du noe her vil du antagelig også ønske å endre det i KøKontroller.oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling()
    // - kan disse slås sammen?
    private void dekøBehandling(Behandling behandling) {
        var originalBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(
            behandling.getFagsakId());
        if (originalBehandling.isPresent() && behandling.getOriginalBehandlingId().isPresent()
            && !behandling.getOriginalBehandlingId().get().equals(originalBehandling.get().getId())) {

            var oppdatertBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling,
                finnFørsteÅrsak(behandling));

            if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
                // Må ha YF og Søknad fra original ettersom berørt ikke har med evt endringssøknad da den snek i køen.
                ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(),
                    oppdatertBehandling.getId());
                søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling, oppdatertBehandling);
            }
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(oppdatertBehandling);
            return;
        }
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingSettUtført(behandling,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    private BehandlingÅrsakType finnFørsteÅrsak(Behandling behandling) {
        return behandling.getBehandlingÅrsaker()
            .stream()
            .min(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
            .map(første -> første.getBehandlingÅrsakType())
            .orElse(BehandlingÅrsakType.UDEFINERT);
    }

    private Optional<Behandling> finnKøetBehandling(Long fagsakId) {
        return behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakId);
    }

    private Optional<Behandling> finnKøetBehandlingMedforelder(Fagsak fagsak) {
        return behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak);
    }

    private Optional<Fagsak> finnKobletFagsak(Fagsak fagsak) {
        return behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak);
    }
}
