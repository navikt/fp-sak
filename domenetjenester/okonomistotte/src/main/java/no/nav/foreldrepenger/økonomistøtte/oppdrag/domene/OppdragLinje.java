package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.util.Objects;

public class OppdragLinje {
    private final Periode periode;

    private final Satsen sats;
    private final Utbetalingsgrad utbetalingsgrad;
    private final DelytelseId delytelseId;
    private final DelytelseId refDelytelseId;
    private final LocalDate opphørFomDato;

    public static Builder builder() {
        return new Builder();
    }

    public static OppdragLinje lagOpphørslinje(OppdragLinje opphøres, LocalDate opphørFomDato) {
        return OppdragLinje.builder()
            .medDelytelseId(opphøres.getDelytelseId())
            .medPeriode(opphøres.getPeriode())
            .medSats(opphøres.getSats())
            .medUtbetalingsgrad(opphøres.getUtbetalingsgrad())
            .medOpphørFomDato(opphørFomDato)
            .build();
    }

    private OppdragLinje(Periode periode, Satsen sats, Utbetalingsgrad utbetalingsgrad, DelytelseId delytelseId, DelytelseId refDelytelseId, LocalDate opphørFomDato) {
        this.periode = periode;
        this.sats = sats;
        this.utbetalingsgrad = utbetalingsgrad;
        this.delytelseId = delytelseId;
        this.refDelytelseId = refDelytelseId;
        this.opphørFomDato = opphørFomDato;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Satsen getSats() {
        return sats;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public DelytelseId getDelytelseId() {
        return delytelseId;
    }

    public DelytelseId getRefDelytelseId() {
        return refDelytelseId;
    }

    public LocalDate getOpphørFomDato() {
        return opphørFomDato;
    }

    public boolean erOpphørslinje() {
        return opphørFomDato != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OppdragLinje) o;
        return periode.equals(that.periode) &&
            sats.equals(that.sats) &&
            Objects.equals(utbetalingsgrad, that.utbetalingsgrad) &&
            delytelseId.equals(that.delytelseId) &&
            Objects.equals(refDelytelseId, that.refDelytelseId) &&
            Objects.equals(opphørFomDato, that.opphørFomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delytelseId);
    }

    @Override
    public String toString() {
        return "OppdragLinje{" +
            "delytelseId=" + delytelseId +
            ", periode=" + periode +
            ", sats=" + sats +
            (utbetalingsgrad != null ? ", utbetalingsgrad=" + utbetalingsgrad : "") +
            (refDelytelseId != null ? ", refDelytelseId=" + refDelytelseId : "") +
            (opphørFomDato != null ? ", opphørFomDato=" + opphørFomDato : "") +
            '}';
    }
    public static class Builder {

        private Periode periode;
        private Satsen sats;
        private Utbetalingsgrad utbetalingsgrad;
        private DelytelseId delytelseId;
        private DelytelseId refDelytelseId;
        private LocalDate opphørFomDato;

        private Builder() {
        }

        public Builder medYtelsePeriode(YtelsePeriode ytelsePeriode) {
            this.periode = ytelsePeriode.getPeriode();
            this.sats = ytelsePeriode.getSats();
            this.utbetalingsgrad = ytelsePeriode.getUtbetalingsgrad();
            return this;
        }

        public Builder medPeriode(Periode periode) {
            this.periode = periode;
            return this;
        }

        public Builder medSats(Satsen sats) {
            this.sats = sats;
            return this;
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medDelytelseId(DelytelseId delytelseId) {
            this.delytelseId = delytelseId;
            return this;
        }

        //TODO bruk heller metode som tar inn DelytelseId-objektet
        public Builder medDelytelseId(Long delytelseId) {
            this.delytelseId = DelytelseId.parse(Long.toString(delytelseId));
            return this;
        }

        public Builder medRefDelytelseId(DelytelseId refDelytelseId) {
            this.refDelytelseId = refDelytelseId;
            return this;
        }

        //TODO bruk heller metode som tar inn DelytelseId-objektet
        public Builder medRefDelytelseId(Long refDelytelseId) {
            this.refDelytelseId = refDelytelseId != null ? DelytelseId.parse(Long.toString(refDelytelseId)) : null;
            return this;
        }

        public Builder medOpphørFomDato(LocalDate opphørFomDato) {
            this.opphørFomDato = opphørFomDato;
            return this;
        }

        public OppdragLinje build() {
            Objects.requireNonNull(periode);
            Objects.requireNonNull(sats);
            Objects.requireNonNull(delytelseId);
            return new OppdragLinje(periode, sats, utbetalingsgrad, delytelseId, refDelytelseId, opphørFomDato);
        }
    }

}
