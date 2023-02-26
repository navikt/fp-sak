package no.nav.foreldrepenger.mottak.registrerer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;

@ApplicationScoped
public class DokumentRegistrererTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private MottattDokumentPersisterer mottattDokumentPersisterer;

    DokumentRegistrererTjeneste() {
        // CDI
    }

    @Inject
    public DokumentRegistrererTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       MottattDokumentPersisterer mottattDokumentPersisterer) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
    }

    public Optional<AksjonspunktDefinisjon> aksjonspunktManuellRegistrering(Behandling behandling, ManuellRegistreringAksjonspunktDto adapter) {
        if (adapter.getErFullstendigSøknad()) {
            var dokument = new MottattDokument.Builder()
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
        }

        return Optional.of(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
    }

}
