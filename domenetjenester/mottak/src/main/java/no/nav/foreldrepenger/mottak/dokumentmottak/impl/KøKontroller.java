package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@Dependent
public class KøKontroller {

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private SøknadRepository søknadRepository;
    private BehandlingFlytkontroll flytkontroll;

    public KøKontroller() {
        // For CDI proxy
    }

    @Inject
    public KøKontroller(BehandlingProsesseringTjeneste prosesseringTjeneste,
                        BehandlingRevurderingRepository behandlingRevurderingRepository,
                        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                        BehandlingRepositoryProvider repositoryProvider,
                        Behandlingsoppretter behandlingsoppretter,
                        BehandlingFlytkontroll flytkontroll) {
        this.behandlingProsesseringTjeneste = prosesseringTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.behandlingsoppretter = behandlingsoppretter;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.flytkontroll = flytkontroll;
    }


    public void dekøFørsteBehandlingISakskompleks(Behandling behandling) {
        Optional<Behandling> køetBehandlingMedforelder = behandlingRevurderingRepository.finnKøetBehandlingMedforelder(behandling.getFagsak());
        boolean medforelderEndringsSøknad = køetBehandlingMedforelder.map(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).orElse(false);
        if (medforelderEndringsSøknad) {

            // Legger nyopprettet behandling i kø, siden denne ikke skal behandles nå
            enkøBehandling(behandling);
            oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(køetBehandlingMedforelder.get());

        } else {
            opprettTaskForÅStarteBehandling(behandling);
        }
    }

    public void enkøBehandling(Behandling behandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING)) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingSettUtført(behandling, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
        }
    }

    //OBS: Endrer du noe her vil du antagelig også ønske å endre det i BerørtBehandlingKontroller.dekøBehandling() - kan disse slås sammen?
    private void oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(Behandling behandling) {
        Optional<Behandling> originalBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        if (behandling.erRevurdering() && originalBehandling.isPresent() && behandling.getOriginalBehandlingId().isPresent()
            && !behandling.getOriginalBehandlingId().get().equals(originalBehandling.get().getId())) {
            Behandling oppdatertBehandling = oppdaterOriginalBehandlingVedHenleggelse(behandling);

            if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
                // Må ha YF fra original ettersom berørt ikke har med evt endringssøknad da den snek i køen.
                ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), oppdatertBehandling.getId());
                søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling, oppdatertBehandling);
            }
        } else {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingSettUtført(behandling, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
        }
    }

    private Behandling oppdaterOriginalBehandlingVedHenleggelse(Behandling behandling) {
        BehandlingÅrsakType behandlingÅrsakType;
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            behandlingÅrsakType = behandling.getBehandlingÅrsaker().stream().findFirst().get().getBehandlingÅrsakType();
        } else {
            behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;
        }
        Behandling oppdatertBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsakType);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(oppdatertBehandling);
        return oppdatertBehandling;
    }

    public boolean skalNyEvtNyBehandlingKøes(Fagsak fagsak) {
        // Finnes ingen tidligere innvilget -> Skal ikke opprettes revurdering.
        if (behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()).isEmpty()) {
            return false;
        }
        return behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId()).isPresent() || flytkontroll.nyRevurderingSkalVente(fagsak);
    }

}
