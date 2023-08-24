package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Oppdrag {
    private final KodeFagområde kodeFagområde;
    private final FagsystemId fagsystemId;
    private final Betalingsmottaker betalingsmottaker;
    private final Map<KjedeNøkkel, OppdragKjedeFortsettelse> kjeder;

    private Oppdrag(KodeFagområde kodeFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker, Map<KjedeNøkkel, OppdragKjedeFortsettelse> kjeder) {
        this.kodeFagområde = kodeFagområde;
        this.fagsystemId = fagsystemId;
        this.betalingsmottaker = betalingsmottaker;
        this.kjeder = kjeder;
    }

    public KodeFagområde getKodeFagområde() {
        return kodeFagområde;
    }

    public FagsystemId getFagsystemId() {
        return fagsystemId;
    }

    public Betalingsmottaker getBetalingsmottaker() {
        return betalingsmottaker;
    }

    public Map<KjedeNøkkel, OppdragKjedeFortsettelse> getKjeder() {
        return kjeder;
    }

    public static Builder builder(KodeFagområde økonomFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker) {
        return new Builder(økonomFagområde, fagsystemId, betalingsmottaker);
    }

    public boolean erTomt() {
        return kjeder.isEmpty();
    }

    public boolean harLinjer() {
        return !erTomt();
    }

    public LocalDate getEndringsdato() {
        LocalDate endringsdato = null;
        for (var kjede : kjeder.values()) {
            if (endringsdato == null || kjede.getEndringsdato().isBefore(endringsdato)) {
                endringsdato = kjede.getEndringsdato();
            }
        }
        return endringsdato;
    }

    public static class Builder {

        private FagsystemId fagsystemId;
        private Betalingsmottaker betalingsmottaker;
        private Map<KjedeNøkkel, OppdragKjedeFortsettelse> kjeder = new HashMap<>();
        private KodeFagområde kodeFagområde;

        private Builder(KodeFagområde kodeFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker) {
            this.kodeFagområde = kodeFagområde;
            Objects.requireNonNull(kodeFagområde);
            Objects.requireNonNull(fagsystemId);
            Objects.requireNonNull(betalingsmottaker);
            this.fagsystemId = fagsystemId;
            this.betalingsmottaker = betalingsmottaker;
        }

        public Builder leggTil(KjedeNøkkel nøkkel, OppdragKjedeFortsettelse kjede) {
            Objects.requireNonNull(nøkkel);
            Objects.requireNonNull(kjede);
            if (kjeder.containsKey(nøkkel)) {
                throw new IllegalArgumentException("Inneholder allerede denne nøkkelen");
            }
            if (!betalingsmottaker.equals(nøkkel.getBetalingsmottaker())) {
                throw new IllegalArgumentException("Nøkkel er ikke for aktuell betalingsmottaker");
            }
            for (var oppdragslinje : kjede.getOppdragslinjer()) {
                if (!fagsystemId.equals(oppdragslinje.getDelytelseId().getFagsystemId())) {
                    throw new IllegalArgumentException("Oppdragslinje er for annen fagsystemId");
                }
            }
            kjeder.put(nøkkel, kjede);
            return this;
        }

        public Oppdrag build() {
            return new Oppdrag(kodeFagområde, fagsystemId, betalingsmottaker, kjeder);
        }
    }
}
