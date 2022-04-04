package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

@Entity(name = PersonInformasjonEntitet.ENTITY_NAME)
@Table(name = "PO_INFORMASJON")
public class PersonInformasjonEntitet extends BaseEntitet {

    public static final String ENTITY_NAME = "PersonInformasjon";

    private static final String REF_NAME = "personopplysningInformasjon";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_INFORMASJON")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonstatusEntitet> personstatuser = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<OppholdstillatelseEntitet> oppholdstillatelser = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<StatsborgerskapEntitet> statsborgerskap = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonAdresseEntitet> adresser = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonopplysningEntitet> personopplysninger = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonRelasjonEntitet> relasjoner = new ArrayList<>();

    PersonInformasjonEntitet() {
    }

    PersonInformasjonEntitet(PersonInformasjonEntitet aggregat) {
        if (Optional.ofNullable(aggregat.getAdresser()).isPresent()) {
            aggregat.getAdresser()
            .forEach(e -> {
                var entitet = new PersonAdresseEntitet(e);
                adresser.add(entitet);
                entitet.setPersonopplysningInformasjon(this);
            });
        }
        if (Optional.ofNullable(aggregat.getPersonstatus()).isPresent()) {
            aggregat.getPersonstatus()
            .forEach(e -> {
                var entitet = new PersonstatusEntitet(e);
                personstatuser.add(entitet);
                entitet.setPersonInformasjon(this);
            });
        }
        if (Optional.ofNullable(aggregat.getOppholdstillatelser()).isPresent()) {
            aggregat.getOppholdstillatelser()
                .forEach(e -> {
                    var entitet = new OppholdstillatelseEntitet(e);
                    oppholdstillatelser.add(entitet);
                    entitet.setPersonInformasjon(this);
                });
        }
        if (Optional.ofNullable(aggregat.getStatsborgerskap()).isPresent()) {
            aggregat.getStatsborgerskap()
            .forEach(e -> {
                var entitet = new StatsborgerskapEntitet(e);
                statsborgerskap.add(entitet);
                entitet.setPersonopplysningInformasjon(this);
            });
        }
        if (Optional.ofNullable(aggregat.getRelasjoner()).isPresent()) {
            aggregat.getRelasjoner()
            .forEach(e -> {
                var entitet = new PersonRelasjonEntitet(e);
                relasjoner.add(entitet);
                entitet.setPersonopplysningInformasjon(this);
            });
        }
        if (Optional.ofNullable(aggregat.getPersonopplysninger()).isPresent()) {
            aggregat.getPersonopplysninger()
            .forEach(e -> {
                var entitet = new PersonopplysningEntitet(e);
                personopplysninger.add(entitet);
                entitet.setPersonopplysningInformasjon(this);
            });
        }
    }

    void leggTilAdresse(PersonAdresseEntitet adresse) {
        final var adresse1 = adresse;
        adresse1.setPersonopplysningInformasjon(this);
        adresser.add(adresse1);
    }

    void leggTilStatsborgerskap(StatsborgerskapEntitet statsborgerskap) {
        final var statsborgerskap1 = statsborgerskap;
        statsborgerskap1.setPersonopplysningInformasjon(this);
        this.statsborgerskap.add(statsborgerskap1);
    }

    void leggTilPersonstatus(PersonstatusEntitet personstatus) {
        final var personstatus1 = personstatus;
        personstatus1.setPersonInformasjon(this);
        this.personstatuser.add(personstatus1);
    }

    void leggTilOppholdstillatelse(OppholdstillatelseEntitet oppholdstillatelse) {
        final var oppholdstillatelse1 = oppholdstillatelse;
        oppholdstillatelse1.setPersonInformasjon(this);
        this.oppholdstillatelser.add(oppholdstillatelse1);
    }

    void leggTilPersonrelasjon(PersonRelasjonEntitet relasjon) {
        final var relasjon1 = relasjon;
        relasjon1.setPersonopplysningInformasjon(this);
        this.relasjoner.add(relasjon1);
    }

    void leggTilPersonopplysning(PersonopplysningEntitet personopplysning) {
        final var personopplysning1 = personopplysning;
        personopplysning1.setPersonopplysningInformasjon(this);
        this.personopplysninger.add(personopplysning1);
    }

    void fjernPersonopplysning(AktørId aktørId) {
        this.personopplysninger.removeIf(e -> e.getAktørId().equals(aktørId));
    }

    void fjernPersonrelasjon(PersonRelasjonEntitet relasjon) {
        relasjoner.removeIf(e -> e.equals(relasjon));
    }

    /**
     * Rydder bort alt unntatt personopplysninger
     */
    void tilbakestill() {
        this.adresser.clear();
        this.personstatuser.clear();
        this.oppholdstillatelser.clear();
        this.relasjoner.clear();
        this.statsborgerskap.clear();
    }

    PersonInformasjonBuilder.PersonopplysningBuilder getPersonBuilderForAktørId(AktørId aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        final var eksisterendeAktør = personopplysninger.stream().filter(it -> it.getAktørId().equals(aktørId)).findFirst();
        return PersonInformasjonBuilder.PersonopplysningBuilder.oppdater(eksisterendeAktør).medAktørId(aktørId);
    }

