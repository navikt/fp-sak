package no.nav.foreldrepenger.kodeverk;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DefaultKodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverk;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkSynkroniseringRepository;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.log.util.LoggerUtils;

/**
 * Tjenestelogikken for batch tjeneste for å automatisk oppdatere kodeverk.
 * Tilgjengelighet begrenses til package.
 */

@Dependent
class KodeverkSynkronisering {

    private static final Logger LOGGER = LoggerFactory.getLogger(KodeverkSynkronisering.class);

    private KodeverkSynkroniseringRepository kodeverkSynkroniseringRepository;
    private KodeverkTjeneste kodeverkTjeneste;

    KodeverkSynkronisering() {
        // for CDI proxy
    }

    @Inject
    KodeverkSynkronisering(KodeverkSynkroniseringRepository kodeverkSynkroniseringRepository,
            KodeverkTjeneste kodeverkTjeneste) {
        this.kodeverkSynkroniseringRepository = kodeverkSynkroniseringRepository;
        this.kodeverkTjeneste = kodeverkTjeneste;
    }

    void synkroniserAlleKodeverk() {
        int antallSynkronisert = 0;
        List<Kodeverk> kodeverkList = kodeverkSynkroniseringRepository.hentKodeverkForSynkronisering();

        List<KodeverkInfo> kodeverkInfoList = kodeverkTjeneste.hentGjeldendeKodeverkListe();

        for (Kodeverk kodeverk : kodeverkList) {
            KodeverkInfo kodeverkInfo = kodeverkInfoList.stream()
                    .filter(ki -> ki.getNavn().equals(kodeverk.getKodeverkEierNavn()))
                    .findFirst().orElse(null);
            if (kodeverkInfo != null && !kodeverkInfo.getVersjon().equals(kodeverk.getKodeverkEierVersjon())) {
                LOGGER.info("Ny versjon av kodeverk: {}",
                        LoggerUtils.removeLineBreaks(
                                kodeverk.getKode() + " eier " + kodeverkInfo.getVersjon() + " lokal " + kodeverk.getKodeverkEierVersjon())); // NOSONAR
                antallSynkronisert++;
                kodeverkSynkroniseringRepository.oppdaterEksisterendeKodeverk(kodeverk.getKode(), kodeverkInfo.getVersjon(), kodeverkInfo.getUri());
                synkroniserKodeverk(kodeverk, kodeverkInfo.getVersjon());

            }
        }
        if (antallSynkronisert == 0) {
            LOGGER.info("Ingen nye versjoner av kodeverk"); // NOSONAR
        } else {
            kodeverkSynkroniseringRepository.lagre();
            LOGGER.info("Nye versjoner av kodeverk lagret"); // NOSONAR
        }
    }

    private void synkroniserKodeverk(Kodeverk kodeverk, String versjon) {
        LOGGER.info("Synkroniserer kodeverk: {}", LoggerUtils.removeLineBreaks(kodeverk.getKode())); // NOSONAR

        List<DefaultKodeverdi> eksisterendeKoder = kodeverkSynkroniseringRepository.hentKodeliste(kodeverk.getKode());
        Map<String, DefaultKodeverdi> eksisterendeKoderMap = eksisterendeKoder.stream()
                .collect(Collectors.toMap(this::finnOffisiellKode, kodeliste -> kodeliste));

        Map<String, KodeverkKode> masterKoderMap;
        try {
            masterKoderMap = kodeverkTjeneste.hentKodeverk(kodeverk.getKodeverkEierNavn(), versjon, null);
        } catch (IntegrasjonException ex) {
            throw KodeverkFeil.FACTORY.synkronoseringAvKodeverkFeilet(kodeverk.getKode(), ex).toException();
        }

        masterKoderMap.forEach((key, value) -> synkroniserNyEllerEksisterendeKode(kodeverk, eksisterendeKoderMap, value));
        behandleEksisterendeKoderIkkeMottatt(eksisterendeKoderMap, masterKoderMap);
    }

    /**
     * Koder slettes aldri. Dersom en eksisterende kode ikke mottas logges dette som
     * en warning.
     */
    private static void behandleEksisterendeKoderIkkeMottatt(Map<String, DefaultKodeverdi> eksisterendeKoderMap,
            Map<String, KodeverkKode> masterKoderMap) {
        Set<String> eksisterendeKoderIkkeMottatt = eksisterendeKoderMap.keySet().stream()
                .filter(eksisterendeKode -> !masterKoderMap.containsKey(eksisterendeKode))
                .collect(Collectors.toSet());
        eksisterendeKoderIkkeMottatt.forEach(
                ikkeMottatt -> KodeverkFeil.FACTORY.eksisterendeKodeIkkeMottatt(eksisterendeKoderMap.get(ikkeMottatt).getKodeverk(), ikkeMottatt)
                        .log(LOGGER));
    }

    private void synkroniserNyEllerEksisterendeKode(Kodeverk kodeverk, Map<String, DefaultKodeverdi> eksisterendeKoderMap,
            KodeverkKode masterKode) {
        if (kodeverk.getSynkEksisterendeKoderFraKodeverkEier() && eksisterendeKoderMap.containsKey(masterKode.getKode())) {
            synkroniserEksisterendeKode(masterKode, eksisterendeKoderMap.get(masterKode.getKode()));
        }
        if (kodeverk.getSynkNyeKoderFraKodeverEier() && !eksisterendeKoderMap.containsKey(masterKode.getKode())) {
            synkroniserNyKode(kodeverk.getKode(), masterKode);
        }
    }

    private String finnOffisiellKode(DefaultKodeverdi kodeliste) { // NOSONAR
        return kodeliste.getOffisiellKode() != null ? kodeliste.getOffisiellKode() : kodeliste.getKode();
    }

    private void synkroniserNyKode(String kodeverk, KodeverkKode kodeverkKode) {
        LOGGER.info("Ny kode: {} {}", // NOSONAR
                LoggerUtils.removeLineBreaks(kodeverk), // NOSONAR
                LoggerUtils.removeLineBreaks(kodeverkKode.getKode())); // NOSONAR

        kodeverkSynkroniseringRepository.opprettNyKode(kodeverk, kodeverkKode.getKode(),
                kodeverkKode.getKode(), kodeverkKode.getNavn(), kodeverkKode.getGyldigFom(), kodeverkKode.getGyldigTom());
    }

    private void synkroniserEksisterendeKode(KodeverkKode kodeverkKode, DefaultKodeverdi kodeliste) {
        if (!erLike(kodeverkKode, kodeliste)) {
            LOGGER.info("Oppdaterer kode: {} {}", // NOSONAR
                    LoggerUtils.removeLineBreaks(kodeliste.getKodeverk()), // NOSONAR
                    LoggerUtils.removeLineBreaks(kodeliste.getKode())); // NOSONAR

            kodeverkSynkroniseringRepository.oppdaterEksisterendeKode(kodeliste.getKodeverk(), kodeliste.getKode(),
                    kodeverkKode.getKode(), kodeverkKode.getNavn(), kodeverkKode.getGyldigFom(), kodeverkKode.getGyldigTom());
        }
    }

    private static boolean erLike(KodeverkKode kodeverkKode, DefaultKodeverdi kodeliste) {
        return kodeverkKode.getGyldigFom().compareTo(kodeliste.getGyldigFom()) == 0
                && kodeverkKode.getGyldigTom().compareTo(kodeliste.getGyldigTom()) == 0
                && kodeverkKode.getNavn().compareTo(kodeliste.getNavn()) == 0;
    }

}
