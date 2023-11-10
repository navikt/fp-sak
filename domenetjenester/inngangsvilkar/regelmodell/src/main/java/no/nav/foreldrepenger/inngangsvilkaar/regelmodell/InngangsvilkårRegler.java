package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import java.util.Set;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårEngangsstønad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårFar;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårMor;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp.OpptjeningsvilkårSvangerskapspenger;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.svp.RegelFastsettOpptjeningsperiodeSVP;

public class InngangsvilkårRegler {

    private InngangsvilkårRegler() {
    }

    public static RegelEvalueringResultat adopsjon(RegelYtelse ytelse, AdopsjonsvilkårGrunnlag grunnlag) {
        sjekkYtelse(Set.of(RegelYtelse.ENGANGSTØNAD, RegelYtelse.FORELDREPENGER), ytelse);
        var regel = RegelYtelse.FORELDREPENGER.equals(ytelse) ? new AdopsjonsvilkårForeldrepenger() : new AdopsjonsvilkårEngangsstønad();
        var evaluation = regel.evaluer(grunnlag);
        return RegelOversetter.oversett(evaluation, grunnlag);
    }

    public static RegelEvalueringResultat fødsel(RegelSøkerRolle rolle, FødselsvilkårGrunnlag grunnlag) {
        var regel = RegelSøkerRolle.MORA.equals(rolle) ? new FødselsvilkårMor() : new FødselsvilkårFar();
        var evaluation = regel.evaluer(grunnlag);
        return RegelOversetter.oversett(evaluation, grunnlag);
    }

    public static RegelEvalueringResultat medlemskap(MedlemskapsvilkårGrunnlag grunnlag) {
        var evaluation = new Medlemskapsvilkår().evaluer(grunnlag);
        return RegelOversetter.oversett(evaluation, grunnlag);
    }

    public static RegelEvalueringResultat opptjeningsperiode(RegelYtelse ytelse, OpptjeningsperiodeGrunnlag grunnlag) {
        sjekkYtelse(Set.of(RegelYtelse.FORELDREPENGER, RegelYtelse.SVANGERSKAPSPENGER), ytelse);
        var data = new OpptjeningsPeriode();
        var regel = RegelYtelse.FORELDREPENGER.equals(ytelse) ? new RegelFastsettOpptjeningsperiode() : new RegelFastsettOpptjeningsperiodeSVP();
        var evaluation = regel.evaluer(grunnlag, data);
        return RegelOversetter.oversett(evaluation, grunnlag, data);
    }

    public static RegelEvalueringResultat opptjening(RegelYtelse ytelse, Opptjeningsgrunnlag grunnlag) {
        sjekkYtelse(Set.of(RegelYtelse.FORELDREPENGER, RegelYtelse.SVANGERSKAPSPENGER), ytelse);
        var output = new OpptjeningsvilkårResultat();
        var regel = RegelYtelse.FORELDREPENGER.equals(ytelse) ? new OpptjeningsvilkårForeldrepenger() : new OpptjeningsvilkårSvangerskapspenger();
        var evaluation = regel.evaluer(grunnlag, output);
        return RegelOversetter.oversett(evaluation, grunnlag, output);
    }

    private static void sjekkYtelse(Set<RegelYtelse> lovlige, RegelYtelse aktuell) {
        if (aktuell == null || !lovlige.contains(aktuell)) {
            throw new RegelOversetter.InngangsvilkårRegelFeil("Utviklerfeil: Ulovlig ytelse");
        }
    }

}
