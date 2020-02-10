package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

public interface EndringsdatoRevurderingUtleder {
    LocalDate utledEndringsdato(UttakInput input);
}
