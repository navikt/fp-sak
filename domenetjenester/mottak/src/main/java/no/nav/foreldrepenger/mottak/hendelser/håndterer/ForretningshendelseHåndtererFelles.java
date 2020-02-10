package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;

@Dependent
public class ForretningshendelseHåndtererFelles {

    private Behandlingsoppretter behandlingsoppretter;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private Kompletthetskontroller kompletthetskontroller;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private KøKontroller køKontroller;

    @SuppressWarnings("unused")
    private ForretningshendelseHåndtererFelles() { // NOSONAR
        // For CDI
    }

    @Inject
    public ForretningshendelseHåndtererFelles(HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                              Kompletthetskontroller kompletthetskontroller,
                                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                              Behandlingsoppretter behandlingsoppretter,
                                              KøKontroller køKontroller) {
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.kompletthetskontroller = kompletthetskontroller;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.køKontroller = køKontroller;
    }

    public Behandling opprettRevurderingLagStartTask(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        return revurdering;
    }

    public void fellesHåndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(åpenBehandling, behandlingÅrsakType);
        kompletthetskontroller.vurderNyForretningshendelse(åpenBehandling);
    }

    public void fellesHåndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> køetBehandlingOpt) {
        if (køetBehandlingOpt.isPresent()) {
            // Oppdateringer fanges opp etter at behandling tas av kø, ettersom den vil passere steg innhentregisteropplysninger
            return;
        }
        Behandling køetBehandling = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        historikkinnslagTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(køetBehandling, HistorikkinnslagType.BEH_KØET, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
        kompletthetskontroller.oppdaterKompletthetForKøetBehandling(køetBehandling);
        køKontroller.enkøBehandling(køetBehandling);
    }
}
