package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "PersonopplysningGrunnlagEntitet")
@Table(name = "GR_PERSONOPPLYSNING")
public class PersonopplysningGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_PERSONOPPLYSNING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne(cascade = { /* NONE - Aldri cascade til et selvstendig aggregat! */}, fetch = FetchType.EAGER)
    @JoinColumn(name = "so_annen_part_id", updatable = false)
    private OppgittAnnenPartEntitet søknadAnnenPart;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "registrert_informasjon_id", updatable = false)
    private PersonInformasjonEntitet registrertePersonopplysninger;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "overstyrt_informasjon_id", updatable = false)
    private PersonInformasjonEntitet overstyrtePersonopplysninger;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Transient
    private AktørId aktørId;

    PersonopplysningGrunnlagEntitet() {
    }

    PersonopplysningGrunnlagEntitet(PersonopplysningGrunnlagEntitet behandlingsgrunnlag) {
        behandlingsgrunnlag.getOppgittAnnenPart().ifPresent(oap -> this.søknadAnnenPart = oap);
        behandlingsgrunnlag.getOverstyrtVersjon().ifPresent(ov -> this.overstyrtePersonopplysninger = ov);
        behandlingsgrunnlag.getRegisterVersjon().ifPresent(rv -> this.registrertePersonopplysninger = rv);
    }

    /**
     * Kun synlig for abstract test scenario
     *
     * @return id
     */

    public Long getId() {
        return id;
    }

    Long getBehandlingId() {
        return behandlingId;
    }

    void setAktiv(final boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setOppgittAnnenPart(OppgittAnnenPartEntitet søknadAnnenPart) {
        this.søknadAnnenPart = søknadAnnenPart;
    }

    void setRegistrertePersonopplysninger(PersonInformasjonEntitet registrertePersonopplysninger) {
        this.registrertePersonopplysninger = registrertePersonopplysninger;
    }

    void setOverstyrtePersonopplysninger(PersonInformasjonEntitet overstyrtePersonopplysninger) {
        this.overstyrtePersonopplysninger = overstyrtePersonopplysninger;
    }


    public PersonInformasjonEntitet getGjeldendeVersjon() {
        return getOverstyrtVersjon().orElse(registrertePersonopplysninger);
    }


    public Optional<PersonInformasjonEntitet> getRegisterVersjon() {
        return Optional.ofNullable(registrertePersonopplysninger);
    }


    public Optional<PersonInformasjonEntitet> getOverstyrtVersjon() {
        return Optional.ofNullable(overstyrtePersonopplysninger);
    }


    public Optional<OppgittAnnenPartEntitet> getOppgittAnnenPart() {
        return Optional.ofNullable(søknadAnnenPart);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PersonopplysningGrunnlagEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) && Objects.equals(søknadAnnenPart, that.søknadAnnenPart) && Objects.equals(
            registrertePersonopplysninger, that.registrertePersonopplysninger) && Objects.equals(overstyrtePersonopplysninger,
            that.overstyrtePersonopplysninger);
    }


    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, søknadAnnenPart, registrertePersonopplysninger, overstyrtePersonopplysninger);
    }


    @Override
    public String toString() {
        var sb = new StringBuilder("PersonopplysningGrunnlagEntitet{");
        sb.append("id=").append(id);
        sb.append(", søknadAnnenPart=").append(søknadAnnenPart);
        sb.append(", aktiv=").append(aktiv);
        sb.append(", registrertePersonopplysninger=").append(registrertePersonopplysninger);
        sb.append(", overstyrtePersonopplysninger=").append(overstyrtePersonopplysninger);
        sb.append('}');
        return sb.toString();
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }
}
