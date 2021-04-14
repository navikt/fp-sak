package no.nav.foreldrepenger.domene.medlem.identifiserer;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.Node;

@Dependent
public class MedlemEndringIdentifiserer {

    private static Period PERIODE_FØR_STP = Period.ofMonths(12);

    @Inject
    MedlemEndringIdentifiserer() {
    }

    public boolean erEndretFørSkjæringstidspunkt(MedlemskapAggregat grunnlag1, MedlemskapAggregat grunnlag2, LocalDate skjæringstidspunkt) {
        var differ = new RegisterdataDiffsjekker(true);
        final var nodeEndringer = differ.finnForskjellerPå(grunnlag1.getRegistrertMedlemskapPerioder(), grunnlag2.getRegistrertMedlemskapPerioder());

        return nodeEndringer.keySet().stream()
            .map(Node::getObject)
            .filter(it -> it instanceof MedlemskapPerioderEntitet)
            .anyMatch(adr -> {
                var medlPer = ((MedlemskapPerioderEntitet) adr).getPeriode();
                return medlPer != null && medlPer.getFomDato().isBefore(skjæringstidspunkt) && medlPer.getTomDato().isAfter(skjæringstidspunkt.minus(PERIODE_FØR_STP));
            });
    }
}
