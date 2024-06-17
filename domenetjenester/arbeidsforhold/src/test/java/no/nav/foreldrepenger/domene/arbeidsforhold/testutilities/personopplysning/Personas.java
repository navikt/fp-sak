package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class Personas {
    private Builder builder;
    private AktørId aktørId;
    private Personopplysning.Builder persInfoBuilder;
    private LocalDate fødselsdato;

    public Personas(Builder builder) {
        this.builder = builder;
    }

    public Personas voksenPerson(AktørId aktørId, SivilstandType st) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.persInfoBuilder = Personopplysning.builder();
            this.fødselsdato = LocalDate.now().minusYears(30); // VOKSEN
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder.aktørId(aktørId).fødselsdato(fødselsdato).sivilstand(st));
        return this;
    }

    public Personas kvinne(AktørId aktørId) {
        voksenPerson(aktørId, SivilstandType.SAMBOER);
        return this;
    }

    public PersonInformasjon build() {
        return builder.build();
    }

}
