package no.nav.foreldrepenger.historikk.kafka;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.historikk.v1.HistorikkInnslagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ProsessTask("historikk.kafka.opprettHistorikkinnslag")
public class LagreHistorikkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LagreHistorikkTask.class);

    private HistorikkRepository historikkRepository;
    private HistorikkFraDtoMapper historikkFraDtoMapper;

    public LagreHistorikkTask() {
    }

    @Inject
    public LagreHistorikkTask(HistorikkRepository historikkRepository,
            HistorikkFraDtoMapper historikkFraDtoMapper) {
        this.historikkRepository = historikkRepository;
        this.historikkFraDtoMapper = historikkFraDtoMapper;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var payload = prosessTaskData.getPayloadAsString();
        var jsonHistorikk = StandardJsonConfig.fromJson(payload, HistorikkInnslagV1.class);
        opprettOgLagreHistorikkInnslag(jsonHistorikk);
    }

    private void opprettOgLagreHistorikkInnslag(HistorikkInnslagV1 jsonHistorikk) {
        var nyttHistorikkInnslag = historikkFraDtoMapper.opprettHistorikkInnslag(jsonHistorikk);
        if (historikkRepository.finnesUuidAllerede(nyttHistorikkInnslag.getUuid())) {
            LOG.info("Oppdaget duplikat historikkinnslag: {}, lagrer ikke.", nyttHistorikkInnslag.getUuid());
            return;
        }
        historikkRepository.lagre(nyttHistorikkInnslag);
    }

}
