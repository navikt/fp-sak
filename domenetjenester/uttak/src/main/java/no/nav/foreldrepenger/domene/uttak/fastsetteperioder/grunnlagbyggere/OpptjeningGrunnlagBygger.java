package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Opptjening;

@ApplicationScoped
public class OpptjeningGrunnlagBygger {

    public Opptjening.Builder byggGrunnlag(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        return new Opptjening.Builder()
            .skjæringstidspunkt(ref.getUtledetSkjæringstidspunkt());
    }
}
