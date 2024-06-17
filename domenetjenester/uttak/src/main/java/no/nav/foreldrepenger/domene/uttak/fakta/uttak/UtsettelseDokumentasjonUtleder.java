package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;

final class UtsettelseDokumentasjonUtleder {

    private UtsettelseDokumentasjonUtleder() {
    }

    static Optional<DokumentasjonVurderingBehov.Behov> utledBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                  LocalDate gjeldendeFamilieHendelse,
                                                                  boolean kreverSammenhengendeUttak,
                                                                  List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        if (!oppgittPeriode.isUtsettelse()) {
            return Optional.empty();
        }
        var årsak = utledBehovÅrsak(oppgittPeriode, gjeldendeFamilieHendelse, kreverSammenhengendeUttak, pleiepengerInnleggelser);
        if (årsak == null) {
            return Optional.empty();
        }
        return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE, årsak));
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsak(OppgittPeriodeEntitet oppgittPeriode,
                                                                           LocalDate gjeldendeFamilieHendelse,
                                                                           boolean kreverSammenhengendeUttak,
                                                                           List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        if (kreverSammenhengendeUttak) {
            return utledBehovÅrsakForSammenhengendeUttak(oppgittPeriode, pleiepengerInnleggelser);
        }
        return utledBehovÅrsakForFrittUttak(oppgittPeriode, gjeldendeFamilieHendelse, pleiepengerInnleggelser);
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsakForFrittUttak(OppgittPeriodeEntitet oppgittPeriode,
                                                                                        LocalDate gjeldendeFamilieHendelse,
                                                                                        List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        if (søktPeriodeInnenforTidsperiodeForbeholdtMor(oppgittPeriode, gjeldendeFamilieHendelse)) {
            var årsak = (UtsettelseÅrsak) oppgittPeriode.getÅrsak();
            return switch (årsak) {
                case SYKDOM -> DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER;
                case INSTITUSJON_SØKER -> DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_SØKER;
                case INSTITUSJON_BARN -> utledBehovÅrsakForInnlagtBarn(oppgittPeriode, pleiepengerInnleggelser);
                case FERIE, ARBEID, FRI, UDEFINERT, NAV_TILTAK, HV_OVELSE -> null;
            };
        }
        return null;
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsakForSammenhengendeUttak(OppgittPeriodeEntitet oppgittPeriode,
                                                                                                 List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        var årsak = (UtsettelseÅrsak) oppgittPeriode.getÅrsak();
        return switch (årsak) {
            case SYKDOM -> DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_SØKER;
            case INSTITUSJON_SØKER -> DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_SØKER;
            case INSTITUSJON_BARN -> utledBehovÅrsakForInnlagtBarn(oppgittPeriode, pleiepengerInnleggelser);
            case HV_OVELSE -> DokumentasjonVurderingBehov.Behov.Årsak.HV_ØVELSE;
            case NAV_TILTAK -> DokumentasjonVurderingBehov.Behov.Årsak.NAV_TILTAK;
            case FERIE, ARBEID, FRI, UDEFINERT -> null;
        };
    }

    private static DokumentasjonVurderingBehov.Behov.Årsak utledBehovÅrsakForInnlagtBarn(OppgittPeriodeEntitet utsettelseInnlagtBarn,
                                                                                         List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        return erAvklartAvVedtakOmPleiepenger(utsettelseInnlagtBarn,
            pleiepengerInnleggelser) ? null : DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_BARN;
    }

    static boolean søktPeriodeInnenforTidsperiodeForbeholdtMor(OppgittPeriodeEntitet søknadsperiode, LocalDate familiehendelse) {
        var tidsperiodeForbeholdtMor = TidsperiodeForbeholdtMor.tidsperiode(familiehendelse);
        var søktTidsperiode = new SimpleLocalDateInterval(søknadsperiode.getFom(), søknadsperiode.getTom());
        return søktTidsperiode.overlapper(tidsperiodeForbeholdtMor);
    }

    private static boolean erAvklartAvVedtakOmPleiepenger(OppgittPeriodeEntitet søknadsperiode,
                                                          List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser) {
        return pleiepengerInnleggelser.stream().anyMatch(i -> søknadsperiode.getTidsperiode().erOmsluttetAv(i.getPeriode()));
    }
}
