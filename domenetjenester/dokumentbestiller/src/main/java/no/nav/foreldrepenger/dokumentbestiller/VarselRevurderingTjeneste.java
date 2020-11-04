package no.nav.foreldrepenger.dokumentbestiller;

import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class VarselRevurderingTjeneste {

    private Period defaultVenteFrist;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;

    VarselRevurderingTjeneste() {
        // CDI
    }

    @Inject
    public VarselRevurderingTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     OppgaveTjeneste oppgaveTjeneste,
                                     OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    public void håndterVarselRevurdering(Behandling behandling, VarselRevurderingAksjonspunktDto adapter) {
        new VarselRevurderingHåndterer(defaultVenteFrist, oppgaveBehandlingKoblingRepository, oppgaveTjeneste,
            behandlingskontrollTjeneste, dokumentBestillerTjeneste).oppdater(behandling, adapter);
    }
}
