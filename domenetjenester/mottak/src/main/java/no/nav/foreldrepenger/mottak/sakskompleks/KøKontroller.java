package no.nav.foreldrepenger.mottak.sakskompleks;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/*
 *
 * Denne tjenesten kalles for å håndtere kø av revurderinger / behandlinger
 * - Førstegangsbehandlinger skal ikke legges i kø - det skjer evt der man går inn i Uttak
 * - Endringssøknader skal køhåndteres ved start - andre revurderinger går til synkpunkt ved Uttak
 * - Nye revuderinger legges i kø hvis  finnes åpen berørt eller annenpart har åpen behandling
 * - Når revurderinger tas ut av kø må det sjekkes om det har vært en berørt og man trenger ny originalbehandling
 *
 */
@Dependent
public class KøKontroller {

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private SøknadRepository søknadRepository;
    private BehandlingFlytkontroll flytkontroll;
    private ProsessTaskTjeneste taskTjeneste;

    public KøKontroller() {
        // For CDI proxy
    }

    @Inject
    public KøKontroller(BehandlingProsesseringTjeneste prosesseringTjeneste,
                        BehandlingRepositoryProvider repositoryProvider,
                        ProsessTaskTjeneste taskTjeneste,
                        BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                        Behandlingsoppretter behandlingsoppretter,
                        BehandlingFlytkontroll flytkontroll) {
        this.behandlingProsesseringTjeneste = prosesseringTjeneste;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.taskTjeneste = taskTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.flytkontroll = flytkontroll;
    }


