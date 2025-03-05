package no.nav.foreldrepenger.mottak.registrerer;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;

@ApplicationScoped
public class DokumentRegistrererTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private MottattDokumentPersisterer mottattDokumentPersisterer;
    private BehandlingRepository behandlingRepository;

    @Inject
    public DokumentRegistrererTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       MottattDokumentPersisterer mottattDokumentPersisterer,
                                       BehandlingRepository behandlingRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
        this.behandlingRepository = behandlingRepository;
    }

    DokumentRegistrererTjeneste() {
        // CDI
    }

    public void aksjonspunktManuellRegistrering(BehandlingReferanse behandlingReferanse,
                                                String søknadsXml, DokumentTypeId dokumentTypeId, LocalDate mottattDato) {
        var dokument = new MottattDokument.Builder()
            .medDokumentType(dokumentTypeId)
            .medDokumentKategori(DokumentKategori.SØKNAD)
            .medElektroniskRegistrert(false)
            .medMottattDato(mottattDato)
            .medXmlPayload(søknadsXml)
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medFagsakId(behandlingReferanse.fagsakId())
            .build();

        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        mottattDokumentPersisterer.persisterDokumentinnhold(dokument, behandling);
        mottatteDokumentRepository.lagre(dokument);
    }

}
