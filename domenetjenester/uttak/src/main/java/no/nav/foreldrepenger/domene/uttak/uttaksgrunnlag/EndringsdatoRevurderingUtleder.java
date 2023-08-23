package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.time.LocalDate;

public interface EndringsdatoRevurderingUtleder {
    LocalDate utledEndringsdato(UttakInput input);
}
