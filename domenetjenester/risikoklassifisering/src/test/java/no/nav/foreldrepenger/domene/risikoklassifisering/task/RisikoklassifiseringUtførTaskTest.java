package no.nav.foreldrepenger.domene.risikoklassifisering.task;


import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config.RisikoklassifiseringMeldingProducer;
import no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class RisikoklassifiseringUtførTaskTest {

    private static final Long BEHANDLING_ID = 123342L;

    private static final String KONSUMENT_ID = "konsumentId";


    @Mock
    private RisikoklassifiseringMeldingProducer kafkaProducer;

    private RisikoklassifiseringUtførTask risikoklassifiseringUtførTask;

    @BeforeEach
    public void init(){
        risikoklassifiseringUtførTask = new RisikoklassifiseringUtførTask(kafkaProducer);
    }

    @Test
    public void skal_produsere_melding_til_kafka(){
        var prosessTaskData = ProsessTaskData.forProsessTask(RisikoklassifiseringUtførTask.class);
        prosessTaskData.setPayload("json");

        var konsumentId = UUID.randomUUID().toString();
        prosessTaskData.setProperty(KONSUMENT_ID, konsumentId);

        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_ID, String.valueOf(BEHANDLING_ID));
        risikoklassifiseringUtførTask.doTask(prosessTaskData);
        verify(kafkaProducer).sendJsonMedNøkkel(konsumentId, "json");
    }

}
