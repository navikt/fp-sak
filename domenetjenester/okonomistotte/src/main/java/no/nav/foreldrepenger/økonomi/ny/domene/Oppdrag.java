package no.nav.foreldrepenger.økonomi.ny.domene;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

public class Oppdrag {
    private ØkonomiKodeFagområde økonomiFagområde;
    private FagsystemId fagsystemId;
    private Betalingsmottaker betalingsmottaker;
    private Map<KjedeNøkkel, OppdragKjedeFortsettelse> kjeder;

    private Oppdrag(ØkonomiKodeFagområde økonomiFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker, Map<KjedeNøkkel, OppdragKjedeFortsettelse> kjeder) {
        this.økonomiFagområde = økonomiFagområde;
        this.fagsystemId = fagsystemId;
        this.betalingsmottaker = betalingsmottaker;
        this.kjeder = kjeder;
    }

    public ØkonomiKodeFagområde getØkonomiFagområde() {
        return økonomiFagområde;
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

    public static Builder builder(ØkonomiKodeFagområde økonomFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker) {
        return new Builder(økonomFagområde, fagsystemId, betalingsmottaker);
    }

    public boolean erTomt() {
        return kjeder.isEmpty();
    }

    public boolean harLinjer() {
        return !erTomt();
    }

    public boolean erNytt() {
        for (OppdragKjedeFortsettelse kjede : kjeder.values()) {
            if (!kjede.erNy()) {
                return false;
            }
        }
        return true;
    }

    public LocalDate getEndringsdato() {
        LocalDate endringsdato = null;
        for (OppdragKjedeFortsettelse kjede : kjeder.values()) {
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
        private ØkonomiKodeFagområde økonomiFagområde;

        private Builder(ØkonomiKodeFagområde økonomiFagområde, FagsystemId fagsystemId, Betalingsmottaker betalingsmottaker) {
            this.økonomiFagområde = økonomiFagområde;
            Objects.requireNonNull(økonomiFagområde);
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
            for (OppdragLinje oppdragslinje : kjede.getOppdragslinjer()) {
                if (!fagsystemId.equals(oppdragslinje.getDelytelseId().getFagsystemId())) {
                    throw new IllegalArgumentException("Oppdragslinje er for annen fagsystemId");
                }
            }
            kjeder.put(nøkkel, kjede);
            return this;
        }

        public Oppdrag build() {
            return new Oppdrag(økonomiFagområde, fagsystemId, betalingsmottaker, kjeder);
        }
    }
}
