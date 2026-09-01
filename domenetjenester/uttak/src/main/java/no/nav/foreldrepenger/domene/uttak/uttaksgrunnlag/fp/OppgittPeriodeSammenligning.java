package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public final class OppgittPeriodeSammenligning {

    private static final Set<UtsettelseÅrsak> UTSETTELSER_SOM_KREVER_SAKSBEHANDLING = Set.of(
        UtsettelseÅrsak.SYKDOM,
        UtsettelseÅrsak.INSTITUSJON_BARN,
        UtsettelseÅrsak.INSTITUSJON_SØKER);

    private OppgittPeriodeSammenligning() {
    }

    static SøknadMotInnvilgetUttak forSøknadMotInnvilgetUttak(OppgittPeriodeEntitet periode) {
        return new SøknadMotInnvilgetUttak(periode);
    }

    static Dokumentasjonsvurdering forDokumentasjonsvurdering(OppgittPeriodeEntitet periode) {
        return new Dokumentasjonsvurdering(periode);
    }

    public static boolean kreverSaksbehandling(OppgittPeriodeEntitet periode) {
        return kreverSaksbehandling(forSøknadMotInnvilgetUttak(periode));
    }

    static boolean kreverSaksbehandling(SøknadMotInnvilgetUttak periode) {
        if (periode == null || periode.årsak() instanceof OppholdÅrsak) {
            return false;
        }
        if (periode.årsak() instanceof UtsettelseÅrsak utsettelse) {
            return UTSETTELSER_SOM_KREVER_SAKSBEHANDLING.contains(utsettelse)
                || MorsAktivitet.forventerDokumentasjon(periode.morsAktivitet());
        }
        return true;
    }

    record SøknadMotInnvilgetUttak(Årsak årsak,
                                   UttakPeriodeType periodeType,
                                   SamtidigUttaksprosent samtidigUttaksprosent,
                                   Gradering gradering,
                                   boolean flerbarnsdager,
                                   MorsAktivitet morsAktivitet) {

        private SøknadMotInnvilgetUttak(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(),
                Optional.ofNullable(periode.getSamtidigUttaksprosent()).orElse(SamtidigUttaksprosent.HUNDRED),
                periode.isGradert() ? new Gradering(periode) : null,
                periode.isFlerbarnsdager(), periode.getMorsAktivitet());
        }
    }

    record Dokumentasjonsvurdering(Årsak årsak, UttakPeriodeType periodeType, MorsAktivitet morsAktivitet) {

        private Dokumentasjonsvurdering(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), normalisertPeriodeType(periode), periode.getMorsAktivitet());
        }
    }

    private record Gradering(GraderingAktivitetType aktivitet, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {

        private Gradering(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

    private static UttakPeriodeType normalisertPeriodeType(OppgittPeriodeEntitet periode) {
        return periode.isUtsettelse() ? UttakPeriodeType.UDEFINERT : periode.getPeriodeType();
    }
}
