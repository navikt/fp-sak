package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    @Inject
    public HarEtablertYtelseImpl(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                 UttakInputTjeneste uttakInputTjeneste,
                                 RelatertBehandlingTjeneste relatertBehandlingTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    HarEtablertYtelseImpl() {
        //CDI
    }

    @Override
    public boolean vurder(Behandling revurdering,
                          boolean finnesInnvilgetIkkeOpphørtVedtak,
                          VurderOpphørDagensDato opphørFørEllerEtterDagensDato,
                          UttakResultatHolder uttakResultatHolder) {
        if (!uttakResultatHolder.eksistererUttakResultat()) {
            return false;
        }

        if (erSisteVedtakAvslagEllerOpphør(uttakResultatHolder, opphørFørEllerEtterDagensDato)) {
            return false;
        }

        var annenpartUttak = getAnnenPartUttak(revurdering.getFagsak().getSaksnummer());
        if (erDagensDatoEtterSistePeriodeIUttak(uttakResultatHolder, annenpartUttak)) {
            var uttakInputOriginalBehandling = uttakInputTjeneste.lagInput(revurdering.getOriginalBehandling().orElseThrow());
            if (stønadskontoSaldoTjeneste.erSluttPåStønadsdager(uttakInputOriginalBehandling)) {
                return false;
            }
        }
        return finnesInnvilgetIkkeOpphørtVedtak;
    }

    private UttakResultatHolder getAnnenPartUttak(Saksnummer saksnummer) {
        return new UttakResultatHolderImpl(relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattUttaksplan(saksnummer)
            .filter(this::erTilknyttetLøpendeFagsak));
    }

    private boolean erTilknyttetLøpendeFagsak(UttakResultatEntitet uttakResultatEntitet) {
        return uttakResultatEntitet.getBehandlingsresultat().getBehandling().getFagsak().getStatus().equals(FagsakStatus.LØPENDE);
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
