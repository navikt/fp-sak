package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse.VurderOpphørDagensDato;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

class FastsettBehandlingsresultatVedEndring {
    private FastsettBehandlingsresultatVedEndring() {
    }

    public static Behandlingsresultat fastsett(Behandling revurdering,
                                               Betingelser er,
                                               UttakResultatHolder uttakresultatFraOriginalBehandling,
                                               UttakResultatHolder uttakresultatAnnenPart,
                                               HarEtablertYtelse harEtablertYtelse,
                                               boolean erSluttPåStønadsdager) {
        List<KonsekvensForYtelsen> konsekvenserForYtelsen = utledKonsekvensForYtelsen(er.endringIBeregning, er.endringIUttakFraEndringstidspunkt);

        if (!harEtablertYtelse.vurder(er.minstEnInnvilgetBehandlingUtenPåfølgendeOpphør, er.opphørFørEllerEtterDagensDato,
            uttakresultatFraOriginalBehandling, uttakresultatAnnenPart, erSluttPåStønadsdager)) {
            return harEtablertYtelse.fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        if (er.kunEndringIFordelingAvYtelsen) {
            return ErKunEndringIFordelingAvYtelsen.fastsett(revurdering, er.varselOmRevurderingSendt);
        }
        Vedtaksbrev vedtaksbrev = utledVedtaksbrev(konsekvenserForYtelsen, er.varselOmRevurderingSendt);
        BehandlingResultatType behandlingResultatType = utledBehandlingResultatType(konsekvenserForYtelsen);
        return buildBehandlingsresultat(revurdering, behandlingResultatType, konsekvenserForYtelsen, vedtaksbrev);
    }

    private static Vedtaksbrev utledVedtaksbrev(List<KonsekvensForYtelsen> konsekvenserForYtelsen, boolean erVarselOmRevurderingSendt) {
        if (!erVarselOmRevurderingSendt && konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return Vedtaksbrev.INGEN;
        }
        return Vedtaksbrev.AUTOMATISK;
    }

    private static BehandlingResultatType utledBehandlingResultatType(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        if (konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return BehandlingResultatType.INGEN_ENDRING;
        }
        return BehandlingResultatType.FORELDREPENGER_ENDRET;
    }

    private static List<KonsekvensForYtelsen> utledKonsekvensForYtelsen(boolean erEndringIBeregning, boolean erEndringIUttakFraEndringstidspunkt) {
        List<KonsekvensForYtelsen> konsekvensForYtelsen = new ArrayList<>();

        if (erEndringIBeregning) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_BEREGNING);
        }
        if (erEndringIUttakFraEndringstidspunkt) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_UTTAK);
        }
        if (konsekvensForYtelsen.isEmpty()) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.INGEN_ENDRING);
        }
        return konsekvensForYtelsen;
    }

    protected static Behandlingsresultat buildBehandlingsresultat(Behandling revurdering, BehandlingResultatType behandlingResultatType,
                                                                  List<KonsekvensForYtelsen> konsekvenserForYtelsen, Vedtaksbrev vedtaksbrev) {
        Behandlingsresultat behandlingsresultat = revurdering.getBehandlingsresultat();
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        behandlingsresultatBuilder.medBehandlingResultatType(behandlingResultatType);
        behandlingsresultatBuilder.medVedtaksbrev(vedtaksbrev);
        behandlingsresultatBuilder.medRettenTil(RettenTil.HAR_RETT_TIL_FP);
        konsekvenserForYtelsen.forEach(behandlingsresultatBuilder::leggTilKonsekvensForYtelsen);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }

    static class Betingelser {
        boolean endringIBeregning;
        boolean endringIUttakFraEndringstidspunkt;
        boolean varselOmRevurderingSendt;
        boolean kunEndringIFordelingAvYtelsen;
        boolean minstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
        VurderOpphørDagensDato opphørFørEllerEtterDagensDato;

        private Betingelser() {
        }

        public static Betingelser fastsett(boolean erEndringIBeregning, boolean erEndringIUttakFraEndringstidspunkt,
                                           boolean erVarselOmRevurderingSendt, boolean erKunEndringIFordelingAvYtelsen,
                                           boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør,
                                           VurderOpphørDagensDato gittOpphørFørEllerEtterDagensDato) {
            Betingelser b = new Betingelser();
            b.endringIBeregning = erEndringIBeregning;
            b.endringIUttakFraEndringstidspunkt = erEndringIUttakFraEndringstidspunkt;
            b.varselOmRevurderingSendt = erVarselOmRevurderingSendt;
            b.kunEndringIFordelingAvYtelsen = erKunEndringIFordelingAvYtelsen;
            b.minstEnInnvilgetBehandlingUtenPåfølgendeOpphør = erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
            b.opphørFørEllerEtterDagensDato = gittOpphørFørEllerEtterDagensDato;
            return b;
        }
    }
}
