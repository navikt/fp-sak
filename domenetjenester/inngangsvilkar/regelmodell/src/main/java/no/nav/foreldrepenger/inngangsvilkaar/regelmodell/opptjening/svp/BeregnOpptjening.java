package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp;

import static no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger.UTLAND;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårMellomregning;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjentTidslinje;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Slår sammen alle gjenværende aktivitet tidslinjer og akseptert mellomliggende perioder til en samlet tidslinje for
 * aktivitet, samt telle totalt antall godkjente perioder
 *
 * Telling av dager for å finne aktiviteter i opptjeningsperioden skal gjøres etter følgende regler:
 * <ul>
 * <li>1 hel kalendermåned = 1 måned med godkjent opptjening</li>
 * <li>26 kalenderdager = 1 måned med godkjent opptjening</li>
 * <li>Perioder som ikke utgjør 1 hel kalender måned eller 26 kalenderdager = x antall dager med godkjent
 * opptjening</li>
 * </ul>
 * <p>
 * Følgende legges til grunn for vurdering av opptjeningsvilkåret:
 * <ul>
 * <li>Dersom det finnes minst 5 måneder og 26 kalenderdager med godkjent opptjening i løpet av opptjeningsperioden, er
 * vilkåret oppfylt</li>
 * <li>Dersom det finnes mindre enn 5 måneder og 26 kalenderdager med arbeid, næring, ytelser og likestilte aktiviteter
 * og det finnes godkjente mellomliggende perioder som gir opptjening, er vilkåret oppfylt</li>
 * <li>Dersom det finnes mindre enn 5 måneder og 26 kalenderdager med godkjent opptjening, så er vilkåret ikke
 * oppfylt.</li>
 *
 * <li>Dersom det finnes en mellomliggende periode hos samme arbeidsgiver som er kortere enn 14 dager og bruker har vært
 * i jobb i minst 4 uker sammenhengende før den mellomliggende perioden, skal den mellomliggende perioden godkjennes som
 * opptjening.</li>
 * <li>Dersom bruker ikke har vært i jobb i minst 4 uker sammenhengende før en mellomliggende periode, skal den
 * mellomliggende perioden ikke godkjennes som opptjening.</li>
 * <li>Dersom det finnes overlappende perioder med aktivitet, skal den eller de overlappende periodene kun telles som
 * opptjening én gang.</li>
 * </ul>
 */
@RuleDocumentation(value = "FP_VK_23.1.3")
public class BeregnOpptjening extends LeafSpecification<OpptjeningsvilkårMellomregning> {
    public static final String ID = BeregnOpptjening.class.getSimpleName();


    protected BeregnOpptjening() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OpptjeningsvilkårMellomregning data) {

        Evaluation evaluation = ja();

        // Pseudo-beregn "norske" aktiviteter og finn max-dato. underkjenne utlandsk aktivitet i perioden MAX(norsk) - END(opptjeningsperiode)
        if (evaluerEvtUnderkjennUtlandskeAktiviteteter(data)) {
            evaluation.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_UNDERKJENTE_PERIODER, data.getUnderkjentePerioder());
        }

        // beregn bekreftet opptjening
        LocalDateTimeline<Boolean> bekreftetOpptjeningTidslinje = slåSammenTilFellesTidslinje(data, false, Collections.emptyList());

        Period bekreftetOpptjening = beregnTotalOpptjeningPeriode(bekreftetOpptjeningTidslinje);
        data.setBekreftetTotalOpptjening(new OpptjentTidslinje(bekreftetOpptjening, bekreftetOpptjeningTidslinje));
        evaluation.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_BEKREFTET_AKTIVITET_TIDSLINJE, bekreftetOpptjeningTidslinje);
        evaluation.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_BEKREFTET_OPPTJENING, bekreftetOpptjening);

        // beregn inklusiv antatt opptjening
        LocalDateTimeline<Boolean> antattOpptjeningTidslinje = slåSammenTilFellesTidslinje(data, true, Collections.emptyList());
        Period antattOpptjening = beregnTotalOpptjeningPeriode(antattOpptjeningTidslinje);
        data.setAntattOpptjening(new OpptjentTidslinje(antattOpptjening, antattOpptjeningTidslinje));
        // ikke sett evaluation properties for antatt før vi vet vi trenger det. (gjøre ved Sjekk av tilstrekkelig opptjening inklusiv antatt godkjent)

        return evaluation;
    }

    private LocalDateTimeline<Boolean> slåSammenTilFellesTidslinje(OpptjeningsvilkårMellomregning data, boolean medAntattGodkjent, Collection<Aktivitet> unntak) {
        LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(Collections.emptyList());

        // slå sammen alle aktivitetperioder til en tidslinje (disse er fratrukket underkjente perioder allerede)
        Map<Aktivitet, LocalDateTimeline<Boolean>> aktivitetTidslinjer = data.getAktivitetTidslinjer(medAntattGodkjent, false);
        for (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>> entry : aktivitetTidslinjer
                .entrySet()) {
            if (!unntak.contains(entry.getKey())) {
                tidslinje = tidslinje.crossJoin(entry.getValue(), StandardCombinators::alwaysTrueForMatch);
            }
        }

        tidslinje = tidslinje.compress(); // minimer tidslinje intervaller

        LocalDateTimeline<Boolean> opptjeningsTidslinje = new LocalDateTimeline<>(data.getGrunnlag().getOpptjeningPeriode(), Boolean.TRUE);

        tidslinje = tidslinje.intersection(opptjeningsTidslinje, StandardCombinators::leftOnly);
        return tidslinje;
    }

    private Period beregnTotalOpptjeningPeriode(LocalDateTimeline<Boolean> tidslinje) {
        if (tidslinje.isEmpty()) {
            return Period.ofDays(0);
        }
        return Period.between(tidslinje.getMinLocalDate(), tidslinje.getMaxLocalDate().plusDays(1));
    }

    private boolean evaluerEvtUnderkjennUtlandskeAktiviteteter(OpptjeningsvilkårMellomregning data) {
        Aktivitet utlandsFilter = new Aktivitet(UTLAND);

        LocalDateTimeline<Boolean> tidslinje = slåSammenTilFellesTidslinje(data, false, Arrays.asList(utlandsFilter));

        LocalDate maxDatoIkkeUtlandsk =  tidslinje.isEmpty() ? data.getGrunnlag().getFørsteDatoIOpptjening().minusDays(1) : tidslinje.getMaxLocalDate();

        // Må overskrive manuell godkjenning da annen aktivitet gjerne er vurdert i aksjonspunkt i steg 82
        return data.splitOgUnderkjennSegmenterEtterDatoForAktivitet(utlandsFilter, maxDatoIkkeUtlandsk);
    }
}
