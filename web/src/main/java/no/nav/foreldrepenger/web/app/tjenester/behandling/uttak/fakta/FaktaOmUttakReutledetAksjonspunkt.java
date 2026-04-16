package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.VLLogLevel;

public class FaktaOmUttakReutledetAksjonspunkt extends FunksjonellException {
    public FaktaOmUttakReutledetAksjonspunkt() {
        super("FP-273197", "Lagrede perioder fører til at aksjonspunkt reutledes");
    }

    @Override
    public String getFeilkode() {
        return "FAKTA_UTTAK_REUTLEDET";
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.NOLOG;
    }
}
