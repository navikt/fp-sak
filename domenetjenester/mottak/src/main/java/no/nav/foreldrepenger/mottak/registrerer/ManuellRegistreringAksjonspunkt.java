package no.nav.foreldrepenger.mottak.registrerer;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

class ManuellRegistreringAksjonspunkt {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private MottattDokumentPersisterer mottattDokumentPersisterer;

    ManuellRegistreringAksjonspunkt(MottatteDokumentRepository mottatteDokumentRepository,
                                    OppgaveTjeneste oppgaveTjeneste,
                                    OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, MottattDokumentPersisterer mottattDokumentPersisterer) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
    }

    public Optional<AksjonspunktDefinisjon> oppdater(Behandling behandling, ManuellRegistreringAksjonspunktDto adapter) {

        if (adapter.getErFullstendigSøknad()) {
            MottattDokument dokument = new MottattDokument.Builder()
                .medDokumentType(adapter.getDokumentTypeId())
                .medDokumentKategori(DokumentKategori.SØKNAD)
                .medElektroniskRegistrert(false)
                .medMottattDato(adapter.getMottattDato())
                .medXmlPayload(adapter.getSøknadsXml())
                .medBehandlingId(behandling.getId())
                .medFagsakId(behandling.getFagsakId())
                .build();
            mottattDokumentPersisterer.persisterDokumentinnhold(dokument, behandling);
            mottatteDokumentRepository.lagre(dokument);

            return adapter.getErRegistrertVerge() ? Optional.of(AksjonspunktDefinisjon.AVKLAR_VERGE) : Optional.empty();
        } else {
            avsluttTidligereRegistreringsoppgave(behandling);

            return Optional.of(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
        }
    }

    private void avsluttTidligereRegistreringsoppgave(Behandling behandling) {
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.REGISTRER_SØKNAD, oppgaver)
            .ifPresent(aktivOppgave -> oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, aktivOppgave.getOppgaveÅrsak()));
    }
}
