package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.regler.uttak.konfig.Konfigurasjon;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;

final class UtsettelseDokumentasjonUtleder {

    private UtsettelseDokumentasjonUtleder() {
    }

    static Optional<DokumentasjonVurderingBehov.Behov> utledBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                  LocalDate gjeldendeFamilieHendelse,
                                                                  boolean kreverSammenhengendeUttak) {
        //TODO TFP-4873 pleiepenger
        if (!oppgittPeriode.isUtsettelse()) {
            return Optional.empty();
        }
        var årsak = utledBehovÅrsak(oppgittPeriode, gjeldendeFamilieHendelse, kreverSammenhengendeUttak);
        if (årsak == null) {
            return Optional.empty();
        }
        return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE, årsak));
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsak(OppgittPeriodeEntitet oppgittPeriode,
                                                                           LocalDate gjeldendeFamilieHendelse,
                                                                           boolean kreverSammenhengendeUttak) {
        if (kreverSammenhengendeUttak) {
            return utledBehovÅrsakForSammenhengendeUttak(oppgittPeriode);
        }
        return utledBehovÅrsakForFrittUttak(oppgittPeriode, gjeldendeFamilieHendelse);
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsakForFrittUttak(OppgittPeriodeEntitet oppgittPeriode,
                                                                                        LocalDate gjeldendeFamilieHendelse) {
        if (søktPeriodeInnenforTidsperiodeForbeholdtMor(oppgittPeriode, gjeldendeFamilieHendelse)) {
            var årsak = (UtsettelseÅrsak) oppgittPeriode.getÅrsak();
            return switch (årsak) {
                case SYKDOM -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER;
                case INSTITUSJON_SØKER -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_SØKER;
                case INSTITUSJON_BARN -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_BARN;
                case FERIE, ARBEID, FRI, UDEFINERT, NAV_TILTAK, HV_OVELSE -> null;
            };
        }
        return null;
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsakForSammenhengendeUttak(OppgittPeriodeEntitet oppgittPeriode) {
        var årsak = (UtsettelseÅrsak) oppgittPeriode.getÅrsak();
        return switch (årsak) {
            case SYKDOM -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.SYKDOM_SØKER;
            case INSTITUSJON_SØKER -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_SØKER;
            case INSTITUSJON_BARN -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.INNLEGGELSE_BARN;
            case HV_OVELSE -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> DokumentasjonVurderingBehov.Behov.UtsettelseÅrsak.NAV_TILTAK;
            case FERIE, ARBEID, FRI, UDEFINERT -> null;
        };
    }

    static boolean søktPeriodeInnenforTidsperiodeForbeholdtMor(OppgittPeriodeEntitet søknadsperiode, LocalDate familiehendelse) {
        var tidsperiodeForbeholdtMor = new SimpleLocalDateInterval(fomTidsperiodeForbeholdtMor(familiehendelse),
            TidsperiodeForbeholdtMor.tilOgMed(familiehendelse));
        var søktTidsperiode = new SimpleLocalDateInterval(søknadsperiode.getFom(), søknadsperiode.getTom());
        return søktTidsperiode.overlapper(tidsperiodeForbeholdtMor);
    }

    private static LocalDate fomTidsperiodeForbeholdtMor(LocalDate familiehendelse) {
        return familiehendelse.minusWeeks(Konfigurasjon.STANDARD.getParameter(Parametertype.UTTAK_FELLESPERIODE_FØR_FØDSEL_UKER, familiehendelse));
    }
}
