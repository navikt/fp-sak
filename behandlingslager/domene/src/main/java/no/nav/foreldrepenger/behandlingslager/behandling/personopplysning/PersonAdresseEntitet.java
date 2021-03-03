package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

@Entity(name = "PersonopplysningAdresse")
@Table(name = "PO_ADRESSE")
public class PersonAdresseEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_ADRESSE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @Convert(converter = AdresseType.KodeverdiConverter.class)
    @Column(name = "adresse_type", nullable = false)
    private AdresseType adresseType;

    @ChangeTracked
    @Column(name = "matrikkelid")
    private String matrikkelId;

    @ChangeTracked
    @Column(name = "adresselinje1")
    private String adresselinje1;

    @ChangeTracked
    @Column(name = "adresselinje2")
    private String adresselinje2;

    @ChangeTracked
    @Column(name = "adresselinje3")
    private String adresselinje3;

    @ChangeTracked
    @Column(name = "adresselinje4")
    private String adresselinje4;

    @ChangeTracked
    @Column(name = "postnummer")
    private String postnummer;

    @ChangeTracked
    @Column(name = "poststed")
    private String poststed;

    @ChangeTracked
    @Column(name = "land")
    private String land;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonAdresseEntitet() {
    }

    PersonAdresseEntitet(PersonAdresseEntitet adresse) {
        this.matrikkelId = adresse.getMatrikkelId();
        this.adresselinje1 = adresse.getAdresselinje1();
        this.adresselinje2 = adresse.getAdresselinje2();
        this.adresselinje3 = adresse.getAdresselinje3();
        this.adresselinje4 = adresse.getAdresselinje4();
        this.adresseType = adresse.getAdresseType();
        this.postnummer = adresse.getPostnummer();
        this.poststed = adresse.getPoststed();
        this.land = adresse.getLand();

        this.aktørId = adresse.getAktørId();
        this.periode = adresse.getPeriode();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(aktørId, adresseType, land, periode);
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    public AdresseType getAdresseType() {
        return adresseType;
    }

    void setAdresseType(AdresseType adresseType) {
        this.adresseType = adresseType;
    }

    public String getMatrikkelId() {
        return matrikkelId;
    }

    public void setMatrikkelId(String matrikkelId) {
        this.matrikkelId = matrikkelId;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    void setAdresselinje1(String adresselinje1) {
        this.adresselinje1 = max40(adresselinje1);
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    void setAdresselinje2(String adresselinje2) {
        this.adresselinje2 = max40(adresselinje2);
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    void setAdresselinje3(String adresselinje3) {
        this.adresselinje3 = max40(adresselinje3);
    }

    public String getAdresselinje4() {
        return adresselinje4;
    }

    void setAdresselinje4(String adresselinje4) {
        this.adresselinje4 = max40(adresselinje4);
    }

    public String getPostnummer() {
        return postnummer;
    }

    void setPostnummer(String postnummer) {
        this.postnummer = postnummer;
    }

    public String getPoststed() {
        return poststed;
    }

    void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getLand() {
        return land;
    }

    void setLand(String land) {
        this.land = land;
    }

    private String max40(String adresselinje) {
        return adresselinje != null ? adresselinje.substring(0, Math.min(40, adresselinje.length())) : null;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public boolean erUtlandskAdresse() {
        return !Landkoder.erNorge(land) || adresseType.erUtlandsAdresseType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonAdresseEntitet entitet = (PersonAdresseEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(periode, entitet.periode) &&
            Objects.equals(adresseType, entitet.adresseType) &&
            Objects.equals(land, entitet.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, adresseType, land);
    }

    @Override
    public String toString() {
        return "PersonAdresseEntitet{" +
            "periode=" + periode +
            ", adresseType=" + adresseType +
            ", matrikkelId='" + matrikkelId + '\'' +
            ", adresselinje1='" + adresselinje1 + '\'' +
            ", adresselinje2='" + adresselinje2 + '\'' +
            ", postnummer='" + postnummer + '\'' +
            ", poststed='" + poststed + '\'' +
            ", land='" + land + '\'' +
            '}';
    }

}
