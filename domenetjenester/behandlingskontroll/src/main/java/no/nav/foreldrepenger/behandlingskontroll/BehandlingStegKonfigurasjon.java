package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** For å få tak i riktig status konfigurasjon. */
public class BehandlingStegKonfigurasjon {

    private List<BehandlingStegStatus> statuser;

    public BehandlingStegKonfigurasjon(Collection<BehandlingStegStatus> list) {
        this.statuser = new ArrayList<>(list);
    }

    public BehandlingStegStatus getStartet() {
        return mapTilStatusEntitet(BehandlingStegStatus.STARTET);
    }

    public BehandlingStegStatus getInngang() {
        return mapTilStatusEntitet(BehandlingStegStatus.INNGANG);
    }

    public BehandlingStegStatus getVenter() {
        return mapTilStatusEntitet(BehandlingStegStatus.VENTER);
    }

    public BehandlingStegStatus getUtgang() {
        return mapTilStatusEntitet(BehandlingStegStatus.UTGANG);
    }

    public BehandlingStegStatus getAvbrutt() {
        return mapTilStatusEntitet(BehandlingStegStatus.AVBRUTT);
    }

    public BehandlingStegStatus getUtført() {
        return mapTilStatusEntitet(BehandlingStegStatus.UTFØRT);
    }

    public BehandlingStegStatus getTilbakeført() {
        return mapTilStatusEntitet(BehandlingStegStatus.TILBAKEFØRT);
    }

    public BehandlingStegStatus mapTilStatus(BehandlingStegResultat stegResultat) {
        var status = BehandlingStegResultat.mapTilStatus(stegResultat);
        return mapTilStatusEntitet(status);
    }

    private BehandlingStegStatus mapTilStatusEntitet(BehandlingStegStatus status) {
        return statuser.get(statuser.indexOf(status));
    }

}
