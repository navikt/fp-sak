package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

import java.util.Optional;

/**
 * Hopper fremover fra steg A til steg B slik at stegene besøkes.
 *
 * NB! Merk at denne transisjonstypen besøker stegene slik at
 * {@see BehandlingSteg#vedHoppOverFramover} kalles på.
 */
class FremoverhoppTransisjon implements StegTransisjon {

    private final String id;
    private final BehandlingStegType målsteg;

    public FremoverhoppTransisjon(String id, BehandlingStegType målsteg) {
        this.id = id;
        this.målsteg = målsteg;
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        var funnetMålsteg = nåværendeSteg.getBehandlingModell().hvertStegEtter(nåværendeSteg.getBehandlingStegType())
                .filter(s -> s.getBehandlingStegType().equals(målsteg))
                .findFirst();
        if (funnetMålsteg.isPresent()) {
            return funnetMålsteg.get();
        }
        throw new IllegalArgumentException("Finnes ikke noe steg av type " + målsteg + " etter " + nåværendeSteg);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<BehandlingStegType> getMålstegHvisHopp() {
        return Optional.of(målsteg);
    }

    @Override
    public BehandlingStegResultat getRetningForHopp() {
        return BehandlingStegResultat.FREMOVERFØRT;
    }

    @Override
    public String toString() {
        return "FremoverhoppTransisjon{" +
                "id='" + id + '\'' +
                '}';
    }
}
