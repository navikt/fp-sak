package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentKafkaBestiller;

@ApplicationScoped
public class DokumentBestillerApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;

    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private Unleash unleash;

    public DokumentBestillerApplikasjonTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerApplikasjonTjeneste(BehandlingRepository behandlingRepository,
                                                KlageRepository klageRepository,
                                                AnkeRepository ankeRepository,
                                                BrevHistorikkinnslag brevHistorikkinnslag,
                                                DokumentKafkaBestiller dokumentKafkaBestiller,
                                                Unleash unleash) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentKafkaBestiller = dokumentKafkaBestiller;
        this.unleash = unleash;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        final Behandlingsresultat behandlingsresultat = behandlingVedtak.getBehandlingsresultat();

        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }

        DokumentMalType dokumentMal = velgDokumentMalForVedtak(behandlingsresultat, behandlingVedtak, klageRepository, ankeRepository, unleash);
        dokumentKafkaBestiller.bestillBrev(behandlingsresultat.getBehandling(), dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        bestillDokument(bestillBrevDto, aktør, false);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør, boolean manueltBrev) {
        if (manueltBrev) {
            final Behandling behandling = behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId());
            brevHistorikkinnslag.opprettHistorikkinnslagForManueltBestiltBrev(aktør, behandling, bestillBrevDto.getBrevmalkode());
        }
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
    }
}
