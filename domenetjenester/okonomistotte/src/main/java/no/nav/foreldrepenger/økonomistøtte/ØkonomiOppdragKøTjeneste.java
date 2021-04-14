package no.nav.foreldrepenger.økonomistøtte;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;

@ApplicationScoped
public class ØkonomiOppdragKøTjeneste {

    private ØkonomioppdragRepository økonomioppdragRepository;
    private ØkonomioppdragJmsProducer økonomioppdragJmsProducer;

    ØkonomiOppdragKøTjeneste() {
        // CDI krav
    }

    @Inject
    public ØkonomiOppdragKøTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                                    ØkonomioppdragJmsProducer økonomioppdragJmsProducer) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.økonomioppdragJmsProducer = økonomioppdragJmsProducer;
    }

    /**
     * Skal legge økonomi oppdrag på kø
     *
     * @param behandlingId
     */
    public void leggOppdragPåKø(Long behandlingId) {
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId);
        // hvis oppdragskontroll ble tidgligere lagret av en annen prosess
        // legge oppdrag på kø
        oppdragskontroll.ifPresent(ok -> {
            var mapper = new ØkonomioppdragMapper();
            var oppdragXMLListe = mapper.generateOppdragXML(ok);
            //Legge oppdragXML i kø til Økonomiløsningen
            for (var oppdragXML : oppdragXMLListe) {
                økonomioppdragJmsProducer.sendØkonomiOppdrag(oppdragXML);
            }
        });
    }
}
