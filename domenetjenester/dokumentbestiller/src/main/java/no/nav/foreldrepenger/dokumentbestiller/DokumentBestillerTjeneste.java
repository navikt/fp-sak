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
public class DokumentBestillerTjeneste {

    private Period defaultVenteFrist;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;

    DokumentBestillerTjeneste() {
        // CDI
    }

    @Inject
    public DokumentBestillerTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     OppgaveTjeneste oppgaveTjeneste,
                                     OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    public void håndterVarselRevurdering(Behandling behandling, VarselRevurderingAksjonspunktDto adapter) {
        new VarselRevurderingHåndterer(defaultVenteFrist, oppgaveBehandlingKoblingRepository, oppgaveTjeneste, behandlingskontrollTjeneste, dokumentBestillerApplikasjonTjeneste)
            .oppdater(behandling, adapter);
    }
}
