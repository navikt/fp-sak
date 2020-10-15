package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersoninfoSpråk {

    private AktørId aktørId;
    private Språkkode foretrukketSpråk;

    public PersoninfoSpråk(AktørId aktørId, Språkkode foretrukketSpråk) {
        this.aktørId = aktørId;
        this.foretrukketSpråk = foretrukketSpråk;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public Språkkode getForetrukketSpråk() {
        return foretrukketSpråk;
    }

}
