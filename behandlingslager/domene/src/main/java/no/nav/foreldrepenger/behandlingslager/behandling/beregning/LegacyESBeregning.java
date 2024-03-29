package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "LegacyESBeregning")
@Table(name = "BR_LEGACY_ES_BEREGNING")
public class LegacyESBeregning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_LEGACY_ES_BEREGNING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beregning_resultat_id", nullable = false, updatable = false)
    private LegacyESBeregningsresultat beregningResultat;

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
    LegacyESBeregning() {
        // for hibernate
    }

    public LegacyESBeregning(long satsVerdi, long antallBarn, long beregnetTilkjentYtelse, LocalDateTime beregnetTidspunkt, boolean overstyrt, Long opprinneligBeregnetTilkjentYtelse) {
        this(null, satsVerdi, antallBarn, beregnetTilkjentYtelse, beregnetTidspunkt, overstyrt, opprinneligBeregnetTilkjentYtelse);
    }

    public LegacyESBeregning(long satsVerdi, long antallBarn, long beregnetTilkjentYtelse, LocalDateTime beregnetTidspunkt) {
        this(null, satsVerdi, antallBarn, beregnetTilkjentYtelse, beregnetTidspunkt, false, null);
    }

    LegacyESBeregning(LegacyESBeregningsresultat beregningResultat, long satsVerdi, long antallBarn, long beregnetTilkjentYtelse, LocalDateTime beregnetTidspunkt, boolean overstyrt, Long opprinneligBeregnetTilkjentYtelse) {
        Objects.requireNonNull(beregnetTidspunkt, "beregnetTidspunkt må være satt");
        this.beregningResultat = beregningResultat;
        this.satsVerdi = satsVerdi;
        this.antallBarn = antallBarn;
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
        this.beregnetTidspunkt = beregnetTidspunkt;
        this.overstyrt = overstyrt;
        this.opprinneligBeregnetTilkjentYtelse = opprinneligBeregnetTilkjentYtelse;
    }

    public boolean isOverstyrt() {
        return overstyrt;
    }

    public Long getId() {
        return id;
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

    public LegacyESBeregningsresultat getBeregningResultat() {
        return beregningResultat;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var beregning = (LegacyESBeregning) o;
        return Objects.equals(this.overstyrt, beregning.overstyrt)
            && Objects.equals(this.satsVerdi, beregning.satsVerdi)
            && Objects.equals(this.beregnetTilkjentYtelse, beregning.beregnetTilkjentYtelse)
            && Objects.equals(this.beregnetTidspunkt, beregning.beregnetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overstyrt, satsVerdi, beregnetTilkjentYtelse, beregnetTidspunkt);
    }

}
