package no.nav.foreldrepenger.poststed;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;


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
        LOG.info("Synkroniserer kodeverk: {}", KODEVERK_POSTNUMMER); // NOSONAR

        var kodeverksDato = poststedKodeverkRepository.getPostnummerKodeverksDato();

        var pnrInfo = kodeverkTjeneste.hentGjeldendeKodeverk(KODEVERK_POSTNUMMER).orElseThrow();
        if (pnrInfo.getVersjonDato().isAfter(kodeverksDato)) {
            lagreNyVersjon(pnrInfo);
            LOG.info("Nye Postnummer lagret: versjon {} med dato {}", pnrInfo.getVersjon(), pnrInfo.getVersjonDato());
        } else {
            LOG.info("Har allerede Postnummer: versjon {} med dato {}", pnrInfo.getVersjon(), pnrInfo.getVersjonDato());
        }
    }

    private void lagreNyVersjon(KodeverkInfo pnrInfo) {
        var eksisterendeMap = poststedKodeverkRepository.hentAllePostnummer().stream()
                .collect(Collectors.toMap(Poststed::getPoststednummer, p -> p));
        var masterKoderMap = kodeverkTjeneste.hentKodeverk(KODEVERK_POSTNUMMER, pnrInfo.getVersjon());
        masterKoderMap.forEach((key, value) -> synkroniserNyEllerEksisterendeKode(eksisterendeMap, value));
        poststedKodeverkRepository.setPostnummerKodeverksDato(pnrInfo.getVersjon(), pnrInfo.getVersjonDato());
    }

    private void synkroniserNyEllerEksisterendeKode(Map<String, Poststed> eksisterendeKoderMap, KodeverkKode masterKode) {
        if (eksisterendeKoderMap.containsKey(masterKode.getKode())) {
            synkroniserEksisterendeKode(masterKode, eksisterendeKoderMap.get(masterKode.getKode()));
        } else {
            synkroniserNyKode(masterKode);
        }
    }

    private void synkroniserNyKode(KodeverkKode kodeverkKode) {
        var nytt = new Poststed(kodeverkKode.getKode(), kodeverkKode.getNavn(), kodeverkKode.getGyldigFom(), kodeverkKode.getGyldigTom());
        poststedKodeverkRepository.lagrePostnummer(nytt);
    }

    private void synkroniserEksisterendeKode(KodeverkKode kodeverkKode, Poststed postnummer) {
        if (!erLike(kodeverkKode, postnummer)) {
            postnummer.setPoststednavn(kodeverkKode.getNavn());
            postnummer.setGyldigFom(kodeverkKode.getGyldigFom());
            postnummer.setGyldigTom(kodeverkKode.getGyldigTom());
            poststedKodeverkRepository.lagrePostnummer(postnummer);
        }
    }

    private static boolean erLike(KodeverkKode kodeverkKode, Poststed postnummer) {
        return Objects.equals(kodeverkKode.getGyldigFom(), postnummer.getGyldigFom())
                && Objects.equals(kodeverkKode.getGyldigTom(), postnummer.getGyldigTom())
                && Objects.equals(kodeverkKode.getNavn(), postnummer.getPoststednavn());
    }

}
