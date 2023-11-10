package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OppdragKjede {

    private static final Logger LOG = LoggerFactory.getLogger(OppdragKjede.class);
    public static final OppdragKjede EMPTY = OppdragKjede.builder().build();

    private List<OppdragLinje> oppdragslinjer;

    private OppdragKjede(List<OppdragLinje> oppdragslinjer) {
        this.oppdragslinjer = oppdragslinjer;
    }

    public List<OppdragLinje> getOppdragslinjer() {
        return Collections.unmodifiableList(oppdragslinjer);
    }

    public Ytelse tilYtelse() {
        var builder = Ytelse.builder();
        for (var linje : oppdragslinjer) {
            leggTilOppdragLinje(builder, linje);
        }
        return builder.build();
    }

    static void leggTilOppdragLinje(Ytelse.Builder builder, OppdragLinje linje) {
        if (linje.getOpphørFomDato() == null) {
            builder.fjernAltEtter(linje.getPeriode().getFom());
            builder.leggTilPeriode(new YtelsePeriode(linje.getPeriode(), linje.getSats(), linje.getUtbetalingsgrad()));
        } else {
            builder.fjernAltEtter(linje.getOpphørFomDato());
        }
    }

    public FagsystemId getFagsystemId() {
        return oppdragslinjer.get(0).getDelytelseId().getFagsystemId();
    }

    public static Builder builder() {
        return new Builder();
    }


    public OppdragLinje getSisteLinje() {
        if (oppdragslinjer.isEmpty()) {
            throw new IllegalArgumentException("Har ingen linjer, kan ikke hente siste");
        }
        return oppdragslinjer.get(oppdragslinjer.size() - 1);
    }

    public boolean erTom() {
        return oppdragslinjer.isEmpty();
    }

    public OppdragKjede leggTil(OppdragKjedeFortsettelse fortsettelse) {
        var builder = builder();
        for (var oppdragLinje : oppdragslinjer) {
            builder.medOppdragslinje(oppdragLinje);
        }
        for (var oppdragLinje : fortsettelse.getOppdragslinjer()) {
            builder.medOppdragslinje(oppdragLinje);
        }
        return builder.build();
    }

    public LocalDate getFørsteDato() {
        return tilYtelse().getFørsteDato();
    }

    public static class Builder {

        private Ytelse.Builder ytelseBuilder = Ytelse.builder();
        private List<OppdragLinje> oppdragslinjer = new ArrayList<>();

        private Builder() {
        }

        public Builder medOppdragslinje(OppdragLinje linje) {
            if (oppdragslinjer.isEmpty()) {
                if (linje.getRefDelytelseId() != null) {
                    throw new IllegalArgumentException("Første oppdragslinje (delytelseId" + linje.getDelytelseId() + ") kan ikke referere til en annen");
                }
            } else {
                var siste = oppdragslinjer.get(oppdragslinjer.size() - 1);
                if (linje.getOpphørFomDato() == null) {
                    validerLinjeUtenOpphør(linje, siste);
                } else {
                    validerLinjeMedOpphør(linje, siste);
                }
            }
            oppdragslinjer.add(linje);
            leggTilOppdragLinje(ytelseBuilder, linje);
            return this;
        }

        private void validerLinjeUtenOpphør(OppdragLinje linje, OppdragLinje siste) {
            if (!ytelseBuilder.erTom() && !siste.getDelytelseId().equals(linje.getRefDelytelseId())) {
                throw new IllegalArgumentException("Oppdragslinje med delytelseId " + linje.getDelytelseId() + " er ikke først i kjeden, og må referere til forrige oppdragslinje (delytelseId " + siste.getDelytelseId() + ")");
            }

            var overskriverSiste = siste.getPeriode().equals(linje.getPeriode());
            if (!overskriverSiste && ytelseBuilder.sisteTidspunkt() != null && !ytelseBuilder.sisteTidspunkt().isBefore(linje.getPeriode().getFom())) {
                LOG.info("Oppdragslinje med delytelseid {} overlappet med det som er gjeldende så langt. Dette skal vanligvis ikke skje, men kan skje på gamle data", linje.getDelytelseId());
            }
        }

        private void validerLinjeMedOpphør(OppdragLinje linje, OppdragLinje siste) {
            if (!siste.getDelytelseId().equals(linje.getDelytelseId())) {
                throw new IllegalArgumentException("Ved opphør må samme delytelsesid som forrige oppdragslinje gjenbrukes");
            }
        }

        public OppdragKjede build() {
            return new OppdragKjede(oppdragslinjer);
        }

        public boolean erEffektivtTom() {
                return ytelseBuilder.erTom();
        }
    }
}
