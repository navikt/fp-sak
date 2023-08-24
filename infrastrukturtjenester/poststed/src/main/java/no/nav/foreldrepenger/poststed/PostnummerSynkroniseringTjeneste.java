package no.nav.foreldrepenger.poststed;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;


@ApplicationScoped
public class PostnummerSynkroniseringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PostnummerSynkroniseringTjeneste.class);
    private static final String KODEVERK_POSTNUMMER = "Postnummer";

    private PoststedKodeverkRepository poststedKodeverkRepository;
    private KodeverkTjeneste kodeverkTjeneste;

    PostnummerSynkroniseringTjeneste() {
        // for CDI proxy
    }

    @Inject
    PostnummerSynkroniseringTjeneste(PoststedKodeverkRepository poststedKodeverkRepository,
                                     KodeverkTjeneste kodeverkTjeneste) {
        this.poststedKodeverkRepository = poststedKodeverkRepository;
        this.kodeverkTjeneste = kodeverkTjeneste;
    }

    public void synkroniserPostnummer() {
        LOG.info("Synkroniserer Postnummer");

        var betydninger = kodeverkTjeneste.hentKodeverkBetydninger(KODEVERK_POSTNUMMER);
        var eksisterendeMap = poststedKodeverkRepository.hentAllePostnummer().stream()
            .collect(Collectors.toMap(Poststed::getPoststednummer, p -> p));
        betydninger.forEach((k, v) -> {
            if (eksisterendeMap.get(k) == null) {
                LOG.info("Nytt Postnummer {} med innhold {}", k, v);
                var nytt = new Poststed(k, v.term(), v.gyldigFra(), v.gyldigTil());
                poststedKodeverkRepository.lagrePostnummer(nytt);
            } else if (!Objects.equals(v.term(), eksisterendeMap.get(k).getPoststednavn())) {
                LOG.info("Endret Postnummer {} med innhold {}", k, v);
                poststedKodeverkRepository.oppdaterPostnummer(eksisterendeMap.get(k), v.term(), v.gyldigFra(), v.gyldigTil());
            }
        });
    }
}
