package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkFarMedmorUttakRundtFødsel extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkFarMedmorUttakRundtFødsel.class.getSimpleName();

    SjekkFarMedmorUttakRundtFødsel() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag grunnlag) {
        if (grunnlag.farMedmorUttakRundtFødsel()) {
            return ja();
        }
        return nei();
    }
}
