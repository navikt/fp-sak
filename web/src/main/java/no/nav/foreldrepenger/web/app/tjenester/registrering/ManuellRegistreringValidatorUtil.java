package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.LIK_ELLER_ETTER_MOTTATT_DATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.OVERLAPPENDE_PERIODER;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.PAAKREVD_FELT;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.STARTDATO_FØR_SLUTTDATO;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster.TIDLIGERE_DATO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManuellRegistreringValidatorUtil {

    private ManuellRegistreringValidatorUtil() {
        // skal ikke lages instans
    }

    static boolean perioderOverlapper(Periode p1, Periode p2) {
        if (p2.getStart() == null || p2.getSlutt() == null || p1.getStart() == null || p1.getSlutt() == null ) {
            return false;
        }
        p2.begynnerFør(p1);
        var p1BegynnerFørst = p1.begynnerFør(p2);
        var begynnerFørst = p1BegynnerFørst ? p1 : p2;
        var begynnerSist = p1BegynnerFørst ? p2 : p1;
        return begynnerFørst.getSlutt().isAfter(begynnerSist.getStart());
    }

    public static List<String> overlappendePerioder(List<Periode> perioder) {
        List<String> feil = new ArrayList<>();
        for (var i = 0; i < perioder.size(); i++) {
            var periode = perioder.get(i);

            for (var y = i + 1; y < perioder.size(); y++) {
                if (perioderOverlapper(periode, perioder.get(y))) {
                    feil.add(OVERLAPPENDE_PERIODER);
                }
            }
        }
        return feil;
    }

    public static List<String> startdatoFørSluttdato(List<Periode> perioder) {
        return perioder.stream().filter(p -> !p.startFørSlutt()).map(p -> STARTDATO_FØR_SLUTTDATO).collect(Collectors.toList());
    }

    public static List<String> datoIkkeNull(List<Periode> perioder) {
        List<String> feil = new ArrayList<>();
        for (var periode: perioder) {
            if (periode.getStart() == null || periode.getSlutt() == null) {
                feil.add(PAAKREVD_FELT);
            }
        }
        return feil;
    }

    public static List<String> startdatoFørMottatDato(List<Periode> perioder, LocalDate mottattDato) {
        return perioder.stream().filter(p -> p.start.isBefore(mottattDato)).map(p -> LIK_ELLER_ETTER_MOTTATT_DATO).collect(Collectors.toList());
    }

    public static List<String> periodeFørDagensDato(List<Periode> perioder) {
        return perioder.stream().filter(p -> !p.erFørDagensDato()).map(p -> TIDLIGERE_DATO).collect(Collectors.toList());
    }

    public static class Periode {
        private LocalDate start;
        private LocalDate slutt;

        public Periode(LocalDate start, LocalDate slutt) {
            this.start = start;
            this.slutt = slutt;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getSlutt() {
            return slutt;
        }

        boolean begynnerFør(Periode otherPeriode) {
            return start.isBefore(otherPeriode.start);
        }

        boolean startFørSlutt(){
            if (slutt == null || start == null) {
                return true;
            }
            return start.isBefore(slutt) || start.isEqual(slutt);
        }

        boolean erFørDagensDato() {
            var now = LocalDate.now();
            return !(start.isAfter(now) || start.isEqual(now) || slutt.isAfter(now));
        }
    }
}
