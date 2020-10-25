package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;

public class GeografiskTilknytning {

    private String tilknytning;
    private Diskresjonskode diskresjonskode;

    public GeografiskTilknytning(String geografiskTilknytning, Diskresjonskode diskresjonskode) {
        this.tilknytning = geografiskTilknytning;
        this.diskresjonskode = diskresjonskode;
    }

    public String getTilknytning() {
        return tilknytning;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }
}
