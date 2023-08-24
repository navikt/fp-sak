package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.FagsystemId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class OverordnetOppdragKjedeOversikt {

    protected final Map<KjedeNøkkel, OppdragKjede> oppdragskjeder;
    private final Map<Betalingsmottaker, FagsystemId> fagsystemIdPrMottaker;

    public static OverordnetOppdragKjedeOversikt TOM = new OverordnetOppdragKjedeOversikt(Collections.emptyMap());

    public OverordnetOppdragKjedeOversikt(Map<KjedeNøkkel, OppdragKjede> oppdragskjeder) {
        this.oppdragskjeder = oppdragskjeder;
        this.fagsystemIdPrMottaker = oppdragskjeder.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getBetalingsmottaker(), entry -> entry.getValue().getFagsystemId(), (a, b) -> a));
    }

    public Set<Betalingsmottaker> getBetalingsmottakere() {
        return getFagsystemIdPrMottaker().keySet();
    }

    public Map<Betalingsmottaker, FagsystemId> getFagsystemIdPrMottaker() {
        return fagsystemIdPrMottaker;
    }

    public FagsystemId høyesteFagsystemId() {
        return getFagsystemIdPrMottaker().values().stream()
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    public MottakerOppdragKjedeOversikt filter(Betalingsmottaker betalingsmottaker) {
        return new MottakerOppdragKjedeOversikt(betalingsmottaker, oppdragskjeder.entrySet().stream()
            .filter(e -> e.getKey().getBetalingsmottaker().equals(betalingsmottaker))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public Map<KjedeNøkkel, OppdragKjede> getKjeder() {
        return Collections.unmodifiableMap(oppdragskjeder);
    }

    public boolean isEmpty() {
        return oppdragskjeder.isEmpty();
    }
}
