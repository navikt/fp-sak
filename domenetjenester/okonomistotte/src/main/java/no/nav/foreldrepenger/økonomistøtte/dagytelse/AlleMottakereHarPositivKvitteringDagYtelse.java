package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTjeneste;

public class AlleMottakereHarPositivKvitteringDagYtelse {

    private KodeFagområdeTjeneste kodeFagområdeTjeneste;

    private AlleMottakereHarPositivKvitteringDagYtelse(KodeFagområdeTjeneste kodeFagområdeTjeneste) {
        this.kodeFagområdeTjeneste = kodeFagområdeTjeneste;
    }

    public static AlleMottakereHarPositivKvitteringDagYtelse forForeldrepenger() {
        KodeFagområdeTjeneste tjenesteFP = KodeFagområdeTjeneste.forForeldrepenger();
        return new AlleMottakereHarPositivKvitteringDagYtelse(tjenesteFP);
    }

    public static AlleMottakereHarPositivKvitteringDagYtelse forSvangerskapspenger() {
        KodeFagområdeTjeneste tjenesteSVP = KodeFagområdeTjeneste.forSvangerskapspenger();
        return new AlleMottakereHarPositivKvitteringDagYtelse(tjenesteSVP);
    }

    public boolean vurder(Oppdragskontroll oppdragskontroll) {
        if (oppdragskontroll.getOppdrag110Liste().isEmpty()) {
            return false;
        }
        return harPositivKvitteringForBrukerHvisNødvendig(oppdragskontroll)
            && alleArbeidsgivereHarEnPositivKvittering(oppdragskontroll);
    }

    private boolean harPositivKvitteringForBrukerHvisNødvendig(Oppdragskontroll oppdragskontroll) {
        boolean finnesBruker = oppdragskontroll.getOppdrag110Liste().stream()
            .anyMatch(oppdrag110 -> kodeFagområdeTjeneste.gjelderBruker(oppdrag110));
        if (!finnesBruker) {
            return true;
        }
        return harPositivKvitteringForBruker(oppdragskontroll);
    }

    private boolean harPositivKvitteringForBruker(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream()
            .filter(oppdrag110 -> kodeFagområdeTjeneste.gjelderBruker(oppdrag110))
            .anyMatch(OppdragKvitteringTjeneste::harPositivKvittering);
    }

    private boolean alleArbeidsgivereHarEnPositivKvittering(Oppdragskontroll oppdragskontroll) {
        Set<String> alleArbeidsgivere = hentRefunderesId(oppdragskontroll.getOppdrag110Liste().stream()
            .filter(oppdrag110 -> !kodeFagområdeTjeneste.gjelderBruker(oppdrag110)));
        Set<String> arbeidsgivereMedPositivKvittering = hentRefunderesId(oppdragskontroll.getOppdrag110Liste().stream()
            .filter(oppdrag110 -> !kodeFagområdeTjeneste.gjelderBruker(oppdrag110))
            .filter(OppdragKvitteringTjeneste::harPositivKvittering));

        return alleArbeidsgivere.equals(arbeidsgivereMedPositivKvittering);
    }

    private Set<String> hentRefunderesId(Stream<Oppdrag110> stream) {
        return stream
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(List::stream)
            .map(Oppdragslinje150::getRefusjonsinfo156)
            .map(Refusjonsinfo156::getRefunderesId)
            .collect(Collectors.toSet());
    }
}
