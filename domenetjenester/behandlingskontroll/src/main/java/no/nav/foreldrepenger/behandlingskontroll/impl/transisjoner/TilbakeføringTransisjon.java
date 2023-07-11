package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

class TilbakeføringTransisjon implements StegTransisjon {

    private final String id;
    private final BehandlingStegType målsteg;

    public TilbakeføringTransisjon(String id) {
        this.id = id;
        this.målsteg = null;
    }

    public TilbakeføringTransisjon(String id, BehandlingStegType målsteg) {
        this.id = id;
        this.målsteg = målsteg;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        if (målsteg == null) {
            throw new IllegalArgumentException("Utvikler-feil: skal ikke kalle nesteSteg på " + getId());
        }
        var tilModell = nåværendeSteg.getBehandlingModell().finnSteg(målsteg);
        if (tilModell == null || nåværendeSteg.getBehandlingModell().erStegAFørStegB(nåværendeSteg.getBehandlingStegType(), målsteg)) {
            throw new IllegalArgumentException("Finnes ikke noe steg av type " + målsteg + " før " + nåværendeSteg);
        }
        return tilModell;
    }

    @Override
    public Optional<BehandlingStegType> getMålstegHvisHopp() {
        return Optional.ofNullable(målsteg);
    }

    @Override
    public BehandlingStegResultat getRetningForHopp() {
        return BehandlingStegResultat.TILBAKEFØRT;
    }

    @Override
    public String toString() {
        return "TilbakeføringTransisjon{" +
                "id='" + id + '\'' +
                '}';
    }
}
