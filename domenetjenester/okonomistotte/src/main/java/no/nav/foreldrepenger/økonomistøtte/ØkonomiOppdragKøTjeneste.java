package no.nav.foreldrepenger.økonomistøtte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ØkonomiOppdragKøTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ØkonomiOppdragKøTjeneste.class);

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
            LOG.debug("Sender {} økonomi oppdrag.", oppdragXMLListe.size());
            //Legge oppdragXML i kø til Økonomiløsningen
            for (var oppdragXML : oppdragXMLListe) {
                økonomioppdragJmsProducer.sendØkonomiOppdrag(oppdragXML);
            }
        });
    }
}
