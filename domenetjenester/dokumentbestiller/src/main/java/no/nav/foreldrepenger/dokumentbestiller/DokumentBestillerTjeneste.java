package no.nav.foreldrepenger.dokumentbestiller;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentKafkaBestiller;
import no.nav.foreldrepenger.konfig.Environment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

@ApplicationScoped
public class DokumentBestillerTjeneste {

    private final static Environment ENV = Environment.current();

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;

    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private DokumentBestiller dokumentBestiller;

    public DokumentBestillerTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerTjeneste(BehandlingRepository behandlingRepository,
                                     KlageRepository klageRepository,
                                     AnkeRepository ankeRepository,
                                     BrevHistorikkinnslag brevHistorikkinnslag,
                                     DokumentKafkaBestiller dokumentKafkaBestiller,
                                     DokumentBestiller dokumentBestiller) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentKafkaBestiller = dokumentKafkaBestiller;
        this.dokumentBestiller = dokumentBestiller;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        final var behandlingsresultat = behandlingVedtak.getBehandlingsresultat();

        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingsresultat.getBehandlingId());
        var dokumentMal = velgDokumentMalForVedtak(behandling, behandlingsresultat, behandlingVedtak,
            klageRepository, ankeRepository);
        if (ENV.isProd()) {
            dokumentKafkaBestiller.bestillBrev(behandling, dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
        } else {
            dokumentBestiller.bestillBrev(behandling, dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        bestillDokument(bestillBrevDto, aktør, false);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør, boolean manueltBrev) {
        if (manueltBrev) {
            var behandling = bestillBrevDto.getBehandlingUuid() == null ? behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId())
                : behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingUuid());
            brevHistorikkinnslag.opprettHistorikkinnslagForManueltBestiltBrev(aktør, behandling,
                DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()));
        }

        if (ENV.isProd()) {
            dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        } else {
            dokumentBestiller.bestillBrev(bestillBrevDto, aktør);
        }
    }
}
