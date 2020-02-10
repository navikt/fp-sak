package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.ForretningshendelseRegistrerer;

@ApplicationScoped
public class ForretningshendelseRegistrererProvider {

    private Instance<ForretningshendelseRegistrerer<? extends HendelseDto>> registrerere;

    ForretningshendelseRegistrererProvider() {
        // CDI
    }

    @Inject
    public ForretningshendelseRegistrererProvider(@Any Instance<ForretningshendelseRegistrerer<? extends HendelseDto>> registrerere) {
        this.registrerere = registrerere;
    }

    @SuppressWarnings("unchecked")
    public <T extends HendelseDto> ForretningshendelseRegistrerer<T> finnRegistrerer(HendelseDto hendelseDto) {
        Instance<ForretningshendelseRegistrerer<? extends HendelseDto>> selected = registrerere.select(new HendelseTypeRef.HendelseTypeRefLiteral(hendelseDto));
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for hendelsetype:" + hendelseDto.getHendelsetype());
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for hendelsetype:" + hendelseDto.getHendelsetype());
        }
        ForretningshendelseRegistrerer<? extends HendelseDto> minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (ForretningshendelseRegistrerer<T>) minInstans;
    }
}