    /**
     * Relasjoner mellom to aktører
     *
     * @return entitet
     */
    public List<PersonRelasjonEntitet> getRelasjoner() {
        return Collections.unmodifiableList(relasjoner);
    }

    /**
     * Alle relevante aktørers personopplysninger
     *
     * @return entitet
     */
    public List<PersonopplysningEntitet> getPersonopplysninger() {
        return Collections.unmodifiableList(personopplysninger);
    }

    /**
     * Alle relevante aktørers personstatuser med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<PersonstatusEntitet> getPersonstatus() {
        return Collections.unmodifiableList(personstatuser);
    }

    /**
     * Eventuelle oppholdstillatelser med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn data med historikk for søker, tillatelser for øvrige aktører blir ikke innhentet
     *
     * @return entitet
     */
    public List<OppholdstillatelseEntitet> getOppholdstillatelser() {
        return Collections.unmodifiableList(oppholdstillatelser);
    }

    /**
     * Alle relevante aktørers statsborgerskap med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<StatsborgerskapEntitet> getStatsborgerskap() {
        return Collections.unmodifiableList(statsborgerskap);
    }

    /**
     * Alle relevante aktørers adresser med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<PersonAdresseEntitet> getAdresser() {
        return Collections.unmodifiableList(adresser);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PersonInformasjonEntitet) o;
        return Objects.equals(personstatuser, that.personstatuser) &&
                Objects.equals(oppholdstillatelser, that.oppholdstillatelser) &&
                Objects.equals(statsborgerskap, that.statsborgerskap) &&
                Objects.equals(adresser, that.adresser) &&
                Objects.equals(personopplysninger, that.personopplysninger) &&
                Objects.equals(relasjoner, that.relasjoner);
    }


    @Override
    public int hashCode() {
        return Objects.hash(personstatuser, oppholdstillatelser, statsborgerskap, adresser, personopplysninger, relasjoner);
    }


    @Override
    public String toString() {
        return "PersonInformasjonEntitet{" +
            "id=" + id +
            ", personstatuser=" + personstatuser +
            ", oppholdstillatelser=" + oppholdstillatelser +
            ", statsborgerskap=" + statsborgerskap +
            ", adresser=" + adresser +
            ", personopplysninger=" + personopplysninger +
            ", relasjoner=" + relasjoner +
            '}';
    }

    PersonInformasjonBuilder.RelasjonBuilder getRelasjonBuilderForAktørId(AktørId fraAktør, AktørId tilAktør, RelasjonsRolleType rolle) {
        final var eksisterende = relasjoner.stream()
                .filter(it -> it.getAktørId().equals(fraAktør) && it.getTilAktørId().equals(tilAktør) && it.getRelasjonsrolle().equals(rolle))
                .findAny();
        return PersonInformasjonBuilder.RelasjonBuilder.oppdater(eksisterende).fraAktør(fraAktør).tilAktør(tilAktør).medRolle(rolle);
    }

    PersonInformasjonBuilder.AdresseBuilder getAdresseBuilderForAktørId(AktørId aktørId, AdresseType type, DatoIntervallEntitet periode) {
        final var eksisterende = adresser.stream()
                .filter(it -> it.getAktørId().equals(aktørId) && it.getAdresseType().equals(type) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
                .findAny();
        return PersonInformasjonBuilder.AdresseBuilder.oppdater(eksisterende).medAktørId(aktørId).medAdresseType(type).medPeriode(periode);
    }

    private boolean erSannsynligvisSammePeriode(DatoIntervallEntitet eksiterendePeriode, DatoIntervallEntitet nyPeriode) {
        return eksiterendePeriode.equals(nyPeriode) || eksiterendePeriode.getFomDato().equals(nyPeriode.getFomDato())
                && eksiterendePeriode.getTomDato().equals(Tid.TIDENES_ENDE) && !nyPeriode.getTomDato().equals(Tid.TIDENES_ENDE);
    }

    PersonInformasjonBuilder.StatsborgerskapBuilder getStatsborgerskapBuilderForAktørId(AktørId aktørId, Landkoder landkode, DatoIntervallEntitet periode) {
        final var eksisterende = statsborgerskap.stream()
                .filter(it -> it.getAktørId().equals(aktørId) && it.getStatsborgerskap().equals(landkode) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
                .findAny();
        return PersonInformasjonBuilder.StatsborgerskapBuilder.oppdater(eksisterende).medAktørId(aktørId).medStatsborgerskap(landkode).medPeriode(periode);
    }

    PersonInformasjonBuilder.PersonstatusBuilder getPersonstatusBuilderForAktørId(AktørId aktørId, DatoIntervallEntitet periode) {
        final var eksisterende = personstatuser.stream()
                .filter(it -> it.getAktørId().equals(aktørId) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
                .findAny();
        return PersonInformasjonBuilder.PersonstatusBuilder.oppdater(eksisterende).medAktørId(aktørId).medPeriode(periode);
    }

    PersonInformasjonBuilder.OppholdstillatelseBuilder getOppholdstillatelseBuilderForAktørId(AktørId aktørId, DatoIntervallEntitet periode) {
        final var eksisterende = oppholdstillatelser.stream()
            .filter(it -> it.getAktørId().equals(aktørId) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
            .findAny();
        return PersonInformasjonBuilder.OppholdstillatelseBuilder.oppdater(eksisterende).medAktørId(aktørId).medPeriode(periode);
    }
}
