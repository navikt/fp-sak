package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.ENTITY_NAME)
class BehandlingÅrsakUtlederPersonopplysning implements BehandlingÅrsakUtleder {
    private static final Logger LOG = LoggerFactory.getLogger(BehandlingÅrsakUtlederPersonopplysning.class);

    private PersonopplysningRepository personopplysningRepository;


    BehandlingÅrsakUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    BehandlingÅrsakUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = (long) grunnlagId1;
        var grunnlag2 = (long) grunnlagId2;

        var personopplysningGrunnlag1 = personopplysningRepository.hentGrunnlagPåId(grunnlag1);
        var personopplysningGrunnlag2 = personopplysningRepository.hentGrunnlagPåId(grunnlag2);

        var poDiff = new PersonopplysningGrunnlagDiff(ref.aktørId(), personopplysningGrunnlag1, personopplysningGrunnlag2);
        var forelderErDødEndret = poDiff.erForeldreDødsdatoEndret();
        var barnetsDødsdatoEndret = poDiff.erBarnDødsdatoEndret();

        if (forelderErDødEndret || barnetsDødsdatoEndret) {
            LOG.info("Setter endringsresultat til opplysning om død, har endring forelderErDødEndret {} barnetsDødsdatoEndret {}, grunnlagid1: {}, grunnlagid2: {}", forelderErDødEndret, barnetsDødsdatoEndret, grunnlag1, grunnlag2); //$NON-NLS-1
            return Set.of(EndringResultatType.OPPLYSNING_OM_DØD, EndringResultatType.REGISTEROPPLYSNING);
        }
        return Collections.singleton(EndringResultatType.REGISTEROPPLYSNING);
    }
}