    public void dekøFørsteBehandlingISakskompleks(Behandling behandling) {
        var køetBehandlingMedforelder = behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(behandling.getFagsak());
        var medforelderEndringsSøknad = køetBehandlingMedforelder.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).isPresent();
        if (medforelderEndringsSøknad && BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(køetBehandlingMedforelder.get());
            opprettTaskForÅStarteBehandling(behandling);
        } else if (medforelderEndringsSøknad) {
            // Legger nyopprettet behandling i kø, siden denne ikke skal behandles nå
            enkøBehandling(behandling);
            oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(køetBehandlingMedforelder.get());
        } else {
            opprettTaskForÅStarteBehandling(behandling);
        }
    }

    public void enkøBehandling(Behandling behandling) {
        behandlingProsesseringTjeneste.enkøBehandling(behandling);
    }

    public void submitBerørtBehandling(Behandling behandling, Optional<Behandling> åpenBehandling) {
        if (!behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())) throw new IllegalArgumentException("Behandling er ikke berørt");
        åpenBehandling.ifPresent(b -> behandlingProsesseringTjeneste.enkøBehandling(b));
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
    }

    // Ta høyde for ikke-køet revurdering og køet førstegangsbehandling. Ikke forutsett køing utelukkende bak berørt eller annenpart.
    // Koden as-is forutsetter en-og-en berørt (disse setter andre i kø).
    public void håndterSakskompleks(Fagsak fagsak) {
        var aktivBehandling = behandlingRevurderingTjeneste.finnÅpenYtelsesbehandling(fagsak.getId());
        var aktivBehandlingMedforelder = behandlingRevurderingTjeneste.finnÅpenBehandlingMedforelder(fagsak);
        if (aktivBehandling.isPresent() || aktivBehandlingMedforelder.isPresent()) {
            return;
        }
        var køetBehandling = behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId());
        var køetBehandlingMedforelder = behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsak);
        var køetBerørt = sjekkKøetBerørt(køetBehandling, køetBehandlingMedforelder);
        if (køetBerørt.isPresent()) {
            behandlingProsesseringTjeneste.dekøBehandling(køetBerørt.get());
            return;
        }
        var nesteBehandling = finnTidligsteSøknad(køetBehandling, køetBehandlingMedforelder)
            .or(() -> finnTidligstOpprettet(køetBehandling, køetBehandlingMedforelder));
        nesteBehandling.ifPresent(b -> {
            if (skalOppdatereKøetBehandling(b)) {
                lagreOppdaterKøetProsesstask(b);
            } else {
                behandlingProsesseringTjeneste.dekøBehandling(b);
            }
        });
    }

    private Optional<Behandling> sjekkKøetBerørt(Optional<Behandling> behandling1, Optional<Behandling> behandling2) {
        var ts1 = behandling1.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).map(Behandling::getOpprettetTidspunkt);
        var ts2 = behandling2.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).map(Behandling::getOpprettetTidspunkt);
        if (ts1.isEmpty() && ts2.isEmpty()) return Optional.empty();
        if (ts1.isPresent() && ts2.isPresent()) {
            return ts1.get().isBefore(ts2.get()) ? behandling1 : behandling2;
        }
        return ts1.isPresent() ? behandling1 : behandling2;
    }

    // OBS: Filter i tilfelle førstegang - de har ikke behandlingsårsak (dvs førstegang eller årsak endring)
    private Optional<Behandling> finnTidligsteSøknad(Optional<Behandling> behandling1, Optional<Behandling> behandling2) {
        var ts1 = behandling1.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
            .flatMap(b -> søknadRepository.hentSøknadHvisEksisterer(b.getId())).map(SøknadEntitet::getMottattDato);
        var ts2 = behandling2.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
            .flatMap(b -> søknadRepository.hentSøknadHvisEksisterer(b.getId())).map(SøknadEntitet::getMottattDato);
        if (ts1.isEmpty() && ts2.isEmpty()) return Optional.empty();
        if (ts1.isPresent() && ts2.isPresent()) {
            if (ts1.get().equals(ts2.get())) {
                return behandling1.orElseThrow().getOpprettetTidspunkt().isBefore(behandling2.orElseThrow().getOpprettetTidspunkt()) ? behandling1 : behandling2;
            }
            return ts1.get().isBefore(ts2.get()) ? behandling1 : behandling2;
        }
        return ts1.isPresent() ? behandling1 : behandling2;
    }

    private Optional<Behandling> finnTidligstOpprettet(Optional<Behandling> behandling1, Optional<Behandling> behandling2) {
        var ts1 = behandling1.map(Behandling::getOpprettetTidspunkt);
        var ts2 = behandling2.map(Behandling::getOpprettetTidspunkt);
        if (ts1.isEmpty() && ts2.isEmpty()) return Optional.empty();
        if (ts1.isPresent() && ts2.isPresent()) return ts1.get().isBefore(ts2.get()) ? behandling1 : behandling2;
        return ts1.isPresent() ? behandling1 : behandling2;
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING)) {
            behandlingProsesseringTjeneste.dekøBehandling(behandling);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
        }
    }

    public void oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(Long behandlingId) {
        behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(behandling);
    }

    void oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(Behandling behandling) {

        if (skalOppdatereKøetBehandling(behandling)) {
            var oppdatertBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling);

            if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
                // Må ha YF fra original ettersom berørt ikke har med evt endringssøknad da den snek i køen.
                ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), oppdatertBehandling);
                søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling, oppdatertBehandling);
            }
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(oppdatertBehandling);
        } else {
            behandlingProsesseringTjeneste.dekøBehandling(behandling);
        }
    }

    private boolean skalOppdatereKøetBehandling(Behandling behandling) {
        var originalBehandlingId = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())
            .map(Behandling::getId).orElse(null);
        return behandling.erRevurdering() && originalBehandlingId != null &&
            behandling.getOriginalBehandlingId().filter(ob -> !ob.equals(originalBehandlingId)).isPresent();
    }

    void lagreOppdaterKøetProsesstask(Behandling behandling) {
        var data = ProsessTaskData.forProsessTask(GjenopptaKøetBehandlingTask.class);
        data.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        taskTjeneste.lagre(data);
    }

    public boolean skalEvtNyBehandlingKøes(Fagsak fagsak) {
        // Finnes ingen tidligere innvilget -> Skal ikke opprettes revurdering.
        if (behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()).isEmpty()) {
            return false;
        }
        var åpenBehandling = behandlingRevurderingTjeneste.finnÅpenYtelsesbehandling(fagsak.getId());
        if (åpenBehandling.isPresent()) {
            // Køes hvis finnes berørt, ellers legg dokument på åpen behandling
            return åpenBehandling.filter(b -> SpesialBehandling.erSpesialBehandling(b) || b.harBehandlingÅrsak(BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI)).isPresent();
        }
        return behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId()).isPresent() || flytkontroll.nyRevurderingSkalVente(fagsak);
    }

}
