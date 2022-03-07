package no.nav.foreldrepenger.historikk.kafka;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.historikk.v1.HistorikkInnslagDokumentLink;
import no.nav.historikk.v1.HistorikkInnslagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ProsessTask("historikk.kafka.opprettHistorikkinnslag")
public class LagreHistorikkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LagreHistorikkTask.class);

    private HistorikkRepository historikkRepository;
    private HistorikkFraDtoMapper historikkFraDtoMapper;
    private BehandlingDokumentRepository behandlingDokumentRepository;

    public LagreHistorikkTask() {
    }

    @Inject
    public LagreHistorikkTask(HistorikkRepository historikkRepository,
                              HistorikkFraDtoMapper historikkFraDtoMapper,
                              BehandlingDokumentRepository behandlingDokumentRepository) {
        this.historikkRepository = historikkRepository;
        this.historikkFraDtoMapper = historikkFraDtoMapper;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var payload = prosessTaskData.getPayloadAsString();
        var jsonHistorikk = StandardJsonConfig.fromJson(payload, HistorikkInnslagV1.class);
        opprettOgLagreHistorikkInnslag(jsonHistorikk);
        oppdaterDokumentBestillingMedJournalpostId(jsonHistorikk.getHistorikkUuid(), jsonHistorikk.getDokumentLinker());
    }

    private void opprettOgLagreHistorikkInnslag(HistorikkInnslagV1 jsonHistorikk) {
        var nyttHistorikkInnslag = historikkFraDtoMapper.opprettHistorikkInnslag(jsonHistorikk);
        if (historikkRepository.finnesUuidAllerede(nyttHistorikkInnslag.getUuid())) {
            LOG.info("Oppdaget duplikat historikkinnslag: {}, lagrer ikke.", nyttHistorikkInnslag.getUuid());
            return;
        }
        historikkRepository.lagre(nyttHistorikkInnslag);
    }

    private void oppdaterDokumentBestillingMedJournalpostId(UUID historikkUuid, List<HistorikkInnslagDokumentLink> dokumentLinker) {
        var dokumentBestiling = behandlingDokumentRepository.hentHvisEksisterer(historikkUuid);
        if (dokumentBestiling.isEmpty()) {
            LOG.info("Fant ikke dokument bestillinger for historikkUuid: {}.", historikkUuid);
        }
        dokumentBestiling.ifPresent(bestilling -> {
            var journalpostId = dokumentLinker.get(0).getJournalpostId().getVerdi();
            LOG.info("JournalpostId: {}.", journalpostId);
            bestilling.setJournalpostId(new JournalpostId(journalpostId));
            behandlingDokumentRepository.lagreOgFlush(bestilling);
        });
    }

}
