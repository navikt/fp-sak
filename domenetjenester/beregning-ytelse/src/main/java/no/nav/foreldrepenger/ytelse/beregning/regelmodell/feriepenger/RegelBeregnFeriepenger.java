package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelBeregnFeriepenger.ID, specificationReference = "https://confluence.adeo.no/display/MODNAV/27c+Beregn+feriepenger+PK-51965+OMR-49")
public class RegelBeregnFeriepenger implements RuleService<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "";
    public static final String BESKRIVELSE = "RegelBeregnFeriepenger";

    @Override
    public Evaluation evaluer(BeregningsresultatFeriepengerRegelModell regelmodell) {
        return getSpecification().evaluate(regelmodell);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<BeregningsresultatFeriepengerRegelModell> getSpecification() {
        var rs = new Ruleset<BeregningsresultatFeriepengerRegelModell>();

        // FP_BR 8.7 Beregn feriepenger (Ett kalenderår)
        var beregnFeriepengerEttÅr =
            rs.beregningsRegel(BeregnFeriepengerEttÅr.ID, BeregnFeriepengerEttÅr.BESKRIVELSE, new BeregnFeriepengerEttÅr(), new GrupperOgSummerFeriepengerOverÅr(), new BeregnetFeriepenger());

        // FP_BR 8.6 Beregn feriepenger (Flere kalenderår)
        var beregnFeriepengerFlereÅr =
            rs.beregningsRegel(BeregnFeriepengerFlereÅr.ID, BeregnFeriepengerFlereÅr.BESKRIVELSE, new BeregnFeriepengerFlereÅr(), new GrupperOgSummerFeriepengerOverÅr(), new BeregnetFeriepenger());

        // FP_BR 8.5 Går feriepengeperioden over flere kalenderår?
        var sjekkOmFeriepengePeriodenGårOverFlereÅr =
            rs.beregningHvisRegel(new SjekkOmFeriepengePeriodenGårOverFlereÅr(), beregnFeriepengerFlereÅr, beregnFeriepengerEttÅr);

        //FP_BR 8.4 Har bruker uttak i feriepengeperiode?
        var sjekkOmUttakIFeriepengePeriode =
            rs.beregningHvisRegel(new SjekkBrukerHarOmUttakIFeriepengePeriode(), sjekkOmFeriepengePeriodenGårOverFlereÅr, new BeregnetFeriepenger());

        //FP_BR 8.3 Finn brukers feriepengeperiode
        var finnBrukersFeriepengePeriode =
            rs.beregningsRegel(FinnBrukersFeriepengePeriode.ID, FinnBrukersFeriepengePeriode.BESKRIVELSE, new FinnBrukersFeriepengePeriode(), sjekkOmUttakIFeriepengePeriode);

        // FP_BR 8.8 Har bruker flere dager ignen av feriekvoten?
        var sjekkOmBrukerHarDagerIgjenAvFeriekvote =
            rs.beregningHvisRegel(new SjekkOmBrukerHarFeriekvoteTilgjengelig(), finnBrukersFeriepengePeriode, new BeregnetFeriepenger());

        // FP_BR 8.2 Har bruker fått utbetalt foreldrepenger i den totale stønadsperioden?
        var sjekkOmBrukerHarFåttUtbetaltFP =
            rs.beregningHvisRegel(new SjekkOmBrukerHarFåttUtbetaltForeldrepenger(), sjekkOmBrukerHarDagerIgjenAvFeriekvote, new BeregnetFeriepenger());

        // FP_BR 8.1 Er brukers inntektskategori arbeidstaker eller sjømann?

        return rs.beregningHvisRegel(new SjekkOmBrukerHarInntektkategoriATellerSjømann(), sjekkOmBrukerHarFåttUtbetaltFP, new BeregnetFeriepenger());
    }
}
