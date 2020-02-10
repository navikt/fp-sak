package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class NavBrukerBuilder {

    private NavBruker bruker;

    private AktørId aktørId = AktørId.dummy();
    private Personinfo personinfo;
    private NavBrukerKjønn kjønn;

    public NavBrukerBuilder() {
        // default ctor
    }

    public NavBrukerBuilder medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    public NavBrukerBuilder medBruker(NavBruker bruker) {
        this.bruker = bruker;
        return this;
    }

    public NavBrukerBuilder medPersonInfo(Personinfo personinfo) {
        this.personinfo = personinfo;
        return this;
    }

    public NavBrukerBuilder medKjønn(NavBrukerKjønn kjønn) {
        this.kjønn = kjønn;
        return this;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public AktørId getAktørId() {
        if(bruker != null) {
            return bruker.getAktørId();
        }
        return aktørId;
    }

    public NavBruker build() {
        if (bruker != null) {
            return bruker;
        }
        if (personinfo == null) {

            personinfo = new NavPersoninfoBuilder()
                .medAktørId(aktørId)
                .medKjønn(Optional.ofNullable(kjønn).orElse(KVINNE))
                .build();
        }
        return NavBruker.opprettNy(personinfo);
    }
}
