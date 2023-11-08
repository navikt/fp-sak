package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp;

import java.util.Arrays;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårMellomregning;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårParametre;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.SjekkInntektSamsvarerMedArbeidAktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.SjekkTilstrekkeligOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.SjekkTilstrekkeligOpptjeningInklAntatt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.SequenceSpecification;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Regeltjeneste for vurdering av OpptjeningsVilkåret.
 * <p>
 * Dette vurderes som følger:
 * <p>
 * <ul>
 * <li>Perioder med arbeidsaktivitet fra AAReg som ikke samsvarer med registrerte inntekter underkjennes</li>
 * <li>Mellomliggende perioder for en arbeidsgiver (under 14 dager) og derforegående periode er mer enn 4 uker
 * aksepteres som godkjent aktivitet</li>
 * <li>Måneder der et er aktivitet hele måneden, eller minst 26 dager regnes som en måned.
 * <li>Hvis det er mindre enn 26 dager telles disse som enkelt dager</li>
 * <li>Bruker må ha minst 5 måneder (5*26 dager) og 26 dager med godkjente aktiviteter i opptjeningsperiode for at
 * vilkåret skal være oppfylt</li>
 * </ul>
 *
 * <p>
 * Aktiviteter som inngår er:
 * <ul>
 * <li>Arbeid - registrert arbeidsforhold i AA-registeret</li>
 * <li>Næring - Registrert i Enhetsregisteret som selvstendig næringsdrivende</li>
 * <li>Ytelser - Dagpenger, Arbeidsavklaringspenger, Foreldrepenger, Sykepenger, Svangerskapspenger, Opplæringspenger,
 * Omsorgspenger og Pleiepenger</li>
 * <li>Pensjonsgivende inntekt som likestilles med yrkesaktivitet = Lønn fra arbeidsgiver i fbm videre- og
 * etterutdanning, Ventelønn, Vartpenger, Etterlønn/sluttvederlag fra arbeidsgiver, Avtjening av militær- eller
 * siviltjeneste eller obligatorisk sivilforsvarstjeneste.</li>
 * </ul>
 */
@RuleDocumentation(value = OpptjeningsvilkårForeldrepenger.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=174836170", legalReference = "FP_VK 23 § 14-6")
public class OpptjeningsvilkårForeldrepenger implements RuleService<Opptjeningsgrunnlag> {

    public static final String ID = "FP_VK_23";

    /**
     * Evaluation Property: Perioder underkjennes dersom de ikke aksepteres av
     * {@link SjekkInntektSamsvarerMedArbeidAktivitet}.
     */
    public static final String EVAL_RESULT_UNDERKJENTE_PERIODER = "underkjentePerioder";

    /**
     * Evaluation Property: Perioder aksepteres dersom de aksepteres av {@link SjekkMellomliggendePerioderForArbeid}.
     */
    public static final String EVAL_RESULT_AKSEPTERT_MELLOMLIGGENDE_PERIODE = "akseptertMellomliggendePerioder";

    /** Evaluation Property: Bekreftet opptjening aktivitet tidslinje bestemmes av {@link BeregnOpptjening}. */
    public static final String EVAL_RESULT_BEKREFTET_AKTIVITET_TIDSLINJE = "bekreftetOpptjeningAktivitetTidslinje";

    /** Evaluation Property: Antatt opptjening aktivitet tidslinje bestemmes av {@link BeregnOpptjening}. Settes dersom Bekreftet tidslinje ikke gir nok opptjening. */
    public static final String EVAL_RESULT_ANTATT_AKTIVITET_TIDSLINJE = "antattOpptjeningAktivitetTidslinje";

    /** Evaluation Property: Bekreftet opptjening uttrykt som en Period (ISO 8601). eks. P4M22D = 4 måneder + 22 dager. */
    public static final String EVAL_RESULT_BEKREFTET_OPPTJENING = "bekreftetOpptjening";

    /** Evaluation Property: Antatt opptjening uttrykt som en Period (ISO 8601). eks. P4M22D = 4 måneder + 22 dager. Settes dersom Antatt opptjening ikke er nok.*/
    public static final String EVAL_RESULT_ANTATT_OPPTJENING = "antattOpptjening";

    /** Evaluation Property: Antatt godkjente perioder med arbeid. */
    public static final String EVAL_RESULT_ANTATT_GODKJENT = "antattGodkjentArbeid";

    /// TODO (FC) Konstant for AAReg Arbeid
    public static final String ARBEID = "ARBEID";
    public static final String FRILANSREGISTER = "FRILOPP";
    public static final String LØNN = "LØNN";
    public static final String UTLAND = "UTENLANDSK_ARBEIDSFORHOLD";

    @Override
    public Evaluation evaluer(Opptjeningsgrunnlag grunnlag, Object output) {
        var grunnlagOgMellomregning = new OpptjeningsvilkårMellomregning(grunnlag, OpptjeningsvilkårParametre.opptjeningsparametreForeldrepenger());
        var evaluation = getSpecification().evaluate(grunnlagOgMellomregning);

        // kopier ut resultater og sett resultater
        grunnlagOgMellomregning.oppdaterOutputResultat((OpptjeningsvilkårResultat) output);

        return evaluation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<OpptjeningsvilkårMellomregning> getSpecification() {
        var rs = new Ruleset<OpptjeningsvilkårMellomregning>();

        Specification<OpptjeningsvilkårMellomregning> sjekkOpptjeningsvilkåret = rs.hvisRegel("FP_VK 23.2", "Hvis tilstrekkelig opptjening")
                .hvis(new SjekkTilstrekkeligOpptjening(), new Oppfylt())
                .ellers(new SjekkTilstrekkeligOpptjeningInklAntatt());

        return new SequenceSpecification<>("FP_VK 23.1",
                "Sammenstill Arbeid aktivitet med Inntekt, og Mellomliggende perioder",
                Arrays.asList(
                        new SjekkInntektSamsvarerMedArbeidAktivitet(),
                        new SjekkMellomliggendePerioderForArbeid(),
                        new BeregnOpptjening(),
                        sjekkOpptjeningsvilkåret));
    }

}
