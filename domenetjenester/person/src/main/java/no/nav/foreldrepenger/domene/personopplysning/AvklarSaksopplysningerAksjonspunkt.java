package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

class AvklarSaksopplysningerAksjonspunkt {
    private PersonopplysningRepository personopplysningRepository;

    AvklarSaksopplysningerAksjonspunkt(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    void oppdater(Long behandlingId, AktørId aktørId, PersonopplysningAksjonspunktDto adapter) {
        PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForOverstyring(behandlingId);

        LocalDate fom = adapter.getPersonstatusTypeKode().get().getGyldigFom();
        LocalDate tom = adapter.getPersonstatusTypeKode().get().getGyldigTom();
        DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        final PersonstatusType status = settPersonstatusType(adapter.getPersonstatusTypeKode().map(PersonopplysningAksjonspunktDto.PersonstatusPeriode::getPersonstatus));
        if (status != null) {
            PersonInformasjonBuilder.PersonstatusBuilder medPersonstatus = builder.getPersonstatusBuilder(aktørId, intervall)
                .medAktørId(aktørId)
                .medPeriode(intervall)
                .medPersonstatus(status);
            builder.leggTil(medPersonstatus);

            personopplysningRepository.lagre(behandlingId, builder);
        }
    }

    private PersonstatusType settPersonstatusType(Optional<String> personstatus) {
        if (personstatus.isPresent()) {
            Set<PersonstatusType> personstatusType = PersonstatusType.personstatusTyperFortsattBehandling();
            final String personstatusen = personstatus.get();
            for (PersonstatusType type : personstatusType) {
                if (type.getKode().equals(personstatusen)) {
                    return type;
                }
            }
        }
        return null;
    }

}
