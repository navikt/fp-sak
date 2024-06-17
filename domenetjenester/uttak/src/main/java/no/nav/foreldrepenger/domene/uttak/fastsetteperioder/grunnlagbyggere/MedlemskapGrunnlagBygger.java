package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Medlemskap;

@ApplicationScoped
public class MedlemskapGrunnlagBygger {

    public Medlemskap.Builder byggGrunnlag(UttakInput input) {
        return new Medlemskap.Builder().opphørsdato(input.getMedlemskapOpphørsdato().orElse(null));
    }
}
