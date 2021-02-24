package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class PersonIdentDto {

    private String fnr;
    private String aktoerId;
    private Diskresjonskode diskresjonskode;
    private String navn;

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public AktørId getAktoerId() {
        return aktoerId == null ? null : new AktørId(aktoerId);
    }

    void setAktoerId(AktørId aktoerId) {
        if (aktoerId != null) {
            this.aktoerId = aktoerId.getId();
        }
    }

    public String getFnr() {
        return fnr;
    }

    void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }
}
