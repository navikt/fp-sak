package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
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
        Optional<Oppdragskontroll> oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId);
        // hvis oppdragskontroll ble tidgligere lagret av en annen prosess
        // legge oppdrag på kø
        oppdragskontroll.ifPresent(ok -> {
            ØkonomioppdragMapper mapper = new ØkonomioppdragMapper();
            List<String> oppdragXMLListe = mapper.generateOppdragXML(ok);
            //Legge oppdragXML i kø til Økonomiløsningen
            for (String oppdragXML : oppdragXMLListe) {
                økonomioppdragJmsProducer.sendØkonomiOppdrag(oppdragXML);
            }
        });
    }
}
