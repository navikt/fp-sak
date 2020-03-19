package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.adopsjonsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.foreldreansvarsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.fødselsvilkårOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.opptjeningsvilkåretOppfylt;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;

@ApplicationScoped
public class ForvaltningUttakTjeneste {

    private UttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public ForvaltningUttakTjeneste(UttakRepository uttakRepository,
                                    BehandlingRepository behandlingRepository,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                    VilkårResultatRepository vilkårResultatRepository) {
        this.uttakRepository = uttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    ForvaltningUttakTjeneste() {
        //CDI
    }

    boolean erFerdigForeldrepengerBehandlingSomHarFørtTilOpphør(long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getStatus().erFerdigbehandletStatus()
            && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())
            && behandlingsresultat.orElseThrow().isBehandlingsresultatOpphørt();
    }

    void lagOpphørtUttaksresultat(long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        kopierBeregningsgrunnlag(behandling);

        var forrigeUttaksresultat = hentForrigeUttaksresultat(behandling);
        var perioder = new UttakResultatPerioderEntitet();
        for (UttakResultatPeriodeEntitet uttaksperiode : forrigeUttaksresultat.getGjeldendePerioder().getPerioder()) {
            perioder.leggTilPeriode(opphørPeriode(behandling, uttaksperiode));
        }

        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, perioder);
    }

    private void kopierBeregningsgrunnlag(Behandling behandling) {
        behandling.getOriginalBehandling().map(Behandling::getId)
            .ifPresent(originalId -> beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(originalId, behandling.getId()));
    }

    private UttakResultatPeriodeEntitet opphørPeriode(Behandling behandling, UttakResultatPeriodeEntitet periode) {
        UttakResultatPeriodeEntitet.Builder builder = new UttakResultatPeriodeEntitet.Builder(periode.getFom(), periode.getTom())
            .medResultatType(PeriodeResultatType.AVSLÅTT, opphørAvslagsårsak(behandling))
            .medUtsettelseType(periode.getUtsettelseType())
            .medOppholdÅrsak(periode.getOppholdÅrsak())
            .medOverføringÅrsak(periode.getOverføringÅrsak())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medSamtidigUttaksprosent(periode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(periode.isFlerbarnsdager());
        if (periode.getPeriodeSøknad().isPresent()) {
            builder.medPeriodeSoknad(periode.getPeriodeSøknad().get());
        }

        var kopiertPeriode = builder.build();
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : periode.getAktiviteter()) {
            kopiertPeriode.leggTilAktivitet(opphørAktivitet(periode, aktivitet));
        }

        return kopiertPeriode;
    }

    private IkkeOppfyltÅrsak opphørAvslagsårsak(Behandling behandling) {
        VilkårResultat vilkårResultat = vilkårResultatRepository.hent(behandling.getId());
        if (!opptjeningsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.OPPTJENINGSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!fødselsvilkårOppfylt(vilkårResultat, BehandlingReferanse.fra(behandling))) {
            return IkkeOppfyltÅrsak.FØDSELSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!adopsjonsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.ADOPSJONSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!foreldreansvarsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT;
        }
        throw new IllegalStateException("Alle inngangsvilkår er oppfylt");
    }

    private UttakResultatPeriodeAktivitetEntitet opphørAktivitet(UttakResultatPeriodeEntitet periode,
                                                                 UttakResultatPeriodeAktivitetEntitet aktivitet) {
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitet.getUttakAktivitet())
            .medTrekkonto(aktivitet.getTrekkonto())
            .medTrekkdager(Trekkdager.ZERO)
            .medArbeidsprosent(aktivitet.getArbeidsprosent())
            .medUtbetalingsprosent(BigDecimal.ZERO)
            .medErSøktGradering(aktivitet.isSøktGradering())
            .build();
    }

    private UttakResultatEntitet hentForrigeUttaksresultat(Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandling().orElseThrow();
        return uttakRepository.hentUttakResultat(originalBehandling.getId());
    }
}
