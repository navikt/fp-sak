package no.nav.foreldrepenger.domene.risikoklassifisering.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config.RisikoklassifiseringMeldingProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("risiko.klassifisering")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RisikoklassifiseringUtførTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RisikoklassifiseringUtførTask.class);

    public static final String KONSUMENT_ID = "konsumentId";

    private RisikoklassifiseringMeldingProducer kafkaProducer;

    RisikoklassifiseringUtførTask() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringUtførTask(RisikoklassifiseringMeldingProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    private void prosesser(ProsessTaskData prosessTaskData) {
        try {
            var eventJson = prosessTaskData.getPayloadAsString();
            var konsumentId = prosessTaskData.getPropertyValue(KONSUMENT_ID);
            kafkaProducer.sendJsonMedNøkkel(konsumentId, eventJson);
            LOG.info("Publiser risikoklassifisering på kafka slik at fprisk kan klassifisere behandlingen. konsumentId :{} BehandlingsId: {}",
                konsumentId, prosessTaskData.getBehandlingId());
        }catch (Exception e){
            LOG.warn("Feil med publisering av meldingen til kafka. Feilen er ignorert og vil ikke påvirke behandlingsprosessen",e);
        }
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
