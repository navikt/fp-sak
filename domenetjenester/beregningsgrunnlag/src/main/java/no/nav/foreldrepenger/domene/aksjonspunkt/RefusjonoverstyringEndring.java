package no.nav.foreldrepenger.domene.aksjonspunkt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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
