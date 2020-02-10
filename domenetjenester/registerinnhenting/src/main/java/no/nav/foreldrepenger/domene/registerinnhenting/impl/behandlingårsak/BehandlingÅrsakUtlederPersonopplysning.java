package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;

@ApplicationScoped
@GrunnlagRef("PersonInformasjon")
class BehandlingÅrsakUtlederPersonopplysning implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederPersonopplysning.class);

    private PersonopplysningRepository personopplysningRepository;


    BehandlingÅrsakUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    BehandlingÅrsakUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        long grunnlag1 = (long) grunnlagId1;
        long grunnlag2 = (long) grunnlagId2;

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = personopplysningRepository.hentPersonopplysningerPåId(grunnlag1);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = personopplysningRepository.hentPersonopplysningerPåId(grunnlag2);

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(ref.getAktørId(), personopplysningGrunnlag1, personopplysningGrunnlag2);
        boolean forelderErDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean barnetsDødsdatoEndret = poDiff.erBarnDødsdatoEndret();

        if (forelderErDødEndret || barnetsDødsdatoEndret) {
            log.info("Setter behandlingårsak til opplysning om død, har endring forelderErDødEndret {} barnetsDødsdatoEndret {}, grunnlagid1: {}, grunnlagid2: {}", forelderErDødEndret, barnetsDødsdatoEndret, grunnlag1, grunnlag2); //$NON-NLS-1
            return java.util.Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD)));
        }
        log.info("Setter behandlingårsak til registeropplysning, grunnlagid1: {}, grunnlagid2: {}", grunnlag1, grunnlag2); //$NON-NLS-1
        return java.util.Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(BehandlingÅrsakType.RE_REGISTEROPPLYSNING)));
    }
}
