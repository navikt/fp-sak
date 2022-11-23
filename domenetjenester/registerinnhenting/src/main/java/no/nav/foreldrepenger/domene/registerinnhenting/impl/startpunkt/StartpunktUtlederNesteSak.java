package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var startdato1 = Optional.ofNullable(nesteSakRepository.hentGrunnlagPåId((Long)grunnlagId1))
            .map(NesteSakGrunnlagEntitet::getStartdato).orElse(null);
        var startdato2 =Optional.ofNullable(nesteSakRepository.hentGrunnlagPåId((Long)grunnlagId2))
            .map(NesteSakGrunnlagEntitet::getStartdato).orElse(null);

        // Ser kun på endring av startdato for neste stønadsperiode
        return Objects.equals(startdato1, startdato2) ? StartpunktType.UDEFINERT : StartpunktType.UTTAKSVILKÅR;
    }

}
