package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.medlem.MedlemEndringIdentifiserer;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.MEDLEM_GRUNNLAG)
class StartpunktUtlederMedlemskap implements StartpunktUtleder {

    private final MedlemskapRepository medlemskapRepository;

    @Inject
    StartpunktUtlederMedlemskap(MedlemskapRepository medlemskapRepository) {
        this.medlemskapRepository = medlemskapRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId1);
        var grunnlag2 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId2);

        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var periode = stp.getUttaksintervall().map(i -> DatoIntervallEntitet.fraOgMedTilOgMed(i.getFomDato(), i.getTomDato()))
            .orElseGet(() -> DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt));
        if (MedlemEndringIdentifiserer.erEndretForPeriode(grunnlag1, grunnlag2, DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt))) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
                "medlemskap medlemskapsvilkår", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        } else if (MedlemEndringIdentifiserer.harBeslutningsdatoInnenforPeriode(grunnlag1, grunnlag2, periode)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
                "medlemskap medlemskapsvilkår (ny)", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        }
        return StartpunktType.UDEFINERT;
    }
}
