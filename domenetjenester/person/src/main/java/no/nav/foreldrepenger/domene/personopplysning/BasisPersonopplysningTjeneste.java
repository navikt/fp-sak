package no.nav.foreldrepenger.domene.personopplysning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;

@ApplicationScoped
public class BasisPersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    BasisPersonopplysningTjeneste() {
        super();
    }

    @Inject
    public BasisPersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

}
