package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RefusjonoverstyringEndring {

    private List<RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer;

    public RefusjonoverstyringEndring() {
    }

    public RefusjonoverstyringEndring(@Size(min = 1) @NotNull List<@Valid RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer) {
        this.refusjonperiodeEndringer = refusjonperiodeEndringer;
    }

    public List<RefusjonoverstyringPeriodeEndring> getRefusjonperiodeEndringer() {
        return refusjonperiodeEndringer;
    }

}
