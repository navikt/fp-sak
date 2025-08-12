package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ApplicationScoped
public class DokumentBestillerTjeneste extends AbstractDokumentBestillerTjeneste {

    private BehandlingRepository behandlingRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private DokumentBestiller dokumentBestiller;

    DokumentBestillerTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerTjeneste(BehandlingRepository behandlingRepository,
                                     KlageRepository klageRepository,
                                     DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                     DokumentBestiller dokumentBestiller) {
        super(klageRepository);
        this.behandlingRepository = behandlingRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.dokumentBestiller = dokumentBestiller;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        var behandlingResultat = behandlingVedtak.getBehandlingsresultat();

        var behandling = behandlingRepository.hentBehandling(behandlingResultat.getBehandlingId());

        var behandlingResultatType = behandlingResultat.getBehandlingResultatType();

        var dokumentMal = velgDokumentMalForVedtak(behandling, behandlingResultatType,
            behandlingVedtak.getVedtakResultatType(), behandlingVedtak.isBeslutningsvedtak(), finnKlageVurdering(behandling));

        DokumentMalType journalførSom = null; // settes kun ved fritekst
        if (Vedtaksbrev.FRITEKST.equals(behandlingResultat.getVedtaksbrev())) {
            if (dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandling.getId()).isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Overstyring av vedtakbrev mangler!");
            }
            journalførSom = endretVedtakOgKunEndringIFordeling(behandlingResultatType, behandlingResultat.getKonsekvenserForYtelsen())
                ? DokumentMalType.ENDRING_UTBETALING
                : dokumentMal;
            dokumentMal = DokumentMalType.VEDTAKSBREV_FRITEKST_HTML;
        }

        bestillDokument(DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSom)
            .build());
    }

    public void bestillDokument(DokumentBestilling dokumentBestilling) {
        dokumentBestiller.bestillDokument(dokumentBestilling);
    }

    private static boolean endretVedtakOgKunEndringIFordeling(BehandlingResultatType resultatType,
                                                              List<KonsekvensForYtelsen> konsekvensForYtelsenList) {
        return foreldrepengerErEndret(resultatType) && erKunEndringIFordelingAvYtelsen(konsekvensForYtelsenList);
    }
}
