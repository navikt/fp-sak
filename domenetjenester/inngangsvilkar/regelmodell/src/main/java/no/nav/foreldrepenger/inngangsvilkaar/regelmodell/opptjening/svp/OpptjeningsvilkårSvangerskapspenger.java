package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.*;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.SequenceSpecification;
import no.nav.fpsak.nare.specification.Specification;

import java.util.Arrays;

/**
 * Regeltjeneste for vurdering av OpptjeningsVilkåret tilpasset Svangerskapspenger
 * <p>
 * Dette vurderes som følger:
 *
 * Perioden i arbeidsm må være på eller mer 28 dager, får antatt godkjent i perioden siden innteket ikke har tilkommet på tidspunktet.
 *
 * <p>
 * Aktiviteter som inngår er:
 * <ul>
 * <li>Arbeid - registrert arbeidsforhold i AA-registeret</li>
 * <li>Næring - Registrert i Enhetsregisteret som selvstendig næringsdrivende</li>
 * <li>Ytelser - Dagpenger, Foreldrepenger, Sykepenger, Svangerskapspenger, Opplæringspenger,
 * Omsorgspenger og Pleiepenger</li>
 * <li>Pensjonsgivende inntekt som likestilles med yrkesaktivitet = Lønn fra arbeidsgiver i fbm videre- og
 * etterutdanning, Ventelønn, Vartpenger, Etterlønn/sluttvederlag fra arbeidsgiver, Avtjening av militær- eller
 * siviltjeneste eller obligatorisk sivilforsvarstjeneste.</li>
 * </ul>
 */
@RuleDocumentation(value = OpptjeningsvilkårSvangerskapspenger.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=174836170", legalReference = "FP_VK 23 § 14-6")
public class OpptjeningsvilkårSvangerskapspenger implements RuleService<Opptjeningsgrunnlag> {

    public static final String ID = "FP_VK_23";

    @Override
    public Evaluation evaluer(Opptjeningsgrunnlag grunnlag, Object output) {
        var grunnlagOgMellomregning = new OpptjeningsvilkårMellomregning(grunnlag, OpptjeningsvilkårParametre.opptjeningsparametreSvangerskapspenger());
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
                "Sammenstill Arbeid aktivitet med Inntekt",
                Arrays.asList(
                        new SjekkInntektSamsvarerMedArbeidAktivitet(),
                        new BeregnOpptjening(),
                        sjekkOpptjeningsvilkåret));
    }
}
