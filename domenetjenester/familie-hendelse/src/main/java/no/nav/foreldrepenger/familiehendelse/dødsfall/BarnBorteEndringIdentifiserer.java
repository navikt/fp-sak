package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.typer.AktørId;

@Dependent
public class BarnBorteEndringIdentifiserer {

    private PersonopplysningRepository personopplysningRepository;

    BarnBorteEndringIdentifiserer() {
        // For CDI
    }

    @Inject
    BarnBorteEndringIdentifiserer(BehandlingRepositoryProvider provider) {
        this.personopplysningRepository = provider.getPersonopplysningRepository();
    }

    public boolean erEndret(BehandlingReferanse nyBehandling) {
        var origBehandling = nyBehandling.getOriginalBehandlingId().orElse(null);
        if (origBehandling == null) {
            // Støtter bare deteksjon av endring i antall barn dersom det skjer mellom to behandlinger
            // (krevende å sjekke hvilket PO-grunnlag som er nyest basert kun på grunnlag-id)
            return false;
        }

        var søker = nyBehandling.aktørId();

        var origBarna = personopplysningRepository.hentPersonopplysningerHvisEksisterer(origBehandling)
            .map(origGrunnlag ->  getBarn(søker, origGrunnlag))
            .orElse(emptyList());
        var nyeBarna = personopplysningRepository.hentPersonopplysningerHvisEksisterer(nyBehandling.behandlingId())
            .map(origGrunnlag ->  getBarn(søker, origGrunnlag))
            .orElse(emptyList());

        // Sjekk om noen av de registrerte barna på orig grunnlag har forsvunnet på nytt grunnlag
        return origBarna.stream()
            .anyMatch(origBarn -> nyeBarna.stream()
                .noneMatch(nyttBarn -> Objects.equals(nyttBarn.getAktørId(), origBarn.getAktørId())));
    }

    private List<PersonopplysningEntitet> getBarn(AktørId søker, PersonopplysningGrunnlagEntitet grunnlag) {
        if (grunnlag.getRegisterVersjon().isEmpty()) {
            return emptyList();
        }

        return grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getRelasjoner).orElse(emptyList()).stream()
            .filter(rel -> rel.getAktørId().equals(søker) && rel.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(relSøkerBarn -> grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonopplysninger).orElse(emptyList()).stream()
                .filter(person -> person.getAktørId().equals(relSøkerBarn.getTilAktørId()))
                .findAny()
                .orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }
}
