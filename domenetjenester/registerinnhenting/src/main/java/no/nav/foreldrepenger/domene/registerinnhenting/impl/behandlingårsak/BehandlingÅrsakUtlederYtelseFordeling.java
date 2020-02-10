package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef("YtelseFordelingAggregat")
class BehandlingÅrsakUtlederYtelseFordeling implements BehandlingÅrsakUtleder {

    private static final Set<BehandlingÅrsakType> UDEFINERT = Set.of(BehandlingÅrsakType.UDEFINERT);
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederYtelseFordeling.class);
    @Inject
    public BehandlingÅrsakUtlederYtelseFordeling() {
        //For CDI
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        log.info("Setter behandlingårsak til udefinert, har endring i YtelseFordeling, grunnlagid1: {}, grunnlagid2: {}", grunnlagId1, grunnlagId2); //$NON-NLS-1
        return UDEFINERT;
    }
}
