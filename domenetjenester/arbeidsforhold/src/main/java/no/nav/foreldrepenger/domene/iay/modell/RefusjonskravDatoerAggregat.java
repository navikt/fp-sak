package no.nav.foreldrepenger.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RefusjonskravDatoerAggregat {

    private List<RefusjonskravDato> refusjonskravDatoer = new ArrayList<>();


    public RefusjonskravDatoerAggregat() {
    }

    RefusjonskravDatoerAggregat(RefusjonskravDatoerAggregat refusjonskravDatoerAggregat) {
        this(refusjonskravDatoerAggregat.getRefusjonskravDatoer());
    }

    public RefusjonskravDatoerAggregat(Collection<RefusjonskravDato> refusjonskravDatoer) {
        this.refusjonskravDatoer.addAll(refusjonskravDatoer.stream().map(rd -> {
            final RefusjonskravDato refusjonskravDato = new RefusjonskravDato(rd);
            return refusjonskravDato;
        }).collect(Collectors.toList()));
    }

    public List<RefusjonskravDato> getRefusjonskravDatoer() {
        return refusjonskravDatoer;
    }
}
