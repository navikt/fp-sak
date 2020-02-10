package no.nav.foreldrepenger.domene.risikoklassifisering.task;

import static no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask.TASKTYPE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config.RisikoklassifiseringKafkaProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RisikoklassifiseringUtførTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(RisikoklassifiseringUtførTask.class);

    public static final String TASKTYPE = "risiko.klassifisering";

    public static final String KONSUMENT_ID = "konsumentId";

    public static final String RISIKOKLASSIFISERING_JSON = "risikoklassifisering.request.json";

    private RisikoklassifiseringKafkaProducer kafkaProducer;

    RisikoklassifiseringUtførTask() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringUtførTask(RisikoklassifiseringKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    private void prosesser(ProsessTaskData prosessTaskData) {
        try {
            String eventJson = prosessTaskData.getPropertyValue(RISIKOKLASSIFISERING_JSON);
            String konsumentId = prosessTaskData.getPropertyValue(KONSUMENT_ID);
            kafkaProducer.publiserEvent(konsumentId, eventJson);
            log.info("Publiser risikoklassifisering på kafka slik at fprisk kan klassifisere behandlingen. konsumentId :{} BehandlingsId: {}",
                konsumentId, prosessTaskData.getBehandlingId());
        }catch (Exception e){
            log.warn("Feil med publisering av meldingen til kafka. Feilen er ignorert og vil ikke påvirke behandlingsprosessen",e);
        }
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
