package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

class FellesStartpunktUtlederLogger {

    private static final Logger LOG = LoggerFactory.getLogger(FellesStartpunktUtlederLogger.class);

    FellesStartpunktUtlederLogger() {
        // For CDI
    }

    static void loggEndringSomFørteTilStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, Object id1, Object id2) {
        skrivLoggMedStartpunkt(klasseNavn, startpunkt, endring, id1.toString(), id2.toString());
    }

    static void loggAndreEndringSomIkkeFørteTilStartpunkt(String klasseNavn, String endring, Object id1, Object id2) {
        skrivLoggUtenStartpunkt(klasseNavn, endring, id1.toString(), id2.toString());
    }

    static void loggEndringSomFørteTilStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, UUID id1, UUID id2) {
        skrivLoggMedStartpunkt(klasseNavn, startpunkt, endring, id1.toString(), id2.toString());
    }

    static void loggAndreEndringSomIkkeFørteTilStartpunkt(String klasseNavn, String endring, UUID id1, UUID id2) {
        skrivLoggUtenStartpunkt(klasseNavn, endring, id1.toString(), id2.toString());
    }

    static void skrivLoggMedStartpunkt(String klasseNavn, StartpunktType startpunkt, String endring, String id1, String id2) {
        LOG.info("{}: Setter startpunkt til {}. Og har endring i {}. GrunnlagId1: {}, grunnlagId2: {}", klasseNavn, startpunkt.getKode(), endring, id1, id2);
    }

    static void skrivLoggUtenStartpunkt(String klasseNavn, String endring, String id1, String id2) {
        LOG.info("{}: Setter ikke startpunkt for endring i {}. GrunnlagId1: {}, grunnlagId2: {}", klasseNavn, endring, id1, id2);
    }

    static void skrivLoggStartpunktIM(String klasseNavn, String endring, Long behandlingId, String kanalreferanse) {
        LOG.info("{}: Inntektsmelding endring {}. Behandling: {}, kanalreferanse: {}", klasseNavn, endring, behandlingId, kanalreferanse);
    }
}
