package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ApplicationScoped
public class DokumentBestillerTjeneste {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private DokumentBestiller dokumentBestiller;

    public DokumentBestillerTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerTjeneste(BehandlingRepository behandlingRepository,
                                     KlageRepository klageRepository,
                                     DokumentBestiller dokumentBestiller) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.dokumentBestiller = dokumentBestiller;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        var behandlingsresultat = behandlingVedtak.getBehandlingsresultat();

        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingsresultat.getBehandlingId());
        var dokumentMal = velgDokumentMalForVedtak(behandling, behandlingsresultat, behandlingVedtak, klageRepository);

        dokumentBestiller.bestillBrev(behandling, dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        dokumentBestiller.bestillBrev(bestillBrevDto, aktør);
    }
}
