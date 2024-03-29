package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "TILRETTELEGGING_FOM")
public class TilretteleggingFOM extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILRETTELEGGING_FOM")
    private Long id;

    @Convert(converter = TilretteleggingType.KodeverdiConverter.class)
    @Column(name = "type", nullable = false)
    private TilretteleggingType type;

    @Column(name = "FOM_DATO", nullable = false)
    private LocalDate fomDato;

    @Column(name = "STILLINGSPROSENT")
    private BigDecimal stillingsprosent;

    @Column(name = "OVERSTYRT_UTBETALINGSGRAD")
    private BigDecimal overstyrtUtbetalingsgrad;

    @Column(name = "TIDLIGST_MOTATT_DATO")
    private LocalDate tidligstMotattDato;

    @Column(name = "KILDE")
    @Enumerated(EnumType.STRING)
    private SvpTilretteleggingFomKilde kilde;


    public Long getId() {
        return id;
    }

    public TilretteleggingType getType() {
        return type;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public BigDecimal getOverstyrtUtbetalingsgrad() {
        return overstyrtUtbetalingsgrad;
    }

    public LocalDate getTidligstMotattDato() {
        return tidligstMotattDato;
    }

    public SvpTilretteleggingFomKilde getKilde() {
        return kilde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (TilretteleggingFOM) o;
        return type == that.type
            && Objects.equals(fomDato, that.fomDato)
            && Objects.equals(stillingsprosent, that.stillingsprosent)
            && Objects.equals(overstyrtUtbetalingsgrad, that.overstyrtUtbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, fomDato, stillingsprosent, overstyrtUtbetalingsgrad);
    }

    public static class Builder {

        private TilretteleggingFOM mal;

        public Builder(TilretteleggingFOM tilretteleggingFOM) {
            mal = new TilretteleggingFOM();
            mal.type = tilretteleggingFOM.getType();
            mal.fomDato = tilretteleggingFOM.getFomDato();
            mal.stillingsprosent = tilretteleggingFOM.getStillingsprosent();
            mal.overstyrtUtbetalingsgrad = tilretteleggingFOM.getOverstyrtUtbetalingsgrad();
            mal.tidligstMotattDato = tilretteleggingFOM.getTidligstMotattDato();
            mal.kilde = tilretteleggingFOM.kilde != null ? tilretteleggingFOM.kilde : null;
        }

        public Builder() {
            mal = new TilretteleggingFOM();
        }

        public Builder fraEksisterende(TilretteleggingFOM tilretteleggingFOM) {
            var builder =  new Builder().medFomDato(tilretteleggingFOM.getFomDato())
                .medTilretteleggingType(tilretteleggingFOM.getType())
                .medStillingsprosent(tilretteleggingFOM.getStillingsprosent())
                .medOverstyrtUtbetalingsgrad(tilretteleggingFOM.getOverstyrtUtbetalingsgrad())
                .medTidligstMottattDato(tilretteleggingFOM.getTidligstMotattDato());
            if (tilretteleggingFOM.kilde != null) {
                builder.medKilde(tilretteleggingFOM.kilde);
            }
            return builder;
        }

        public Builder medTilretteleggingType(TilretteleggingType type) {
            mal.type = type;
            return this;
        }

        public Builder medFomDato(LocalDate fomDato) {
            mal.fomDato = fomDato;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            mal.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medOverstyrtUtbetalingsgrad(BigDecimal overstyrtUtbetalingsgrad) {
            mal.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
            return this;
        }

        public Builder medTidligstMottattDato(LocalDate tidligstMotattDato) {
            mal.tidligstMotattDato = tidligstMotattDato;
            return this;
        }
        public Builder medKilde(SvpTilretteleggingFomKilde kilde) {
            mal.kilde = kilde;
            return this;
        }

        public TilretteleggingFOM build() {
            Objects.requireNonNull(mal.fomDato, "fomDato er påkrevd");
            Objects.requireNonNull(mal.type, "type er påkrevd");
            return mal;
        }
    }
}
