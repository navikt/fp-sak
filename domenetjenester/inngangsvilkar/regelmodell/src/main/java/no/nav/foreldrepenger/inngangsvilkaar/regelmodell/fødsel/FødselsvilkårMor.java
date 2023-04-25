package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.IkkeOppfylt;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Denne implementerer regeltjenesten som validerer fødselsvilkåret (FP_VK_1)
 * Data underlag definisjoner:<br>
 * - Bekreftet passet 22 svangerskapsuker dato: termindato - 3 dager - 14 uker<br>
 * - Bekreftet passert 22 svangerskapsuker: søknadsdato>=passet 22 svangerskapsuker<br>
 * <p>
 * VilkårUtfall IKKE_OPPFYLT:<br>
 * - Hvis ikke kvinne: Returner VilkårUtfallMerknad 1003. <br>
 * - Hvis kvinne, fødsel registerert, søker ikke barnets mor: 1002<br>
 * - Hvis kvinne, fødsel ikke registrert, ikke passert 22 svangerskapsuker: 1001<br>
 * <p>
 * VilkårUtfall OPPFYLT:<br>
 * - Fødsel registrert og søker er barnets mor (MORA)<br>
 * - Fødsel ikke registert, søker er kvinne, og passert 22 svangerskapsuker.<br>
 */
@RuleDocumentation(value = FødselsvilkårMor.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=173827762")
public class FødselsvilkårMor implements RuleService<FødselsvilkårGrunnlag> {

    public static final String ID = "FP_VK_1";

    @Override
    public Evaluation evaluer(FødselsvilkårGrunnlag data) {
        return getSpecification().evaluate(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<FødselsvilkårGrunnlag> getSpecification() {
        var rs = new Ruleset<FødselsvilkårGrunnlag>();


        Specification<FødselsvilkårGrunnlag> bekreftelseIkkeUtstedtMerEnn10UkerOg3DagerFørTerminNode =
            rs.hvisRegel(ID, "Hvis utstedtdato for terminbekreftelse er innenfor ...")
                .hvis(new SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker.IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO));

        Specification<FødselsvilkårGrunnlag> søknadsdatoPassertXSvangerskapsukeNode =
            rs.hvisRegel("FP_VK_1.2.1.1", "Hvis søknadsdato har passert X svangerskapsuke ...")
                .hvis(new SjekkBehandlingsdatoPassertXSvangerskapsUker(), bekreftelseIkkeUtstedtMerEnn10UkerOg3DagerFørTerminNode)
                .ellers(new IkkeOppfylt(SjekkBehandlingsdatoPassertXSvangerskapsUker.IKKE_OPPFYLT_PASSERT_TIDLIGSTE_SVANGERSKAPSUKE_KAN_BEHANDLES));

        Specification<FødselsvilkårGrunnlag> burdeFødselHaInntruffetNode =
            rs.hvisRegel("FP_VK_1.2.1", "Hvis ikke fødsel burde ha inntruffet ...")
                .hvis(new SjekkErDetForTidligeForAtFødselBurdeHaInntruffet(), søknadsdatoPassertXSvangerskapsukeNode)
                .ellers(new IkkeOppfylt(SjekkErDetForTidligeForAtFødselBurdeHaInntruffet.FØDSEL_BURDE_HA_INNTRUFFET));

        Specification<FødselsvilkårGrunnlag> søkerErMorNode =
            rs.hvisRegel("FP_VK_1.2.2", "Hvis søker er mor ...")
                .hvis(new SjekkSøkerErMor(), new Oppfylt())
                .ellers(new IkkeOppfylt(SjekkSøkerErMor.IKKE_OPPFYLT_FØDSEL_REGISTRERT_SØKER_IKKE_BARNETS_MOR));

        Specification<FødselsvilkårGrunnlag> harSøkerFødtNode = rs
            .hvisRegel("FP_VK_1.2", "Hvis ikke fødsel er registert ...")
            .hvis(new SjekkFødselErRegistrert(), søkerErMorNode)
            .ellers(burdeFødselHaInntruffetNode);

        return rs.hvisRegel("FP_VK_1.1", "Hvis søker er kvinne ...")
            .hvis(new SjekkSøkerErKvinne(), harSøkerFødtNode)
            .ellers(new IkkeOppfylt(SjekkSøkerErKvinne.IKKE_OPPFYLT_SØKER_ER_KVINNE));
    }
}
