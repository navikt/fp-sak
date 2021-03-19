package no.nav.foreldrepenger.mottak.registrerer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@ApplicationScoped
public class DokumentRegistrererTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private MottattDokumentPersisterer mottattDokumentPersisterer;

    DokumentRegistrererTjeneste() {
        // CDI
    }

    @Inject
    public DokumentRegistrererTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       OppgaveTjeneste oppgaveTjeneste,
                                       OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                       MottattDokumentPersisterer mottattDokumentPersisterer) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
    }

    public Optional<AksjonspunktDefinisjon> aksjonspunktManuellRegistrering(Behandling behandling, ManuellRegistreringAksjonspunktDto adapter) {
        return new ManuellRegistreringAksjonspunkt(mottatteDokumentRepository, oppgaveTjeneste, oppgaveBehandlingKoblingRepository,
            mottattDokumentPersisterer)
            .oppdater(behandling, adapter);
    }

}
