package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

import java.util.List;

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

        var behandling = behandlingRepository.hentBehandling(behandlingsresultat.getBehandlingId());

        DokumentMalType opprinneligDokumentMal = null;
        var dokumentMal = velgDokumentMalForVedtak(behandling, behandlingsresultat.getBehandlingResultatType(),
            behandlingVedtak.getVedtakResultatType(), behandlingVedtak.isBeslutningsvedtak(), finnKlageVurdering(behandling));

        if (Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev())) {
            opprinneligDokumentMal = endretVedtakOgKunEndringIFordeling(behandlingsresultat) ? DokumentMalType.ENDRING_UTBETALING : dokumentMal;
            dokumentMal = DokumentMalType.FRITEKSTBREV;
        }

        bestillVedtak(BrevBestilling.builder().medBehandlingUuid(behandling.getUuid()).medDokumentMal(dokumentMal).build(),
            opprinneligDokumentMal,
            HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BrevBestilling brevBestilling, HistorikkAktør aktør) {
        dokumentBestiller.bestillDokument(brevBestilling, aktør);
    }

    private void bestillVedtak(BrevBestilling brevBestilling, DokumentMalType opprinneligDokumentMal, HistorikkAktør aktør) {
        dokumentBestiller.bestillVedtak(brevBestilling, opprinneligDokumentMal, aktør);
    }

    private boolean endretVedtakOgKunEndringIFordeling(Behandlingsresultat behandlingsresultat) {
        return BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingsresultat.getBehandlingResultatType())
            && erKunEndringIFordelingAvYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
    }

    private static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsen) {
        return konsekvensForYtelsen.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsen.size() == 1;
    }

    private KlageVurdering finnKlageVurdering(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling).map(KlageVurderingResultat::getKlageVurdering).orElse(null);
        }
        return null;
    }
}
