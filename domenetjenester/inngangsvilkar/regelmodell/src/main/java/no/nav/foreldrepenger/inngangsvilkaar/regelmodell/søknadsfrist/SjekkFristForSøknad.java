package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.doc.RuleOutcomeDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.specification.LeafSpecification;

/**
 * Vurderer frist for Søknad.
 *
 * Sjekkes som frist for mottak av søknad målt mot skjæringtidspunkt.
 * Eksempelvis kan frist være skjæringstidspunkt + 6 måneder for elektroniske søknader. For papirsøknader 2 ekstra
 * virkedager.
 *
 */
@RuleDocumentation(value = SjekkFristForSøknad.ID, outcomes = {
        @RuleOutcomeDocumentation(code = SjekkFristForSøknad.SØKNADSFRIST_IKKE_VURDERT_KODE, result = Resultat.IKKE_VURDERT, description = "Søknadsdato har passert frist. Output variabel: '"
                + SjekkFristForSøknad.DAGER_FOR_SENT_PROPERTY + "'")
})
public class SjekkFristForSøknad extends LeafSpecification<SøknadsfristvilkårGrunnlag> {
    public static final String SØKNADSFRIST_IKKE_VURDERT_KODE = "5007";

    public static final String DAGER_FOR_SENT_PROPERTY = "antallDagerSoeknadLevertForSent";

    private static final RuleReasonRef IKKE_OPPFYLT_ETTER_FRIST =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_5007,
            "Søknadsdato {0} er {1} dager etter frist ({2}) fra skjæringstidspunkt {3}");

    static final String ID = "FP_VK_3.2";

    private static final EnumSet<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final Period fristFørSøknad;

    private final int utvidAntallVirkedager;

    SjekkFristForSøknad(Period fristFørSøknad, int utvidAntallVirkedager) {
        super(ID);
        this.fristFørSøknad = fristFørSøknad;
        this.utvidAntallVirkedager = utvidAntallVirkedager;
    }

    @Override
    public Evaluation evaluate(SøknadsfristvilkårGrunnlag t) {
        var skjæringstidspunktDato = t.skjaeringstidspunkt();
        var søknadsDato = t.soeknadMottatDato();

        if (skjæringstidspunktDato == null) {
            throw new IllegalArgumentException("Mangler skjæringstidspunktDato i :" + t);
        }
        if (søknadsDato == null) {
            throw new IllegalArgumentException("Mangler søknadsDato i :" + t);
        }

        var fraFristDato = skjæringstidspunktDato.plus(fristFørSøknad);
        var tellVirkedagerFra = antallDagerTotaltNårTellerVirkedager(fraFristDato, utvidAntallVirkedager);
        var sisteDato = fraFristDato.plusDays(tellVirkedagerFra);

        var diffFrist = DAYS.between(sisteDato, søknadsDato);

        if (diffFrist <= 0) {
            return ja();
        }
        var kanIkkeVurdere = kanIkkeVurdere(IKKE_OPPFYLT_ETTER_FRIST, søknadsDato, diffFrist, fristFørSøknad,
                skjæringstidspunktDato);
        kanIkkeVurdere.setEvaluationProperty(DAGER_FOR_SENT_PROPERTY, String.valueOf(diffFrist));
        return kanIkkeVurdere;

    }

    private int antallDagerTotaltNårTellerVirkedager(LocalDate dato, int antallVirkedager) {
        var result = dato;
        var addedDays = 0;
        while (addedDays < antallVirkedager) {
            result = result.plusDays(1);
            if (!(WEEKEND.contains(result.getDayOfWeek()))) {
                ++addedDays;
            }
        }

        return Period.between(dato, result).getDays();
    }

    @Override
    public String beskrivelse() {
        return "Frist: (skjæringstidspunkt) + (" + fristFørSøknad
                + (utvidAntallVirkedager > 0 ? " + " + utvidAntallVirkedager + " virkedager" : "") + ")";
    }
}
