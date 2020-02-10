package no.nav.foreldrepenger.mottak.registrerer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@ApplicationScoped
public class DokumentRegistrererTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private DokumentPersistererTjeneste dokumentPersistererTjeneste;

    DokumentRegistrererTjeneste() {
        // CDI
    }

    @Inject
    public DokumentRegistrererTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       OppgaveTjeneste oppgaveTjeneste,
                                       OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                       DokumentPersistererTjeneste dokumentPersistererTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
    }

    public Optional<AksjonspunktDefinisjon> aksjonspunktManuellRegistrering(Behandling behandling, ManuellRegistreringAksjonspunktDto adapter) {
        return new ManuellRegistreringAksjonspunkt(mottatteDokumentRepository, oppgaveTjeneste, oppgaveBehandlingKoblingRepository, dokumentPersistererTjeneste)
            .oppdater(behandling, adapter);
    }

}
