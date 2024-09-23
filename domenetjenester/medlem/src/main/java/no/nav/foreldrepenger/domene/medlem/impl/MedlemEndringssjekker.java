package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;

public abstract class MedlemEndringssjekker {

    abstract RegisterdataDiffsjekker opprettNyDiffer();

    public boolean erEndret(List<MedlemskapPerioderEntitet> list1, List<MedlemskapPerioderEntitet> list2) {
        var differ = opprettNyDiffer();
        return list1.size() != list2.size() || differ.erForskjellPå(list1, list2);
    }

    public boolean erEndring(MedlemskapPerioderEntitet perioder1, MedlemskapPerioderEntitet perioder2) {
        var differ = opprettNyDiffer();
        return differ.erForskjellPå(perioder1, perioder2);
    }
}
