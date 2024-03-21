package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

public class FaktaOmUttakReutledetAksjonspunkt extends RuntimeException {
    public FaktaOmUttakReutledetAksjonspunkt() {
        super("Lagrede perioder fører til at aksjonspunkt reutledes");
    }
}
