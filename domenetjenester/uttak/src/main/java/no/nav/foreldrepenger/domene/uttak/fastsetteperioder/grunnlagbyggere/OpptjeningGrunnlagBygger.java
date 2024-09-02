package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Opptjening;

@ApplicationScoped
public class OpptjeningGrunnlagBygger {

    public Opptjening.Builder byggGrunnlag(UttakInput input) {
        return new Opptjening.Builder()
            .skjæringstidspunkt(input.getSkjæringstidspunkt().orElseThrow().getUtledetSkjæringstidspunkt());
    }
}
