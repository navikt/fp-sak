package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@GrunnlagRef(NesteSakGrunnlagEntitet.GRUNNLAG_NAME)
class StartpunktUtlederNesteSak implements StartpunktUtleder {

    private NesteSakRepository nesteSakRepository;

    StartpunktUtlederNesteSak() {
        // For CDI
    }

    @Inject
    StartpunktUtlederNesteSak(NesteSakRepository nesteSakRepository) {
        this.nesteSakRepository = nesteSakRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var startdato1 = Optional.ofNullable(nesteSakRepository.hentGrunnlagPåId((Long)grunnlagId1))
            .map(NesteSakGrunnlagEntitet::getStartdato).orElse(null);
        var startdato2 =Optional.ofNullable(nesteSakRepository.hentGrunnlagPåId((Long)grunnlagId2))
            .map(NesteSakGrunnlagEntitet::getStartdato).orElse(null);

        // Ser kun på endring av startdato for neste stønadsperiode
        if (Objects.equals(startdato1, startdato2)) {
            return StartpunktType.UDEFINERT;
        } else {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "nestesak",
                Optional.ofNullable(grunnlagId1).orElse(""), Optional.ofNullable(grunnlagId2).orElse(""));
            return StartpunktType.UTTAKSVILKÅR;
        }
    }

}
