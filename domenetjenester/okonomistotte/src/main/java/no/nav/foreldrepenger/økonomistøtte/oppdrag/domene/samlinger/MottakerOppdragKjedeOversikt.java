package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.*;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.SetUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class MottakerOppdragKjedeOversikt {

    private final Betalingsmottaker betalingsmottaker;
    protected final Map<KjedeNøkkel, OppdragKjede> oppdragskjeder;

    public MottakerOppdragKjedeOversikt(Betalingsmottaker betalingsmottaker, Map<KjedeNøkkel, OppdragKjede> oppdragskjeder) {
        this.betalingsmottaker = betalingsmottaker;
        this.oppdragskjeder = oppdragskjeder;
        if (oppdragskjeder.keySet().stream()
            .map(KjedeNøkkel::getBetalingsmottaker)
            .anyMatch(kjede -> !betalingsmottaker.equals(kjede))) {
            throw new IllegalArgumentException("Støtter kun oppdragskjeder for spesifisert mottaker");
        }
    }

    public Map<KjedeNøkkel, OppdragKjede> getKjeder() {
        return Collections.unmodifiableMap(oppdragskjeder);
    }

    public OppdragKjede getKjede(KjedeNøkkel nøkkel) {
        return oppdragskjeder.getOrDefault(nøkkel, OppdragKjede.EMPTY);
    }

    public DelytelseId finnHøyesteDelytelseId() {
        return oppdragskjeder.values().stream()
            .map(OppdragKjede::getSisteLinje)
            .map(OppdragLinje::getDelytelseId)
            .max(Comparator.naturalOrder())
            .orElseThrow();
    }

    public boolean isEmpty() {
        return oppdragskjeder.isEmpty();
    }

    public MottakerOppdragKjedeOversikt utvidMed(Oppdrag nyttOppdrag) {
        if (!betalingsmottaker.equals(nyttOppdrag.getBetalingsmottaker())) {
            throw new IllegalArgumentException("Kan ikke utvide med oppdrag for annen mottaker");
        }

        Map<KjedeNøkkel, OppdragKjede> kjeder = new TreeMap<>();
        for (var nøkkel : SetUtil.sortertUnionOfKeys(oppdragskjeder, nyttOppdrag.getKjeder())) {
            var eksisterende = getKjede(nøkkel);
            var fortsettelse = nyttOppdrag.getKjeder().get(nøkkel);
            if (fortsettelse == null) {
                kjeder.put(nøkkel, eksisterende);
            } else {
                kjeder.put(nøkkel, eksisterende.leggTil(fortsettelse));
            }
        }
        return new MottakerOppdragKjedeOversikt(betalingsmottaker, kjeder);
    }
}
