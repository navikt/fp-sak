package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;


// Klasse som utleder om en sak skal sette på vent fordi den kan bli påvirket
// av regelendring på 8-41 og behandles før regelendring er fattet

import java.time.LocalDate;
import java.util.List;

import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;

public class SkalVentePåRegelendring {
    private static final LocalDate DATO_FOR_REGELENDRING = LocalDate.of(2023,1,1);

    private SkalVentePåRegelendring() {
        // Skjuler default konstruktør
    }

    public static boolean kanPåvirkesAvRegelendring(LocalDate stpOpptjening,
                                                    OpptjeningAktiviteterDto opptjeningAktiviteter) {
        if (stpOpptjening == null || opptjeningAktiviteter == null || opptjeningAktiviteter.getOpptjeningPerioder().isEmpty()) {
            return false;
        }
        var opptjeningPerioder = opptjeningAktiviteter.getOpptjeningPerioder();
        var stpBG = utledTroligStpBeregning(stpOpptjening, opptjeningPerioder);
        if (stpBG.isBefore(DATO_FOR_REGELENDRING)) {
            return false;
        }
        var beregningstidspunkt = stpBG.minusDays(1);
        var erSN = harAktivitetPåDato(beregningstidspunkt, opptjeningPerioder, OpptjeningAktivitetType.NÆRING);
        var erAT = harAktivitetPåDato(beregningstidspunkt, opptjeningPerioder, OpptjeningAktivitetType.ARBEID);
        var erFL = harAktivitetPåDato(beregningstidspunkt, opptjeningPerioder, OpptjeningAktivitetType.FRILANS);
        return erSN && (erAT || erFL);
    }

    private static boolean harAktivitetPåDato(LocalDate beregningstidspunkt, List<OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningPerioder, OpptjeningAktivitetType aktivitetType) {
            return opptjeningPerioder.stream()
                .anyMatch(opp -> opp.getPeriode().inkluderer(beregningstidspunkt) && opp.getOpptjeningAktivitetType().equals(aktivitetType));
    }

    private static LocalDate utledTroligStpBeregning(LocalDate stpOpptjening, List<OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningAktiviteter) {
        var sistePeriode = opptjeningAktiviteter.stream()
            .min(SkalVentePåRegelendring::slutterEtter)
            .orElseThrow();
        if (sistePeriode.getPeriode().inkluderer(stpOpptjening)) {
            return stpOpptjening;
        }
        return sistePeriode.getPeriode().getTomDato();
    }

    private static int slutterEtter(OpptjeningAktiviteterDto.OpptjeningPeriodeDto p1, OpptjeningAktiviteterDto.OpptjeningPeriodeDto p2) {
        return p1.getPeriode().getTomDato().isAfter(p2.getPeriode().getTomDato()) ? -1 : 1;
    }
}
