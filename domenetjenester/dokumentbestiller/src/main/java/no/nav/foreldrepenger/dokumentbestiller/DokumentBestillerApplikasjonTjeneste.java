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
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;

@ApplicationScoped
public class DokumentBestillerApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;
    private FormidlingRestKlient formidlingRestKlient;

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
                                                FormidlingRestKlient formidlingRestKlient,
                                                DokumentKafkaBestiller dokumentKafkaBestiller,
                                                Unleash unleash) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentKafkaBestiller = dokumentKafkaBestiller;
        this.formidlingRestKlient = formidlingRestKlient;
        this.unleash = unleash;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        final Behandlingsresultat behandlingsresultat = behandlingVedtak.getBehandlingsresultat();

        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }

        DokumentMalType dokumentMal = velgDokumentMalForVedtak(behandlingsresultat, behandlingVedtak, klageRepository, ankeRepository,unleash);
        dokumentKafkaBestiller.bestillBrev(behandlingsresultat.getBehandling(), dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        bestillDokument(bestillBrevDto, aktør, false);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør, boolean manueltBrev) {
        if (manueltBrev) {
            final Behandling behandling = behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId());
            DokumentbestillingDto dokumentbestillingDto = mapDokumentbestillingDto(bestillBrevDto, behandling);
            dokumentbestillingDto.setHistorikkAktør(aktør.getKode());

            brevHistorikkinnslag.opprettHistorikkinnslagForManueltBestiltBrev(aktør, behandling, bestillBrevDto.getBrevmalkode());

            formidlingRestKlient.bestillDokument(dokumentbestillingDto);
        } else {
            dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        }
    }

    private DokumentbestillingDto mapDokumentbestillingDto(BestillBrevDto bestillBrevDto, Behandling behandling) {
        DokumentbestillingDto dokumentbestillingDto = new DokumentbestillingDto();
        dokumentbestillingDto.setBehandlingUuid(behandling.getUuid());
        dokumentbestillingDto.setDokumentMal(bestillBrevDto.getBrevmalkode());
        dokumentbestillingDto.setFritekst(bestillBrevDto.getFritekst());
        dokumentbestillingDto.setArsakskode(bestillBrevDto.getÅrsakskode());
        dokumentbestillingDto.setYtelseType(mapFagsakYtelseType(behandling.getFagsakYtelseType().getKode()));
        return dokumentbestillingDto;
    }

    private FagsakYtelseType mapFagsakYtelseType(String ytelseTypeKode) {
        if (FagsakYtelseType.ENGANGSTØNAD.getKode().equals(ytelseTypeKode)) {
            return FagsakYtelseType.ENGANGSTØNAD;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.getKode().equals(ytelseTypeKode)) {
            return FagsakYtelseType.SVANGERSKAPSPENGER;
        } else {
            return FagsakYtelseType.FORELDREPENGER;
        }
    }
}
