package no.nav.foreldrepenger.kompletthet.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class EtterlysInntektsmeldingTjeneste {

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    public EtterlysInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysInntektsmeldingTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste, DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }


    public void etterlysInntektsmeldingHvisIkkeAlleredeSendt(BehandlingReferanse ref) {
        if (erEtterlysInntektsmeldingBrevSendt(ref.behandlingId())) {
            return;
        }
        sendEtterlysInntektsmeldingBrev(ref);
    }

    private void sendEtterlysInntektsmeldingBrev(BehandlingReferanse ref) {
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(ref.behandlingUuid())
            .medSaksnummer(ref.saksnummer())
            .medDokumentMal(DokumentMalType.ETTERLYS_INNTEKTSMELDING)
            .build();
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
    }

    private boolean erEtterlysInntektsmeldingBrevSendt(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.ETTERLYS_INNTEKTSMELDING);
    }
}
