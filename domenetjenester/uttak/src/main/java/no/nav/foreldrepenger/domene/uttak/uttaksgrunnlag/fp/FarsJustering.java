package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;
import static no.nav.fpsak.tidsserie.LocalDateInterval.min;

class FarsJustering implements ForelderFødselJustering {

    private static final Logger LOG = LoggerFactory.getLogger(FarsJustering.class);

    private final LocalDate termindato; //termindato kan være første fødselsdato ved endring av fødselsdato
    private final LocalDate fødselsdato;
    private final boolean ønskerJustertVedFødsel;

    FarsJustering(LocalDate termindato, LocalDate fødselsdato, boolean ønskerJustertVedFødsel) {
        this.termindato = Virkedager.justerHelgTilMandag(termindato);
        this.fødselsdato = Virkedager.justerHelgTilMandag(fødselsdato);
        this.ønskerJustertVedFødsel = ønskerJustertVedFødsel;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        guard(termindato, fødselsdato);
        var slåttSammen = slåSammen(oppgittePerioder);
        if (!skalJustere(slåttSammen)) {
            return oppgittePerioder;
        }
        var justerFørstePeriode = justerFødselEtterTermin(slåttSammen.get(0), slåttSammen);
        if (justerFørstePeriode.isEmpty()) {
            return slåttSammen.subList(1, slåttSammen.size());
        }
        slåttSammen.set(0, justerFørstePeriode.get());
        return slåttSammen;
    }

    @Override
    public List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder) {
        guard(fødselsdato, termindato);
        var slåttSammen = slåSammen(oppgittePerioder);
        if (!skalJustere(slåttSammen)) {
            return oppgittePerioder;
        }
        var justerFørstePeriode = justerFødselFørTermin(slåttSammen.get(0));
        slåttSammen.set(0, justerFørstePeriode);
        return slåttSammen;
    }

    private static List<OppgittPeriodeEntitet> slåSammen(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return slåSammenLikePerioder(oppgittePerioder, true);
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
            LOG.warn("Kan ikke justere fars uttak rundt fødsel. Selv om bruker har søkt om justering!. Mulig feil eller usynk i søknadsdialogen");
        }
        return false;
    }

    private OppgittPeriodeEntitet justerFødselFørTermin(OppgittPeriodeEntitet periode) {
        var virkedagerIPeriode = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var nyTom = Virkedager.plusVirkedager(fødselsdato, virkedagerIPeriode - 1);
        return OppgittPeriodeBuilder.fraEksisterende(periode).medPeriode(fødselsdato, nyTom).build();
    }

    private Optional<OppgittPeriodeEntitet> justerFødselEtterTermin(OppgittPeriodeEntitet periode, List<OppgittPeriodeEntitet> oppgittePerioder) {
        var tomGrense = oppgittePerioder.size() > 1 ? oppgittePerioder.get(1).getFom().minusDays(1) : LocalDate.MAX;
        if (!fødselsdato.isBefore(tomGrense)) {
            return Optional.empty();
        }
        var virkedagerIPeriode = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var nyTom = min(Virkedager.plusVirkedager(fødselsdato, virkedagerIPeriode - 1), tomGrense);
        return Optional.of(OppgittPeriodeBuilder.fraEksisterende(periode).medPeriode(fødselsdato, nyTom).build());
    }

    private boolean harBareEnPeriodeFedrekvoteRundtFødselFraTermindato(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var førstePeriode = oppgittePerioder.get(0);
        if (!erFedrekvoteRundtTermin(førstePeriode) || !likTermindato(førstePeriode.getFom())) {
            return false;
        }
        return oppgittePerioder.stream().filter(this::liggerIIntervalletRundtTermin).count() == 1;
    }

    private boolean likTermindato(LocalDate dato) {
        return termindato.isEqual(Virkedager.justerHelgTilMandag(dato));
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
