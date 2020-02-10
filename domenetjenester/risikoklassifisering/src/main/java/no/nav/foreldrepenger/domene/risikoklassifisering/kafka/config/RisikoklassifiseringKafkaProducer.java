package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RisikoklassifiseringKafkaProducer {

    private RisikoklassifiseringMeldingProducer meldingProducer;

    RisikoklassifiseringKafkaProducer(){
    }

    @Inject
    public RisikoklassifiseringKafkaProducer(RisikoklassifiseringMeldingProducer meldingProducer) {
        this.meldingProducer = meldingProducer;
    }

    public void publiserEvent(String key, String hendelseJson) {
        meldingProducer.sendJsonMedNøkkel(key,hendelseJson);
    }
}
