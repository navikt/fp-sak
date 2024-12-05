package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ApplicationScoped
public class DokumentBestillerTjeneste extends AbstractDokumentBestillerTjeneste {

    private BehandlingRepository behandlingRepository;
    private DokumentBestiller dokumentBestiller;

    DokumentBestillerTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerTjeneste(BehandlingRepository behandlingRepository,
                                     KlageRepository klageRepository,
                                     DokumentBestiller dokumentBestiller) {
        super(klageRepository);
        this.behandlingRepository = behandlingRepository;
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
            journalførSom = endretVedtakOgKunEndringIFordeling(behandlingResultatType,
                behandlingResultat.getKonsekvenserForYtelsen()) ? DokumentMalType.ENDRING_UTBETALING : dokumentMal;
            dokumentMal = DokumentMalType.FRITEKSTBREV;
        }

        bestillDokument(DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSom)
            .build(), HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(DokumentBestilling dokumentBestilling, HistorikkAktør aktør) {
        dokumentBestiller.bestillDokument(dokumentBestilling, aktør);
    }

    private static boolean endretVedtakOgKunEndringIFordeling(BehandlingResultatType resultatType,
                                                              List<KonsekvensForYtelsen> konsekvensForYtelsenList) {
        return foreldrepengerErEndret(resultatType) && erKunEndringIFordelingAvYtelsen(konsekvensForYtelsenList);
    }
}
