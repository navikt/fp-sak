package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity
@Table(name = "TILRETTELEGGING_FOM")
public class TilretteleggingFOM extends BaseEntitet implements IndexKey {

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

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(id);
    }

    public static class Builder {

        private TilretteleggingFOM mal;

        public Builder(TilretteleggingFOM tilretteleggingFOM) {
            mal = new TilretteleggingFOM();
            mal.type = tilretteleggingFOM.getType();
            mal.fomDato = tilretteleggingFOM.getFomDato();
            mal.stillingsprosent = tilretteleggingFOM.getStillingsprosent();
            mal.overstyrtUtbetalingsgrad = tilretteleggingFOM.getOverstyrtUtbetalingsgrad();
        }

        public Builder() {
            mal = new TilretteleggingFOM();
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

        public TilretteleggingFOM build() {
            Objects.requireNonNull(mal.fomDato, "fomDato er påkrevd");
            Objects.requireNonNull(mal.type, "type er påkrevd");
            return mal;
        }
    }
}
