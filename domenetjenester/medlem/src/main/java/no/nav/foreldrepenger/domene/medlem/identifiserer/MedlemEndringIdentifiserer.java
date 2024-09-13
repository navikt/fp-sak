package no.nav.foreldrepenger.domene.medlem.identifiserer;

import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class MedlemEndringIdentifiserer {

    private MedlemEndringIdentifiserer() {
    }

    public static boolean erEndretForPeriode(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        var medlemPerioder1 = grunnlag1.getRegistrertMedlemskapPerioder().stream()
            .filter(p -> p.getPeriode().overlapper(periode))
            .collect(Collectors.toSet());
        var medlemPerioder2 = grunnlag2.getRegistrertMedlemskapPerioder().stream()
            .filter(p -> p.getPeriode().overlapper(periode))
            .collect(Collectors.toSet());
        return differ.erForskjellPå(medlemPerioder1, medlemPerioder2);
    }

    public static boolean harBeslutningsdatoInnenforPeriode(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        var medlemPerioder1 = grunnlag1.getRegistrertMedlemskapPerioder().stream()
            .filter(p -> p.getBeslutningsdato() == null || periode.inkluderer(p.getBeslutningsdato()))
            .collect(Collectors.toSet());
        var medlemPerioder2 = grunnlag2.getRegistrertMedlemskapPerioder().stream()
            .filter(p -> p.getBeslutningsdato() == null || periode.inkluderer(p.getBeslutningsdato()))
            .collect(Collectors.toSet());
        return differ.erForskjellPå(medlemPerioder1, medlemPerioder2);
    }
}
