package no.nav.foreldrepenger.behandling.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.impl.kafka.behandlingskontroll.AksjonspunktKafkaProducer;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(PubliserEventTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class PubliserEventTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "oppgavebehandling.PubliserEvent";
    public static final String PROPERTY_EVENT = "event";
    public static final String PROPERTY_KEY = "topicKey";

    private static final Logger LOG = LoggerFactory.getLogger(PubliserEventTask.class);

    private AksjonspunktKafkaProducer kafkaProducer;

    PubliserEventTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserEventTask(AksjonspunktKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    protected void prosesser(ProsessTaskData prosessTaskData) {
        var eventJson = prosessTaskData.getPropertyValue(PROPERTY_EVENT);
        var key = prosessTaskData.getPropertyValue(PROPERTY_KEY);
        kafkaProducer.sendJsonMedNøkkel(key, eventJson);
        LOG.info("Publiser aksjonspunktevent på kafka slik at f.eks fplos kan fordele oppgaven for videre behandling. BehandlingsId: {}", key);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }
}
