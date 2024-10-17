package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

public class Personas {
    private Builder builder;
    private AktørId aktørId;
    private Personopplysning.Builder persInfoBuilder;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;

    public Personas(Builder builder) {
        this.builder = builder;
        this.persInfoBuilder = Personopplysning.builder();
    }

    public Personas voksenPerson(AktørId aktørId, SivilstandType st, NavBrukerKjønn kjønn) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.persInfoBuilder = Personopplysning.builder();
            this.fødselsdato = LocalDate.now().minusYears(30);  // VOKSEN
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder
            .aktørId(aktørId)
            .brukerKjønn(kjønn)
            .fødselsdato(fødselsdato)
            .sivilstand(st));
        return this;
    }

    public Personas barn(AktørId aktørId, LocalDate fødselsdato) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.fødselsdato = fødselsdato;
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder
            .aktørId(aktørId)
            .fødselsdato(fødselsdato)
            .brukerKjønn(NavBrukerKjønn.MANN)
            .sivilstand(SivilstandType.UOPPGITT));
        return this;
    }

    public Personas dødsdato(LocalDate dødsdato) {
        this.persInfoBuilder.dødsdato(dødsdato);
        return this;
    }

    public Personas statsborgerskap(Landkoder landkode) {
        // NB: logik her tilsvarer at dersom dødsdato skal settes bør det settes før.
        return statsborgerskap(landkode, fødselsdato, dødsdato);
    }

    public Personas statsborgerskap(Landkoder landkode, LocalDate fom, LocalDate tom) {
        builder.leggTilStatsborgerskap(new Statsborgerskap(aktørId, new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(fom, tom), landkode)));
        return this;
    }

    public Personas personstatus(PersonstatusType personstatus) {
        return personstatus(personstatus, fødselsdato, dødsdato);
    }

    public Personas personstatus(PersonstatusType personstatus, LocalDate fom, LocalDate tom) {
        builder.leggTilPersonstatus(new Personstatus(aktørId, new PersonstatusPeriode(Gyldighetsperiode.innenfor(fom, tom), personstatus)));
        return this;
    }

    public Personas opphold(OppholdstillatelseType oppholdstillatelseType, LocalDate fom, LocalDate tom) {
        builder.leggTilOpphold(new Oppholdstillatelse(aktørId, new OppholdstillatelsePeriode(Gyldighetsperiode.innenfor(fom, tom), oppholdstillatelseType)));
        return this;
    }

    public Personas kvinne(AktørId aktørId) {
        voksenPerson(aktørId, SivilstandType.SAMBOER, NavBrukerKjønn.KVINNE);
        return this;
    }

    public Personas kvinne(AktørId aktørId, SivilstandType st) {
        voksenPerson(aktørId, st, NavBrukerKjønn.KVINNE);
        return this;
    }

    public Personas mann(AktørId aktørId, SivilstandType st) {
        voksenPerson(aktørId, st, NavBrukerKjønn.MANN);
        return this;
    }

    public Personas fødtBarn(AktørId aktørId, LocalDate fødselsdato) {
        barn(aktørId, fødselsdato);
        return this;
    }


    public Personas bostedsadresse(AdressePeriode adresseBuilder) {
        return adresse(AdresseType.BOSTEDSADRESSE, adresseBuilder);
    }

    public Personas adresse(AdresseType adresseType, AdressePeriode adresseBuilder) {
        var ap = adresseBuilder.gyldighetsperiode() != null ? adresseBuilder :
            new AdressePeriode(Gyldighetsperiode.innenfor(LocalDate.of(2000, 1, 1), Tid.TIDENES_ENDE), adresseBuilder.adresse());

        builder.leggTilAdresser(new PersonAdresse(aktørId, ap));
        return this;
    }

    public Builder mann(AktørId aktørId) {
        voksenPerson(aktørId, SivilstandType.SAMBOER, NavBrukerKjønn.MANN);
        return builder;
    }

    public PersonInformasjon build() {
        return builder.build();
    }

    public Personas relasjonTil(AktørId tilAktørId, RelasjonsRolleType rolle) {
        Boolean sammeBosted = true;
        return relasjonTil(tilAktørId, rolle, sammeBosted);
    }

    public Personas relasjonTil(AktørId tilAktørId, RelasjonsRolleType rolle, Boolean sammeBosted) {
        builder.leggTilRelasjon(PersonRelasjon.builder().fraAktørId(aktørId).tilAktørId(tilAktørId).relasjonsrolle(rolle).harSammeBosted(sammeBosted));
        return this;
    }

}
