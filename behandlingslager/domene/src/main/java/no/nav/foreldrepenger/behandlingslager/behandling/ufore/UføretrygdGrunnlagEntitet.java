package no.nav.foreldrepenger.behandlingslager.behandling.ufore;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/*
 * Grunnlag med livssyklus gitt av søknad - vil bare bli innhentet dersom søknad tilsier at annenpart (mor) er uføretrygdet
 * Dette er litt spesielt sammensatt ettersom eldre tilfelle er gitt av oppgitt søknadsperiode, ikke oppgitt rettighet
 * Dersom register (Pesys) ikke har informasjon om Uføretrygd, legges det opp til manuell avklaring
 */
@Entity(name = "UforetrygdGrunnlag")
@Table(name = "GR_UFORETRYGD")
public class UføretrygdGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UFORETRYGD")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "register_ufore")
    private Boolean uføretrygdRegister;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt_ufore")
    private Boolean uføretrygdOverstyrt;

    @Column(name = "uforedato")
    @ChangeTracked
    private LocalDate uføredato;

    @Column(name = "virkningsdato")
    @ChangeTracked
    private LocalDate virkningsdato;



    UføretrygdGrunnlagEntitet() {
    }

    UføretrygdGrunnlagEntitet(UføretrygdGrunnlagEntitet grunnlag) {
        this.aktørId = grunnlag.aktørId;
        this.uføretrygdRegister = grunnlag.uføretrygdRegister;
        this.uføretrygdOverstyrt = grunnlag.uføretrygdOverstyrt;
        this.uføredato = grunnlag.uføredato;
        this.virkningsdato = grunnlag.virkningsdato;
    }

    public boolean annenForelderMottarUføretrygd() {
        return Objects.equals(Boolean.TRUE, uføretrygdRegister) || Objects.equals(Boolean.TRUE, uføretrygdOverstyrt);
    }

    public boolean uavklartAnnenForelderMottarUføretrygd() {
        return !Objects.equals(Boolean.TRUE, uføretrygdRegister) && uføretrygdOverstyrt == null;
    }

    Long getId() {
        return id;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    public AktørId getAktørIdAnnenPart() {
        return aktørId;
    }

    public Boolean getUføretrygdRegister() {
        return uføretrygdRegister;
    }

    public Boolean getUføretrygdOverstyrt() {
        return uføretrygdOverstyrt;
    }

    public LocalDate getUføredato() {
        return uføredato;
    }

    public LocalDate getVirkningsdato() {
        return virkningsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UføretrygdGrunnlagEntitet)) return false;
        UføretrygdGrunnlagEntitet that = (UføretrygdGrunnlagEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(aktørId, that.aktørId) &&
            Objects.equals(uføretrygdRegister, that.uføretrygdRegister) &&
            Objects.equals(uføretrygdOverstyrt, that.uføretrygdOverstyrt) &&
            Objects.equals(uføredato, that.uføredato) &&
            Objects.equals(virkningsdato, that.virkningsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, aktørId, uføretrygdRegister, uføretrygdOverstyrt, uføredato, virkningsdato);
    }

    public static class Builder {

        private UføretrygdGrunnlagEntitet kladd;

        private Builder(UføretrygdGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static Builder nytt() {
            return new Builder(new UføretrygdGrunnlagEntitet());
        }

        private static Builder oppdatere(UføretrygdGrunnlagEntitet kladd) {
            return new Builder(new UføretrygdGrunnlagEntitet(kladd));
        }

        public static Builder oppdatere(Optional<UføretrygdGrunnlagEntitet> kladd) {
            return kladd.map(Builder::oppdatere).orElseGet(Builder::nytt);
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medAktørIdUføretrygdet(AktørId aktørId) {
            this.kladd.aktørId = aktørId;
            return this;
        }

        public Builder medRegisterUføretrygd(boolean erUføretrygdet, LocalDate uføredato, LocalDate virkningsdato) {
            if (erUføretrygdet && uføredato == null) {
                throw new IllegalArgumentException("Utviklerfeil: Skal ikke kalles uten uføredato");
            }
            this.kladd.uføretrygdRegister = erUføretrygdet;
            this.kladd.uføredato = erUføretrygdet ? uføredato : null;
            this.kladd.virkningsdato = erUføretrygdet ? virkningsdato : null;
            return this;
        }

        public Builder medOverstyrtUføretrygd(boolean erUføretrygdet) {
            Objects.requireNonNull(this.kladd.uføretrygdRegister, "Utviklerfeil: register skal være satt");
            this.kladd.uføretrygdOverstyrt = erUføretrygdet;
            return this;
        }

        public Builder medManueltAvklartUføretrygd(boolean erUføretrygdet) {
            this.kladd.uføretrygdOverstyrt = erUføretrygdet;
            return this;
        }

        public UføretrygdGrunnlagEntitet build() {
            return this.kladd;
        }
    }
}
