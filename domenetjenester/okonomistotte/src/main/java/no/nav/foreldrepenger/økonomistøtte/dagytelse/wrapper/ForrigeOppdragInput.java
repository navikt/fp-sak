package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;

public class ForrigeOppdragInput {
    private final List<Oppdrag110> alleTidligereOppdrag110;
    private final TilkjentYtelse forrigeTilkjentYtelse;

    public ForrigeOppdragInput(List<Oppdrag110> tidligereOppdrag110, TilkjentYtelse forrigeTilkjentYtelse) {
        this.alleTidligereOppdrag110 = Collections.unmodifiableList(tidligereOppdrag110);
        this.forrigeTilkjentYtelse = forrigeTilkjentYtelse;
    }

    public List<Oppdrag110> getAlleTidligereOppdrag110() {
        return alleTidligereOppdrag110;
    }

    private Optional<TilkjentYtelse> getForrigeTilkjentYtelse() {
        return Optional.ofNullable(forrigeTilkjentYtelse);
    }

    public List<TilkjentYtelsePeriode> getForrigeTilkjentYtelsePerioder() {
        return getForrigeTilkjentYtelse()
            .map(TilkjentYtelse::getTilkjentYtelsePerioder)
            .orElse(Collections.emptyList());
    }
}
