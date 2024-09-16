package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAvvik;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record MedlemskapV3Dto(ManuellBehandling manuellBehandling,
                              LegacyManuellBehandling legacyManuellBehandling,
                              Set<Region> regioner,
                              Set<Personstatus> personstatuser,
                              Set<Utenlandsopphold> utenlandsopphold,
                              Set<Adresse> adresser,
                              Set<Oppholdstillatelse> oppholdstillatelser,
                              Set<MedlemskapPeriode> medlemskapsperioder,
                              Annenpart annenpart) {

    private static final LocalDate OPPHOLD_CUTOFF = LocalDate.of(2018, 7, 1);


    /**
     * Settes hvis det krever manuell behandling og gammel vurdering ikke finnes.
     */
    record ManuellBehandling(Set<MedlemskapAvvik> avvik, Resultat resultat) {


        record Resultat(Avslagsårsak avslagskode, LocalDate medlemFom, LocalDate opphørFom) {
        }
    }

    /**
     * Settes når gammel vurdering finnes, og ikke ny?
     */
    record LegacyManuellBehandling(Set<MedlemPeriode> perioder) {
        record MedlemPeriode(LocalDate vurderingsdato,
                             Boolean oppholdsrettVurdering,
                             Boolean erEosBorger,
                             Boolean lovligOppholdVurdering,
                             Boolean bosattVurdering,
                             MedlemskapManuellVurderingType medlemskapManuellVurderingType,
                             String begrunnelse) {
        }
    }

    record Region(LocalDate fom, LocalDate tom, no.nav.foreldrepenger.behandlingslager.geografisk.Region type) {
    }

    record Personstatus(LocalDate fom, LocalDate tom, PersonstatusType type) {
        public static Personstatus map(PersonstatusEntitet pse) {
            return new Personstatus(pse.getPeriode().getFomDato(), pse.getPeriode().getTomDato(), pse.getPersonstatus());
        }
    }

    record Utenlandsopphold(LocalDate fom, LocalDate tom, Landkoder landkode) {
        public static Utenlandsopphold map(MedlemskapOppgittLandOppholdEntitet moloe) {
            return new Utenlandsopphold(moloe.getPeriodeFom(), moloe.getPeriodeTom(), moloe.getLand());
        }
    }

    record Adresse(LocalDate fom, LocalDate tom, PersonadresseDto adresse) {
        public static Adresse map(PersonAdresseEntitet personAdresseEntitet) {
            return new Adresse(personAdresseEntitet.getPeriode().getFomDato(), personAdresseEntitet.getPeriode().getTomDato(),
                PersonadresseDto.tilDto(personAdresseEntitet));
        }
    }

    record Oppholdstillatelse(LocalDate fom, LocalDate tom, OppholdstillatelseType type) {
        public static Oppholdstillatelse map(OppholdstillatelseEntitet oe) {
            var fom = oe.getPeriode().getFomDato().isBefore(OPPHOLD_CUTOFF) ? null : oe.getPeriode().getFomDato();
            return new Oppholdstillatelse(fom, oe.getPeriode().getTomDato(), oe.getTillatelse());
        }
    }

    record MedlemskapPeriode(LocalDate fom, LocalDate tom, boolean erMedlem, Landkoder lovvalgsland, Landkoder studieland,
                             MedlemskapType medlemskapType, MedlemskapDekningType dekningType, LocalDate beslutningsdato) {
        public static MedlemskapPeriode map(MedlemskapPerioderEntitet mpe) {
            return new MedlemskapPeriode(mpe.getFom(), mpe.getTom(), mpe.getErMedlem(), mpe.getLovvalgLand(), mpe.getStudieland(),
                mpe.getMedlemskapType(), mpe.getDekningType(), mpe.getBeslutningsdato());
        }
    }

    record Annenpart(Set<Adresse> adresser, Set<Region> regioner, Set<Personstatus> personstatuser) {
    }
}

