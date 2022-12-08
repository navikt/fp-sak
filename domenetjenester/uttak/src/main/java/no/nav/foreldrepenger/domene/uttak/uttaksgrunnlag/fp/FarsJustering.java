package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.beregnAntallVirkedager;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.justerHelgTilMandag;
import static no.nav.foreldrepenger.regler.uttak.felles.Virkedager.plusVirkedager;
import static no.nav.fpsak.tidsserie.LocalDateInterval.min;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class FarsJustering implements ForelderFødselJustering {

    private final LocalDate termindato; //termindato kan være første fødselsdato ved endring av fødselsdato
    private final LocalDate fødselsdato;
    private final boolean ønskerJustertVedFødsel;

    FarsJustering(LocalDate termindato, LocalDate fødselsdato, boolean ønskerJustertVedFødsel) {
        this.termindato = justerHelgTilMandag(termindato);
        this.fødselsdato = justerHelgTilMandag(fødselsdato);
        this.ønskerJustertVedFødsel = ønskerJustertVedFødsel;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        guard(termindato, fødselsdato);
        if (!skalJustere(oppgittePerioder)) {
            return oppgittePerioder;
        }
        var slåttSammen = slåSammenLikePerioder(oppgittePerioder);
        var justerFørstePeriode = justerFødselEtterTermin(oppgittePerioder.get(0), oppgittePerioder);
        if (justerFørstePeriode.isEmpty()) {
            return oppgittePerioder.subList(1, oppgittePerioder.size());
        }
        slåttSammen.set(0, justerFørstePeriode.get());
        return slåttSammen;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        guard(fødselsdato, termindato);
        if (!skalJustere(oppgittePerioder)) {
            return oppgittePerioder;
        }
        var slåttSammen = slåSammenLikePerioder(oppgittePerioder);
        var justerFørstePeriode = justerFødselFørTermin(oppgittePerioder.get(0));
        slåttSammen.set(0, justerFørstePeriode);
        return slåttSammen;
    }

    private static void guard(LocalDate førsteDato, LocalDate sisteDato) {
        if (førsteDato.isAfter(sisteDato)) {
            throw new IllegalStateException(førsteDato + " kan ikke ligge etter " + sisteDato);
        }
    }

    private boolean skalJustere(List<OppgittPeriodeEntitet> oppgittePerioder) {
        if (ønskerJustertVedFødsel && !oppgittePerioder.isEmpty()) {
            var bareEnPeriodeFraTermin = harBareEnPeriodeFedrekvoteRundtFødselFraTermindato(
                oppgittePerioder) && intervallRundt(termindato).isPresent();
            if (bareEnPeriodeFraTermin) {
                return true;
            }
            throw new IllegalStateException("Bruker ønsker justert uttak ved fødsel, men det finnes mer enn bare en periode fedrekvote fra termin");
        }
        return false;
    }

    private OppgittPeriodeEntitet justerFødselFørTermin(OppgittPeriodeEntitet periode) {
        var virkedagerIPeriode = beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var nyTom = plusVirkedager(fødselsdato, virkedagerIPeriode - 1);
        return OppgittPeriodeBuilder.fraEksisterende(periode).medPeriode(fødselsdato, nyTom).build();
    }

    private Optional<OppgittPeriodeEntitet> justerFødselEtterTermin(OppgittPeriodeEntitet periode, List<OppgittPeriodeEntitet> oppgittePerioder) {
        var tomGrense = oppgittePerioder.size() > 1 ? oppgittePerioder.get(1).getFom().minusDays(1) : LocalDate.MAX;
        if (!fødselsdato.isBefore(tomGrense)) {
            return Optional.empty();
        }
        var virkedagerIPeriode = beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var nyTom = min(plusVirkedager(fødselsdato, virkedagerIPeriode - 1), tomGrense);
        return Optional.of(OppgittPeriodeBuilder.fraEksisterende(periode).medPeriode(fødselsdato, nyTom).build());
    }

    private boolean harBareEnPeriodeFedrekvoteRundtFødselFraTermindato(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var førstePeriode = oppgittePerioder.get(0);
        if (!erFedrekvoteRundtTermin(førstePeriode) || !likTermindato(førstePeriode.getFom())) {
            return false;
        }
        return oppgittePerioder.stream().filter(op -> liggerIIntervalletRundtTermin(op)).count() == 1;
    }

    private boolean likTermindato(LocalDate dato) {
        return termindato.isEqual(justerHelgTilMandag(dato));
    }

    private boolean erFedrekvoteRundtTermin(OppgittPeriodeEntitet op) {
        return op.isSamtidigUttak() && (erFedrekvote(op) || erForeldrepenger(op)) && liggerIIntervalletRundtTermin(op);
    }

    private Boolean liggerIIntervalletRundtTermin(OppgittPeriodeEntitet op) {
        var intervallFarRundtTermin = intervallRundt(termindato);
        return intervallFarRundtTermin.map(interval -> interval.contains(new LocalDateInterval(op.getFom(), op.getTom()))).orElse(false);
    }

    private Optional<LocalDateInterval> intervallRundt(LocalDate dato) {
        return TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(false, true, dato, dato);
    }

    private boolean erForeldrepenger(OppgittPeriodeEntitet op) {
        return UttakPeriodeType.FORELDREPENGER.equals(op.getPeriodeType());
    }

    private boolean erFedrekvote(OppgittPeriodeEntitet op) {
        return UttakPeriodeType.FEDREKVOTE.equals(op.getPeriodeType());
    }
}
