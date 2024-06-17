package no.nav.foreldrepenger.mottak.registrerer;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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

    public Optional<AksjonspunktDefinisjon> aksjonspunktManuellRegistrering(BehandlingReferanse behandlingReferanse,
                                                                            ManuellRegistreringAksjonspunktDto adapter) {
        if (adapter.getErFullstendigSøknad()) {
            var dokument = new MottattDokument.Builder().medDokumentType(adapter.getDokumentTypeId())
                .medDokumentKategori(DokumentKategori.SØKNAD)
                .medElektroniskRegistrert(false)
                .medMottattDato(adapter.getMottattDato())
                .medXmlPayload(adapter.getSøknadsXml())
                .medBehandlingId(behandlingReferanse.behandlingId())
                .medFagsakId(behandlingReferanse.fagsakId())
                .build();
            var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
            mottattDokumentPersisterer.persisterDokumentinnhold(dokument, behandling);
            mottatteDokumentRepository.lagre(dokument);

            return adapter.getErRegistrertVerge() ? Optional.of(AksjonspunktDefinisjon.AVKLAR_VERGE) : Optional.empty();
        } else if (behandlingReferanse.erRevurdering()) {
            return Optional.empty();
        } else {
            return Optional.of(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
        }
    }

}
