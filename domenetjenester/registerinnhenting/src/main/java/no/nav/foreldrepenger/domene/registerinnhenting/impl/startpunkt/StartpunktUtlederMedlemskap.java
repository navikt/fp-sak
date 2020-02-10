package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.medlem.identifiserer.MedlemEndringIdentifiserer;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@GrunnlagRef("MedlemskapAggregat")
class StartpunktUtlederMedlemskap implements StartpunktUtleder {

    private MedlemskapRepository medlemskapRepository;
    private MedlemEndringIdentifiserer endringIdentifiserer;

    @Inject
    StartpunktUtlederMedlemskap(MedlemskapRepository medlemskapRepository, MedlemEndringIdentifiserer endringIdentifiserer) {
        this.medlemskapRepository = medlemskapRepository;
        this.endringIdentifiserer = endringIdentifiserer;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        final MedlemskapAggregat grunnlag1 = medlemskapRepository.hentMedlemskapPåId((Long)grunnlagId1);
        final MedlemskapAggregat grunnlag2 = medlemskapRepository.hentMedlemskapPåId((Long)grunnlagId2);

        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        final boolean erEndretFørSkjæringstidspunkt = endringIdentifiserer.erEndretFørSkjæringstidspunkt(grunnlag1, grunnlag2, skjæringstidspunkt);
        if (erEndretFørSkjæringstidspunkt) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "medlemskap medlemskapsvilkår", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        }
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "medlemskap uttak", grunnlagId1, grunnlagId2);
        return StartpunktType.UTTAKSVILKÅR;
    }
}
