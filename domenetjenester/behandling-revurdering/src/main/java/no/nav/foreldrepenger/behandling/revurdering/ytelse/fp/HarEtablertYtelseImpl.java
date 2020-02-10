package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    @Override
    public boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak,
                          VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                          UttakResultatHolder uttakResultatHolder,
                          UttakResultatHolder uttakresultatAnnenPart,
                          boolean erSluttPåStønadsdager) {
        return harEtablertYtelse(finnesInnvilgetIkkeOpphørtVedtak, opphørFørEllerEtterDagensDato, uttakResultatHolder, uttakresultatAnnenPart, erSluttPåStønadsdager);
    }

    private boolean harEtablertYtelse(boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør,
                                      VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                                      UttakResultatHolder uttakresultat,
                                      UttakResultatHolder uttakresultatAnnenPart,
                                      boolean erSluttPåStønadsdager) {
        if (!uttakresultat.eksistererUttakResultat()) {
            return false;
        }


        if (erSisteVedtakAvslagEllerOpphør(uttakresultat, opphørFørEllerEtterDagensDato) ||
            erDagensDatoEtterSistePeriodeIUttak(uttakresultat, uttakresultatAnnenPart) && erSluttPåStønadsdager) {
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
        BehandlingVedtak vedtak = fraOrginalBehandling.getBehandlingVedtak();
        if (vedtak == null) {
            return false;
        }
        Behandlingsresultat behandlingsresultat = vedtak.getBehandlingsresultat();
        BehandlingResultatType resultatType = behandlingsresultat.getBehandlingResultatType();
        boolean erOpphørTilbakeITid = opphørFørEllerEtterDagensDato.test(behandlingsresultat);

        return BehandlingResultatType.AVSLÅTT.equals(resultatType) ||
            BehandlingResultatType.OPPHØR.equals(resultatType) && erOpphørTilbakeITid;
    }

    private boolean erDagensDatoEtterSistePeriodeIUttak(UttakResultatHolder uttakResultatHolder,
                                                        UttakResultatHolder uttakResultatHolderAnnenPart) {
        LocalDate dagensDato = FPDateUtil.iDag();
        LocalDate sisteDagISøkersUttak = uttakResultatHolder.getSisteDagAvSistePeriode();
        LocalDate sisteDagIAnnenPartsUttak = uttakResultatHolderAnnenPart.getSisteDagAvSistePeriode();

        if (sisteDagIAnnenPartsUttak.isAfter(sisteDagISøkersUttak)) {
            return dagensDato.isAfter(sisteDagIAnnenPartsUttak);
        }
        return dagensDato.isAfter(sisteDagISøkersUttak);
    }
}
