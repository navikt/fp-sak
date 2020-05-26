package no.nav.foreldrepenger.økonomi.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public class OppdragskontrollPostConditionCheck {

    public static void valider(Oppdragskontroll oppdragskontroll) {
        for (Oppdrag110 oppdrag110 : oppdragskontroll.getOppdrag110Liste()) {
            valider(oppdrag110);
        }
    }

    private static void valider(Oppdrag110 oppdrag110) {
        if (oppdrag110.getOppdragslinje150Liste().isEmpty()) {
            throw Feilene.FACTORY.tomOppdrag110(oppdrag110.getKodeFagomrade(), oppdrag110.getFagsystemId()).toException();
        }
    }


    interface Feilene extends DeklarerteFeil {

        Feilene FACTORY = FeilFactory.create(Feilene.class);

        @TekniskFeil(feilkode = "FP-611541", feilmelding = "PostCondition feilet for generert oppdrag. Fagområde=%s fagsystemid=%s. Oppdrag110 inneholder ingen oppdragslinjer. Det er ikke forventet.", logLevel = LogLevel.WARN)
        Feil tomOppdrag110(String kodeFagomrade, long fagsystemId);

    }
}
