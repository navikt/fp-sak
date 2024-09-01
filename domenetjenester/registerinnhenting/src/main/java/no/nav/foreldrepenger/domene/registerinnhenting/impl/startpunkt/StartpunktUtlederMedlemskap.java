package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.medlem.identifiserer.MedlemEndringIdentifiserer;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.MEDLEM_GRUNNLAG)
class StartpunktUtlederMedlemskap implements StartpunktUtleder {

    private MedlemskapRepository medlemskapRepository;

    @Inject
    StartpunktUtlederMedlemskap(MedlemskapRepository medlemskapRepository) {
        this.medlemskapRepository = medlemskapRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId1);
        var grunnlag2 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId2);

        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt);
        if (MedlemEndringIdentifiserer.erEndretForPeriode(grunnlag1, grunnlag2, periode)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
                "medlemskap medlemskapsvilkår", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        } else if (MedlemEndringIdentifiserer.erEndretForPeriode(grunnlag1, grunnlag2, DatoIntervallEntitet.fraOgMed(skjæringstidspunkt))) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR,
                "medlemskap uttak", grunnlagId1, grunnlagId2);
            return StartpunktType.UTTAKSVILKÅR;
        }
        return StartpunktType.UDEFINERT;
    }
}
