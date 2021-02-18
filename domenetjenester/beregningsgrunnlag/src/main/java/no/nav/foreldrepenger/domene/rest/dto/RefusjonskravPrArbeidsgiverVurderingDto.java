package no.nav.foreldrepenger.domene.rest.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class RefusjonskravPrArbeidsgiverVurderingDto {

    @NotNull
    @Pattern(regexp = "[\\d]{9}|[\\d]{13}")
    private String arbeidsgiverId;

    private boolean skalUtvideGyldighet;


    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public boolean isSkalUtvideGyldighet() {
        return skalUtvideGyldighet;
    }

    public void setSkalUtvideGyldighet(boolean skalUtvideGyldighet) {
        this.skalUtvideGyldighet = skalUtvideGyldighet;
    }
}
