package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

/**
 * Spoler fremover fra steg A til steg B UTEN at stegene besøkes.
 *
 * NB! Merk at denne transisjonstypen IKKE besøker stegene slik at {@see BehandlingSteg#vedHoppOverFramover} IKKE kalles på.
 */
class SpolFremoverTransisjon implements StegTransisjon {

    private final String id;
    private final BehandlingStegType målsteg;

    SpolFremoverTransisjon(BehandlingStegType målsteg) {
        this(FellesTransisjoner.SPOLFREM_PREFIX + målsteg.getKode(), målsteg);
    }

    private SpolFremoverTransisjon(String id, BehandlingStegType målsteg) {
        this.id = id;
        this.målsteg = målsteg;
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        Optional<BehandlingStegModell> funnetMålsteg = nåværendeSteg.getBehandlingModell().hvertStegEtter(nåværendeSteg.getBehandlingStegType())
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
    public Optional<BehandlingStegType> getMålstegHvisFremoverhopp() {
        return Optional.of(målsteg);
    }

    @Override
    public String toString() {
        return "FremoverhoppTransisjon{" +
            "id='" + id + '\'' +
            '}';
    }
}
