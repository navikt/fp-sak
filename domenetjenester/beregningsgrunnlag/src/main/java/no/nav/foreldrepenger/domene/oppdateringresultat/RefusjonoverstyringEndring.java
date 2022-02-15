package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class RefusjonoverstyringEndring {

    private List<RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer;

    public RefusjonoverstyringEndring() {
    }

    public RefusjonoverstyringEndring(@Valid @Size(min = 1) @NotNull List<RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer) {
        this.refusjonperiodeEndringer = refusjonperiodeEndringer;
    }

    public List<RefusjonoverstyringPeriodeEndring> getRefusjonperiodeEndringer() {
        return refusjonperiodeEndringer;
    }

}
