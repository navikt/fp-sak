package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    @Override
    public boolean vurder(Behandling revurdering,
                          boolean finnesInnvilgetIkkeOpphørtVedtak,
                          VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                          UttakResultatHolder uttakResultatHolder) {
        return harEtablertYtelse(finnesInnvilgetIkkeOpphørtVedtak, opphørFørEllerEtterDagensDato, uttakResultatHolder);
    }

    private boolean harEtablertYtelse(boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør,
                                      VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                                      UttakResultatHolder uttakResultatHolder) {
        if (!uttakResultatHolder.eksistererUttakResultat()) {
            return false;
        }

        if (erSisteVedtakAvslagEllerOpphør(uttakResultatHolder, opphørFørEllerEtterDagensDato) ||
            erDagensDatoEtterSistePeriodeIUttak(uttakResultatHolder)) {
            return false;
        }
        return erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
    }

    @Override
    public Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering, List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        Behandlingsresultat behandlingsresultat = revurdering.getBehandlingsresultat();
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        konsekvenserForYtelsen.forEach(behandlingsresultatBuilder::leggTilKonsekvensForYtelsen);
        behandlingsresultatBuilder.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandlingsresultatBuilder.medRettenTil(RettenTil.HAR_RETT_TIL_FP);
        behandlingsresultatBuilder.medVedtaksbrev(Vedtaksbrev.AUTOMATISK);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }

    private boolean erSisteVedtakAvslagEllerOpphør(UttakResultatHolder fraOrginalBehandling,
                                                   VurderOpphørDagensDato opphørFørEllerEtterDagensDato) {
        var vedtak = fraOrginalBehandling.getBehandlingVedtak();
        if (vedtak.isEmpty()) {
            return false;
        }
        Behandlingsresultat behandlingsresultat = vedtak.get().getBehandlingsresultat();
        BehandlingResultatType resultatType = behandlingsresultat.getBehandlingResultatType();
        boolean erOpphørTilbakeITid = opphørFørEllerEtterDagensDato.test(behandlingsresultat);

        return BehandlingResultatType.AVSLÅTT.equals(resultatType) ||
            (BehandlingResultatType.OPPHØR.equals(resultatType) && erOpphørTilbakeITid);
    }

    private boolean erDagensDatoEtterSistePeriodeIUttak(UttakResultatHolder uttakResultat) {
        LocalDate dagensDato = FPDateUtil.iDag();
        LocalDate sisteDagISøkersUttak = uttakResultat.getSisteDagAvSistePeriode();

        return dagensDato.isAfter(sisteDagISøkersUttak);
    }

}
