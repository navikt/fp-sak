package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;

public interface ErEndringIUttakFraEndringsdato {

    boolean vurder(LocalDate endringsdato, UttakResultatHolder uttakresultatRevurderingOpt, UttakResultatHolder uttakresultatOriginalOpt);


}
