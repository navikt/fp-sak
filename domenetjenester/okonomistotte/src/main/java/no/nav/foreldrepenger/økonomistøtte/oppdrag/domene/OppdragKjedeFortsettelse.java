package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OppdragKjedeFortsettelse {

    private final LocalDate endringsdato;
    private final List<OppdragLinje> oppdragslinjer;

    private OppdragKjedeFortsettelse(LocalDate endringsdato, List<OppdragLinje> oppdragslinjer) {
        this.endringsdato = endringsdato;
        this.oppdragslinjer = oppdragslinjer;
    }

    public List<OppdragLinje> getOppdragslinjer() {
        return Collections.unmodifiableList(oppdragslinjer);
    }

    public FagsystemId getFagsystemId() {
        return oppdragslinjer.get(0).getDelytelseId().getFagsystemId();
    }

    public static Builder builder(LocalDate endringsdato) {
        return new Builder(endringsdato);
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

    public LocalDate getEndringsdato() {
        return endringsdato;
    }

    public static class Builder {

        private final LocalDate endringsdato;
        private final List<OppdragLinje> oppdragslinjer = new ArrayList<>();

        private Builder(LocalDate endringsdato) {
            this.endringsdato = endringsdato;
        }

        public Builder medOppdragslinje(OppdragLinje linje) {
            if (!oppdragslinjer.isEmpty()) {
                var siste = oppdragslinjer.get(oppdragslinjer.size() - 1);
                if (linje.getOpphørFomDato() == null) {
                    validerLinjeUtenOpphør(linje, siste);
                } else {
                    validerLinjeMedOpphør(linje, siste);
                }
            }
            //TODO valider at det ikke kan legges til en linje som er tidligere enn seneste ikke-opphørte tidspunkt
            oppdragslinjer.add(linje);
            return this;
        }

        private void validerLinjeUtenOpphør(OppdragLinje linje, OppdragLinje siste) {
            if (!siste.getDelytelseId().equals(linje.getRefDelytelseId())) {
                throw new IllegalArgumentException("Oppdragslinje må referere til forrige oppdragslinje");
            }
        }

        private void validerLinjeMedOpphør(OppdragLinje linje, OppdragLinje siste) {
            if (!siste.getDelytelseId().equals(linje.getDelytelseId())) {
                throw new IllegalArgumentException("Ved opphør må samme delytelsesid som forrige oppdragslinje gjenbrukes");
            }
        }

        public OppdragKjedeFortsettelse build() {
            return new OppdragKjedeFortsettelse(endringsdato, oppdragslinjer);
        }
    }
}
