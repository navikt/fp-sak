package no.nav.foreldrepenger.historikk.kafka;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.historikk.kafka.json.SerialiseringUtil;
import no.nav.historikk.v1.HistorikkInnslagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ProsessTask(LagreHistorikkTask.TASKTYPE)
public class LagreHistorikkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LagreHistorikkTask.class);


    public static final String TASKTYPE = "historikk.kafka.opprettHistorikkinnslag";

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
        String payload = prosessTaskData.getPayloadAsString();
        HistorikkInnslagV1 jsonHistorikk = SerialiseringUtil.deserialiser(payload, HistorikkInnslagV1.class);
        opprettOgLagreHistorikkInnslag(jsonHistorikk);
    }

    private void opprettOgLagreHistorikkInnslag(HistorikkInnslagV1 jsonHistorikk) {
        Historikkinnslag nyttHistorikkInnslag = historikkFraDtoMapper.opprettHistorikkInnslag(jsonHistorikk);
        if (historikkRepository.finnesUuidAllerede(nyttHistorikkInnslag.getUuid())) {
            LOG.info("Oppdaget duplikat historikkinnslag: {}, lagrer ikke.", nyttHistorikkInnslag.getUuid());
            return;
        }
        historikkRepository.lagre(nyttHistorikkInnslag);
    }


}
