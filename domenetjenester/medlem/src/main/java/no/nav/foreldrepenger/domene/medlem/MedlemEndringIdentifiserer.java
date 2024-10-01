package no.nav.foreldrepenger.domene.medlem;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class MedlemEndringIdentifiserer {

    private MedlemEndringIdentifiserer() {
    }

    public static boolean erEndretForPeriode(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        var medlemPerioder1 = grunnlag1.getRegistrertMedlemskapPerioderList().stream()
            .filter(p -> p.getPeriode().overlapper(periode))
            .toList();
        var medlemPerioder2 = grunnlag2.getRegistrertMedlemskapPerioderList().stream()
            .filter(p -> p.getPeriode().overlapper(periode))
            .toList();
        return differ.erForskjellPå(medlemPerioder1, medlemPerioder2);
    }

    public static boolean harBeslutningsdatoInnenforPeriode(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        var medlemPerioder1 = grunnlag1.getRegistrertMedlemskapPerioderList().stream()
            .filter(p -> p.getBeslutningsdato() == null || periode.inkluderer(p.getBeslutningsdato()))
            .toList();
        var medlemPerioder2 = grunnlag2.getRegistrertMedlemskapPerioderList().stream()
            .filter(p -> p.getBeslutningsdato() == null || periode.inkluderer(p.getBeslutningsdato()))
            .toList();
        return differ.erForskjellPå(medlemPerioder1, medlemPerioder2);
    }
}
