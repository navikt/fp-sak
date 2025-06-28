package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "LegacyESBeregning")
@Table(name = "BR_LEGACY_ES_BEREGNING")
public class EngangsstønadBeregning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_LEGACY_ES_BEREGNING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "sats_verdi", nullable = false)
    private long satsVerdi;

    @Column(name = "antall_barn", nullable = false)
    private long antallBarn;

    @Column(name = "beregnet_tilkjent_ytelse", nullable = false)
    private long beregnetTilkjentYtelse;

    @Column(name = "oppr_beregnet_tilkjent_ytelse")
    private Long opprinneligBeregnetTilkjentYtelse;

    @Column(name = "beregnet_tidspunkt", nullable = false)
    private LocalDateTime beregnetTidspunkt;

    /**
     * Hvorvidt hele beregning er overstyrt av Saksbehandler. (fra SF3).
     */
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt", nullable = false)
    private boolean overstyrt = false;

    @SuppressWarnings("unused")
    EngangsstønadBeregning() {
        // for hibernate
    }


    public EngangsstønadBeregning(Long behandlingId, long satsVerdi, long antallBarn, long beregnetTilkjentYtelse, LocalDateTime beregnetTidspunkt) {
        Objects.requireNonNull(behandlingId, "behandlingId må være satt");
        Objects.requireNonNull(beregnetTidspunkt, "beregnetTidspunkt må være satt");
        this.behandlingId = behandlingId;
        this.satsVerdi = satsVerdi;
        this.antallBarn = antallBarn;
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
        this.beregnetTidspunkt = beregnetTidspunkt;
        this.overstyrt = false;
        this.opprinneligBeregnetTilkjentYtelse = null;
    }

    public boolean isOverstyrt() {
        return overstyrt;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean erAktivt() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public long getSatsVerdi() {
        return satsVerdi;
    }

    public long getAntallBarn() {
        return antallBarn;
    }

    public long getBeregnetTilkjentYtelse() {
        return beregnetTilkjentYtelse;
    }

    public LocalDateTime getBeregnetTidspunkt() {
        return beregnetTidspunkt;
    }

    public Long getOpprinneligBeregnetTilkjentYtelse() {
        return opprinneligBeregnetTilkjentYtelse;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var beregning = (EngangsstønadBeregning) o;
        return Objects.equals(this.overstyrt, beregning.overstyrt)
            && Objects.equals(this.satsVerdi, beregning.satsVerdi)
            && Objects.equals(this.beregnetTilkjentYtelse, beregning.beregnetTilkjentYtelse)
            && Objects.equals(this.beregnetTidspunkt, beregning.beregnetTidspunkt)
            && Objects.equals(this.behandlingId, beregning.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overstyrt, satsVerdi, beregnetTilkjentYtelse, beregnetTidspunkt, behandlingId);
    }

    public boolean likBeregning(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var beregning = (EngangsstønadBeregning) o;
        return Objects.equals(this.satsVerdi, beregning.satsVerdi)
            && Objects.equals(this.beregnetTilkjentYtelse, beregning.beregnetTilkjentYtelse)
            && Objects.equals(this.behandlingId, beregning.behandlingId);
    }

}
