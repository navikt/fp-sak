package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusIntervall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record MedlemskapDto(ManuellBehandlingResultat manuellBehandlingResultat,
                            LegacyManuellBehandling legacyManuellBehandling,
                            Set<Region> regioner,
                            Set<Personstatus> personstatuser,
                            Set<Utenlandsopphold> utenlandsopphold,
                            Set<PersonadresseDto> adresser,
                            Set<Oppholdstillatelse> oppholdstillatelser,
                            Set<MedlemskapPeriode> medlemskapsperioder,
                            Set<MedlemskapAvvik> avvik,
                            Annenpart annenpart) {

    private static final LocalDate OPPHOLD_CUTOFF = LocalDate.of(2018, 7, 1);


    /**
     * Settes hvis det krever manuell behandling og gammel vurdering ikke finnes.
     */
    record ManuellBehandlingResultat(Avslagsårsak avslagskode, LocalDate medlemFom, LocalDate opphørFom) {
    }

    /**
     * Settes når gammel vurdering finnes, og ikke ny?
     */
    record LegacyManuellBehandling(Set<MedlemPeriode> perioder) {

        @JsonInclude(Include.NON_NULL)
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
        public static Personstatus map(PersonstatusIntervall pse) {
            return new Personstatus(pse.intervall().getFomDato(), pse.intervall().getTomDato(), pse.personstatus());
        }
    }

    record Utenlandsopphold(LocalDate fom, LocalDate tom, Landkoder landkode) {
        public static Utenlandsopphold map(MedlemskapOppgittLandOppholdEntitet moloe) {
            return new Utenlandsopphold(moloe.getPeriodeFom(), moloe.getPeriodeTom(), moloe.getLand());
        }
    }

    record Oppholdstillatelse(LocalDate fom, LocalDate tom, OppholdstillatelseType type) {
        public static Oppholdstillatelse map(OppholdstillatelseEntitet oe) {
            var fom = oe.getPeriode().getFomDato().isBefore(OPPHOLD_CUTOFF) ? null : oe.getPeriode().getFomDato();
            return new Oppholdstillatelse(fom, oe.getPeriode().getTomDato(), oe.getTillatelse());
        }
    }

    record MedlemskapPeriode(LocalDate fom, LocalDate tom, boolean erMedlem, String lovvalgsland, String studieland,
                             MedlemskapType medlemskapType, MedlemskapDekningType dekningType, LocalDate beslutningsdato) {
        public static MedlemskapPeriode map(MedlemskapPerioderEntitet mpe) {
            return new MedlemskapPeriode(mpe.getFom(), mpe.getTom(), mpe.getErMedlem(), Landkoder.navnLesbart(mpe.getLovvalgLand()),
                Landkoder.navnLesbart(mpe.getStudieland()), mpe.getMedlemskapType(), mpe.getDekningType(), mpe.getBeslutningsdato());
        }
    }

    record Annenpart(Set<PersonadresseDto> adresser, Set<Region> regioner, Set<Personstatus> personstatuser) {
    }
}

