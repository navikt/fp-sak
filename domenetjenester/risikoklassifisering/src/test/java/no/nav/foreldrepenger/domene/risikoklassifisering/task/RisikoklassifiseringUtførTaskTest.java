package no.nav.foreldrepenger.domene.risikoklassifisering.task;


import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config.RisikoklassifiseringKafkaProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@RunWith(MockitoJUnitRunner.class)
public class RisikoklassifiseringUtførTaskTest {

    private static final Long BEHANDLING_ID = 123342L;

    private static final String TASKTYPE = "risiko.klassifisering";

    private static final String KONSUMENT_ID = "konsumentId";

    private static final String RISIKOKLASSIFISERING_JSON = "risikoklassifisering.request.json";

    @Mock
    private RisikoklassifiseringKafkaProducer kafkaProducer;

    private RisikoklassifiseringUtførTask risikoklassifiseringUtførTask;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        risikoklassifiseringUtførTask
            = new RisikoklassifiseringUtførTask(kafkaProducer);
    }

    @Test
    public void skal_produsere_melding_til_kafka(){
        ProsessTaskData prosessTaskData = new ProsessTaskData(TASKTYPE);
        prosessTaskData.setProperty(RISIKOKLASSIFISERING_JSON, "json");

        String konsumentId = UUID.randomUUID().toString();
        prosessTaskData.setProperty(KONSUMENT_ID, konsumentId);

        prosessTaskData.setProperty(ProsessTaskData.BEHANDLING_ID, String.valueOf(BEHANDLING_ID));
        risikoklassifiseringUtførTask.doTask(prosessTaskData);
        verify(kafkaProducer).publiserEvent(konsumentId, "json");
    }

}
