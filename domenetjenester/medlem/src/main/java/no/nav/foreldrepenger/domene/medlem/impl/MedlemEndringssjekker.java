package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;

public abstract class MedlemEndringssjekker {

    abstract RegisterdataDiffsjekker opprettNyDiffer();

    public boolean erEndret(Optional<MedlemskapAggregat> medlemskap, List<MedlemskapPerioderEntitet> list1, List<MedlemskapPerioderEntitet> list2) {
        var differ = opprettNyDiffer();
        return medlemskap.isEmpty() || differ.erForskjellPå(list1, list2);
    }

    public boolean erEndring(Optional<MedlemskapAggregat> nyttMedlemskap, Optional<MedlemskapAggregat> eksisterendeMedlemskap) {

        if (eksisterendeMedlemskap.isEmpty() && nyttMedlemskap.isEmpty()) {
            return false;
        }
        if (eksisterendeMedlemskap.isPresent() && nyttMedlemskap.isEmpty()) {
            return true;
        }
        if (eksisterendeMedlemskap.isEmpty() && nyttMedlemskap.isPresent()) {  // NOSONAR - "redundant" her er false pos.
            return true;
        }

        var differ = opprettNyDiffer();
        return !differ.erForskjellPå(nyttMedlemskap.get().getRegistrertMedlemskapPerioderList(), eksisterendeMedlemskap.get().getRegistrertMedlemskapPerioderList());
    }

    public boolean erEndring(MedlemskapPerioderEntitet perioder1, MedlemskapPerioderEntitet perioder2) {
        var differ = opprettNyDiffer();
        return differ.erForskjellPå(perioder1, perioder2);
    }
}
