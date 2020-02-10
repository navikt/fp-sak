package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Medlemskap;

@ApplicationScoped
public class MedlemskapGrunnlagBygger {

    public Medlemskap.Builder byggGrunnlag(UttakInput input) {
        return new Medlemskap.Builder()
                .medOpphørsdato(input.getMedlemskapOpphørsdato().orElse(null));
    }
}
